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
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import care.data4life.fhir.stu3.json.BigDecimalJsonAdapter;
import care.data4life.fhir.stu3.json.FhirDateJsonAdapter;
import care.data4life.fhir.stu3.json.FhirDateTimeJsonAdapter;
import care.data4life.fhir.stu3.json.FhirDecimalJsonAdapter;
import care.data4life.fhir.stu3.json.FhirInstantJsonAdapter;
import care.data4life.fhir.stu3.json.FhirStu3BaseAdapterFactory;
import care.data4life.fhir.stu3.json.FhirTimeJsonAdapter;
import care.data4life.fhir.stu3.json.FhirUrlJsonAdapter;
import care.data4life.fhir.stu3.model.Attachment;
import care.data4life.fhir.stu3.model.DocumentReference;
import care.data4life.fhir.stu3.model.FhirDate;
import care.data4life.fhir.stu3.model.FhirDateTime;
import care.data4life.fhir.stu3.model.FhirDecimal;
import care.data4life.fhir.stu3.model.FhirInstant;
import care.data4life.fhir.stu3.model.FhirTime;
import care.data4life.sdk.Data4LifeClient;
import care.data4life.sdk.call.Task;
import care.data4life.sdk.config.DataRestrictionException;
import care.data4life.sdk.helpers.stu3.AttachmentExtension;
import care.data4life.sdk.helpers.stu3.DocumentReferenceExtension;
import care.data4life.sdk.lang.D4LException;
import care.data4life.sdk.listener.ResultListener;
import care.data4life.sdk.model.DownloadType;
import care.data4life.sdk.model.Record;
import care.data4life.sdk.util.MimeType;

public class DocumentDetailsActivity extends AppCompatActivity {
    private static final String TAG = DocumentDetailsActivity.class.getSimpleName();
    private static final String RECORD_KEY = "RECORD_KEY";
    private static final String ID_SPLIT_CHAR = "#";
    private static final int INTENT_FILE_PICKER = 434;

    private Data4LifeClient client;
    private ActionBar actionBar;
    private ProgressBar progressBar;
    private RecyclerView mAttachmentsRL;
    private AttachmentsAdapter attachmentsAdapter;
    private DocumentReference document;
    private List<Uri> newAttachmentUris;
    private Task downloadTask;

    private static Moshi moshi = new Moshi.Builder()
            .add(new FhirStu3BaseAdapterFactory())
            .add(URL.class, new FhirUrlJsonAdapter().nullSafe())
            .add(BigDecimal.class, new BigDecimalJsonAdapter())
            .add(FhirDate.class, new FhirDateJsonAdapter().nullSafe())
            .add(FhirDateTime.class, new FhirDateTimeJsonAdapter().nullSafe())
            .add(FhirDecimal.class, new FhirDecimalJsonAdapter().nullSafe())
            .add(FhirInstant.class, new FhirInstantJsonAdapter().nullSafe())
            .add(FhirTime.class, new FhirTimeJsonAdapter().nullSafe())
            .build();
    private static JsonAdapter<Record<DocumentReference>> jsonAdapter = moshi.adapter(Types.newParameterizedType(Record.class, DocumentReference.class));

    static Intent getCallingIntent(Context ctx, Record<DocumentReference> record) {
        Intent i = new Intent(ctx, DocumentDetailsActivity.class);
        i.putExtra(RECORD_KEY, jsonAdapter.toJson(record));
        return i;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_details);
        String json = getIntent().getStringExtra(RECORD_KEY);
        try {
            document = jsonAdapter.fromJson(json).getFhirResource();
        } catch (IOException e) {
            Log.d(TAG, e.getMessage());
            throw new RuntimeException("Failed to deserialize passed object!");
        }

        client = Data4LifeClient.getInstance();

        setSupportActionBar(findViewById(R.id.toolbar));
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(DocumentReferenceExtension.getTitle(document));
        progressBar = findViewById(R.id.progressBar);
        mAttachmentsRL = findViewById(R.id.attachmentsRL);

        attachmentsAdapter = new AttachmentsAdapter(new ArrayList<>());
        mAttachmentsRL.setAdapter(attachmentsAdapter);
        mAttachmentsRL.setLayoutManager(new GridLayoutManager(this, 2));

