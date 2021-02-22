/*
 * Copyright (c) 2020 D4L data4life gGmbH / All rights reserved.
 *
 * D4L owns all legal rights, title and interest in and to the Software Development Kit ("SDK"),
 * including any intellectual property rights that subsist in the SDK.
 *
 * The SDK and its documentation may be accessed and used for viewing/review purposes only.
 * Any usage of the SDK for other purposes, including usage for the development of
 * applications/third-party applications shall require the conclusion of a license agreement
 * between you and D4L.
 *
 * If you are interested in licensing the SDK for your own applications/third-party
 * applications and/or if you’d like to contribute to the development of the SDK, please
 * contact D4L by email to help@data4life.care.
 */

package care.data4life.sample;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.chrisbanes.photoview.PhotoView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import care.data4life.fhir.stu3.model.Attachment;
import care.data4life.sdk.Data4LifeClient;
import care.data4life.sdk.call.Task;
import care.data4life.sdk.helpers.stu3.AttachmentExtension;
import care.data4life.sdk.lang.D4LException;
import care.data4life.sdk.listener.ResultListener;
import care.data4life.sdk.model.DownloadType;
import care.data4life.sdk.util.MimeType;

public class AttachmentPreviewActivity extends AppCompatActivity {
    public static final String TAG = AttachmentPreviewActivity.class.getSimpleName();
    private static final String RECORD_ID_KEY = "RECORD_ID_KEY";
    private static final String ATTACHMENT_ID_KEY = "ATTACHMENT_ID_KEY";
    private static final String ATTACHMENT_DOWLOAD_FAILED = "Attachment download failed!";

    private Toolbar toolbar;
    private ProgressBar progressBar;
    private PhotoView photoView;
    private PDFView pdfView;
    private String recordId;
    private String attachmentId;
    private Task downloadTask;

    private Data4LifeClient client;

    public static Intent getCallingIntent(Context ctx, String recordId, String attachmentId) {
        Intent i = new Intent(ctx, AttachmentPreviewActivity.class);
        i.putExtra(RECORD_ID_KEY, recordId);
        i.putExtra(ATTACHMENT_ID_KEY, attachmentId);
        return i;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attachment_preview);
        toolbar = findViewById(R.id.toolbarT);
        progressBar = findViewById(R.id.progressBar);

        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        recordId = getIntent().getStringExtra(RECORD_ID_KEY);
        attachmentId = getIntent().getStringExtra(ATTACHMENT_ID_KEY);
        photoView = findViewById(R.id.photoView);
        pdfView = findViewById(R.id.pdfView);

        client = Data4LifeClient.getInstance();
        downloadAttachment();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (downloadTask.isActive()) downloadTask.cancel();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) finish();
        return super.onOptionsItemSelected(item);
    }

    private void downloadAttachment() {
        setProgressBarVisibility(View.VISIBLE);

        downloadTask = client.downloadAttachment(recordId, attachmentId, DownloadType.Medium, new ResultListener<Attachment>() {
            @Override
            public void onSuccess(Attachment attachment) {
                runOnUiThread(() -> {
                    renderAttachment(attachment);
                    setProgressBarVisibility(View.GONE);
                });
            }

            @Override
            public void onError(D4LException exception) {
                Log.e(TAG, exception.getMessage());
                runOnUiThread(() -> {
                    Utils.showToastMessage(AttachmentPreviewActivity.this, ATTACHMENT_DOWLOAD_FAILED);
                    setProgressBarVisibility(View.GONE);
                });
            }
        });
    }

    private void setProgressBarVisibility(int visibility) {
        progressBar.setVisibility(visibility);
    }

    private void renderAttachment(Attachment attachment) {
        toolbar.setTitle(attachment.title);
        byte[] data = AttachmentExtension.getData(attachment);

        MimeType dataMimeType = MimeType.Companion.recognizeMimeType(data);
        switch (dataMimeType) {
            case JPEG:
            case PNG:
                Bitmap bmp;
                if (data.length >= Constants.MAX_IMG_SIZE_BYTE) {
                    int scale = (int) Math.ceil((double) data.length / Constants.MAX_IMG_SIZE_BYTE);
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = scale;
                    bmp = BitmapFactory.decodeByteArray(data, 0, data.length, options);
                } else bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                photoView.setImageBitmap(bmp);
                break;
            case PDF:
                photoView.setVisibility(View.GONE);
                pdfView.setVisibility(View.VISIBLE);
                pdfView.fromBytes(data)
                        .enableSwipe(true)
                        .load();
                break;
        }
    }
}