        downloadAttachments(document.id, extractAttachmentIds(document), SuccessAction.RenderAttachments);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (newAttachmentUris != null) {
            List<Attachment> newAttachments = new ArrayList<>();
            for (Uri uri : newAttachmentUris) {
                Attachment attachment = null;
                try {
                    attachment = FHIRUtils.buildAttachmentFromUri(uri, this);
                } catch (DataRestrictionException.UnsupportedFileType unsupportedFileType) {
                    unsupportedFileType.printStackTrace();
                    Utils.showToastMessage(this, "Unsupported attachment file type selected!");
                } catch (DataRestrictionException.MaxDataSizeViolation maxDataSizeViolation) {
                    maxDataSizeViolation.printStackTrace();
                    Utils.showToastMessage(this, "Attachment file size limitation reached!");
                }
                if (attachment == null) continue;

                newAttachments.add(attachment);
                document.content.add(new DocumentReference.DocumentReferenceContent(attachment));
            }

            if (!newAttachments.isEmpty()) updateRecord(document, newAttachments);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (downloadTask.isActive()) downloadTask.cancel();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_details, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.addBTN:
                Intent intent = new Intent();
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "application/pdf"});
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(Intent.createChooser(intent, "Select images"), INTENT_FILE_PICKER);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == INTENT_FILE_PICKER && resultCode == RESULT_OK && data != null) {
            newAttachmentUris = Utils.extractUris(data);
            if (newAttachmentUris.isEmpty() && data.getData() != null)
                newAttachmentUris.add(data.getData());
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private enum SuccessAction {RenderAttachments, AppendAttachments}

    private void downloadAttachments(String recordId, List<String> attachmentIds, SuccessAction action) {
        setProgressBarVisibility(View.VISIBLE);

        downloadTask = client.downloadAttachments(recordId, attachmentIds, DownloadType.Small, new ResultListener<List<Attachment>>() {
            @Override
            public void onSuccess(List<Attachment> attachments) {
                switch (action) {
                    case RenderAttachments:
                        runOnUiThread(() -> renderAttachments(attachments));
                        break;
                    case AppendAttachments:
                        runOnUiThread(() -> appendAttachments(attachments));
                        break;
                    default:
                        throw new IllegalStateException("Unexpected case!");
                }
                runOnUiThread(() -> setProgressBarVisibility(View.GONE));
            }

            @Override
            public void onError(D4LException exception) {
                Log.d(TAG, exception.getMessage());
                runOnUiThread(() -> setProgressBarVisibility(View.GONE));
            }
        });
    }

    private List<String> extractAttachmentIds(DocumentReference document) {
        return extractAttachmentIds(DocumentReferenceExtension.getAttachments(document));
    }

    private List<String> extractAttachmentIds(List<Attachment> attachments) {
        if (attachments == null) return new ArrayList<>(0);

        List<String> ids = new ArrayList<>();
        for (Attachment attachment : attachments) ids.add(attachment.id);
        return ids;
    }

    private void updateRecord(DocumentReference resource, List<Attachment> newAttachments) {
        runOnUiThread(() -> setProgressBarVisibility(View.VISIBLE));

        client.updateRecord(resource, new ResultListener<Record<DocumentReference>>() {
            @Override
            public void onSuccess(Record<DocumentReference> record) {
                DocumentDetailsActivity.this.document = record.getFhirResource();
                newAttachmentUris = null;

                if (newAttachments != null)
                    downloadAttachments(document.id, extractAttachmentIds(newAttachments), SuccessAction.AppendAttachments);
                runOnUiThread(() -> setProgressBarVisibility(View.GONE));
            }

            @Override
            public void onError(D4LException exception) {
                Log.d(TAG, exception.getMessage());
                runOnUiThread(() -> setProgressBarVisibility(View.GONE));
            }
        });
    }

    private final int FULL_ATTACHMENT_POS = 0;

    private void removeAttachmentFromDocument(DocumentReference document, String attachmentId) {
        Iterator<DocumentReference.DocumentReferenceContent> iterator = document.content.iterator();
        String searchId = attachmentId.contains(ID_SPLIT_CHAR) ? attachmentId.split(ID_SPLIT_CHAR)[FULL_ATTACHMENT_POS] : attachmentId;

        while (iterator.hasNext()) {
            DocumentReference.DocumentReferenceContent content = iterator.next();
            if (content.attachment == null || content.attachment.id == null) continue;

            if (content.attachment.id.equals(searchId)) iterator.remove();
        }
    }

    private void setProgressBarVisibility(int visibility) {
        progressBar.setVisibility(visibility);
    }

    private void renderAttachments(List<Attachment> attachments) {
        attachmentsAdapter.bindAttachments(attachments);
    }

    private void appendAttachments(List<Attachment> attachments) {
        attachmentsAdapter.appendAttachments(attachments);
    }

    private class AttachmentsAdapter extends RecyclerView.Adapter<AttachmentsAdapter.AttachmentsViewHolder> {
        private static final int ACTION_DELETE = 0;
        private List<Attachment> attachments;
        private PdfiumCore pdfiumCore;

        AttachmentsAdapter(List<Attachment> attachments) {
            this.attachments = attachments;
            pdfiumCore = new PdfiumCore(getBaseContext());
        }

        void bindAttachments(List<Attachment> attachments) {
            this.attachments.clear();
            this.attachments.addAll(attachments);
            notifyDataSetChanged();
        }

        void appendAttachments(List<Attachment> attachments) {
            int insertPosition = this.attachments.size();
            this.attachments.addAll(attachments);
            notifyItemRangeInserted(insertPosition, attachments.size());
        }

        @Override
        public int getItemCount() {
            return attachments.size();
        }

        @Override
        public AttachmentsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_attachment, parent, false);
            return new AttachmentsViewHolder(view);
        }

        @Override
        public void onBindViewHolder(AttachmentsViewHolder holder, int position) {
            holder.bindAttachment(attachments.get(position));
        }

        class AttachmentsViewHolder extends RecyclerView.ViewHolder {
            private TextView mAttachmentTV;
            private ImageView mAttachmentIV;

            AttachmentsViewHolder(View itemView) {
                super(itemView);
                mAttachmentTV = itemView.findViewById(R.id.attachmentTV);
                mAttachmentIV = itemView.findViewById(R.id.attachmentIV);

            }

            void bindAttachment(Attachment attachment) {
                byte[] data = AttachmentExtension.getData(attachment);
                if (data != null && data.length > 0) {
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
                            mAttachmentIV.setImageBitmap(bmp);
                            break;
                        case PDF:
                            PdfDocument pdfDocument = null;
                            try {
                                pdfDocument = pdfiumCore.newDocument(data);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            pdfiumCore.openPage(pdfDocument, 0);
                            int width = pdfiumCore.getPageWidthPoint(pdfDocument, 0);
                            int height = pdfiumCore.getPageHeightPoint(pdfDocument, 0);
                            Bitmap pdfBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                            pdfiumCore.renderPageBitmap(pdfDocument, pdfBitmap, 0, 0, 0, width, height);
                            mAttachmentIV.setImageBitmap(pdfBitmap);
                            break;
                        default:
                            //ignore
                    }
                }
                mAttachmentTV.setText(attachment.title);
                mAttachmentIV.setOnLongClickListener(view -> {
                    showActionDialog(view.getContext(), attachment);
                    return true;
                });
                mAttachmentIV.setOnClickListener(view -> openAttachmentPreview(view.getContext(), attachment));
            }

        }

        private void showActionDialog(Context ctx, Attachment attachment) {
            new AlertDialog.Builder(ctx)
                    .setTitle("Choose action")
                    .setItems(new String[]{"Delete"}, (dialog, actionPosition) -> {
                        switch (actionPosition) {
                            case ACTION_DELETE:
                                deleteAttachment(attachment);
                                dialog.dismiss();
                                break;
                            default:
                                throw new RuntimeException("Unexpected case!");
                        }
                    })
                    .show();
        }

        private void deleteAttachment(Attachment attachment) {
            int attachmentPosition = attachments.indexOf(attachment);
            attachments.remove(attachment);
            notifyItemRemoved(attachmentPosition);
            removeAttachmentFromDocument(document, attachment.id);
            updateRecord(document, null);
        }

        private void openAttachmentPreview(Context context, Attachment attachment) {
            String attachmentId = attachment.id.contains(ID_SPLIT_CHAR) ? attachment.id.split(ID_SPLIT_CHAR)[0] : attachment.id;
            Intent i = AttachmentPreviewActivity.getCallingIntent(DocumentDetailsActivity.this, document.id, attachmentId);
            startActivity(i);
        }
    }
}
