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
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.NotNull;
import org.threeten.bp.LocalDate;

import java.util.ArrayList;
import java.util.List;

import care.data4life.fhir.stu3.model.Attachment;
import care.data4life.fhir.stu3.model.DocumentReference;
import care.data4life.fhir.stu3.model.DomainResource;
import care.data4life.sdk.Data4LifeClient;
import care.data4life.sdk.call.DataRecord;
import care.data4life.sdk.call.Task;
import care.data4life.sdk.data.DataResource;
import care.data4life.sdk.helpers.stu3.DocumentReferenceExtension;
import care.data4life.sdk.lang.D4LException;
import care.data4life.sdk.lang.DataRestrictionException;
import care.data4life.sdk.listener.Callback;
import care.data4life.sdk.listener.ResultListener;
import care.data4life.sdk.model.Record;

public class DocumentsActivity extends AppCompatActivity {

    private static final String TAG = DocumentsActivity.class.getSimpleName();

    private static final int INTENT_FILE_PICKER = 434;
    public List<Record<DomainResource>> records = new ArrayList<>();
    private Data4LifeClient client;
    private CRUDBenchmark benchmark;
    private FloatingActionButton mAddFAB;
    private CoordinatorLayout mRootCL;
    private TextView mLogout;
    private RecyclerView mDocumentsRV;
    private SwipeRefreshLayout mDocumentsSRL;
    private DocumentAdapter documentAdapter;
    private LayoutInflater inflater;
    private LinearLayoutManager mLayoutManager;
    private int offset = 0;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private LocalDate fromDate, toDate;
    private Task fetchTask;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_documents);

        client = Data4LifeClient.getInstance();
        benchmark = new CRUDBenchmark(client, this);

        mAddFAB = findViewById(R.id.addFAB);
        mRootCL = findViewById(R.id.documentsRootCL);
        mLogout = findViewById(R.id.logoutBTN);
        mDocumentsRV = findViewById(R.id.documentsRV);
        mDocumentsSRL = findViewById(R.id.documentsSRL);
        View addFabAppData = findViewById(R.id.addFABAppData);
        Toolbar toolbar = findViewById(R.id.toolbar);

        mLayoutManager = new LinearLayoutManager(DocumentsActivity.this, LinearLayoutManager.VERTICAL, false);
        mDocumentsRV.setLayoutManager(mLayoutManager);
        documentAdapter = new DocumentAdapter(new ArrayList<>());
        mDocumentsRV.setAdapter(documentAdapter);
        mDocumentsSRL.setOnRefreshListener(this::reloadList);
        mDocumentsRV.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                int lastvisibleitemposition = mLayoutManager.findLastVisibleItemPosition();
                if (lastvisibleitemposition == documentAdapter.getItemCount() - 1) {
                    if (!isLoading && !isLastPage) {
                        isLoading = true;
                        fetchDocuments(records.size());
                    }
                }
            }
        });

        inflater = this.getLayoutInflater();

        mAddFAB.setOnClickListener(view -> {
                    Intent intent = new Intent();
                    intent.setType("*/*");
                    intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "application/pdf"});
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(Intent.createChooser(intent, "Select images"), INTENT_FILE_PICKER);
                }
        );

        addFabAppData.setOnClickListener(view -> {
            fetchDataRecord();
            createNewDataRecord();
        });


        setSupportActionBar(toolbar);
        Snackbar.make(mRootCL, "You have signed in with Data4Life", Snackbar.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        reloadList();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (fetchTask.isActive()) fetchTask.cancel();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == INTENT_FILE_PICKER && resultCode == RESULT_OK && data != null) {
            List<Uri> images = Utils.extractUris(data);
            if (images.isEmpty()) {
                if (data.getData() != null) images.add(data.getData());
            }

            List<Attachment> attachments = new ArrayList<>();
            for (Uri uri : images) {
                Attachment attachment = null;
                try {
                    attachment = FHIRUtils.buildAttachmentFromUri(uri, this);
                } catch (DataRestrictionException.UnsupportedFileType unsupportedFileType) {
                } catch (DataRestrictionException.MaxDataSizeViolation maxDataSizeViolation) {
                }
                if (attachment == null) continue;
                attachments.add(attachment);
            }

            if (!attachments.isEmpty()) startDocumentNameDialog(attachments);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void startDocumentNameDialog(List<Attachment> attachments) {
        View inputTagView = inflater.inflate(R.layout.dialog_create_document, null);
        new AlertDialog.Builder(this)
                .setTitle("Create new document")
                .setMessage("Enter document title")
                .setView(inputTagView)
                .setPositiveButton("Add", (dialog, id) -> {
                    EditText editText = inputTagView.findViewById(R.id.tagsET);
                    if (editText.getText().length() >= 0) {
                        createNewDocument(editText.getText().toString().trim(), attachments);
                    }
                })
                .setNegativeButton("Cancel", (dialog, id) -> dialog.dismiss())
                .create()
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_documents, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.logoutBTN:
                new AlertDialog.Builder(this)
                        .setTitle("Logout")
                        .setMessage("Are you sure you want to sign out from Data4Life?")
                        .setPositiveButton("Logout", (dialog, id) -> {
                            client.logout(new Callback() {
                                @Override
                                public void onSuccess() {
                                    Log.d(TAG, "Logged out");
                                    finish();
                                }

                                @Override
                                public void onError(D4LException exception) {
                                    Log.e(TAG, exception.getMessage());
                                }
                            });
                        })
                        .setNegativeButton("Cancel", (dialog, id) -> dialog.dismiss())
                        .create()
                        .show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void reloadList() {
        runOnUiThread(() -> {
            records.clear();
            offset = 0;
            isLoading = false;
            isLastPage = false;
            fetchDocuments(offset);
            mDocumentsRV.smoothScrollToPosition(0);
        });

    }

    private void createNewDocument(String title, List<Attachment> attachments) {
        mDocumentsSRL.setRefreshing(true);
        DocumentReference document = FHIRUtils.buildDocument(title, attachments);

        client.createRecord(document, new ResultListener<Record<DocumentReference>>() {
            @Override
            public void onSuccess(Record<DocumentReference> record) {
                reloadList();
                runOnUiThread(() -> mDocumentsSRL.setRefreshing(false));
            }

            @Override
            public void onError(D4LException exception) {
                Log.e(TAG, exception.getMessage());
                runOnUiThread(() -> mDocumentsSRL.setRefreshing(false));
            }
        });
    }

    private void fetchDocuments(int offset) {
        mDocumentsSRL.setRefreshing(true);

        fetchTask = client.fetchRecords(
                DomainResource.class,
                fromDate,
                toDate,
                20,
                offset,
                new ResultListener<List<Record<DomainResource>>>() {
                    @Override
                    public void onSuccess(List<Record<DomainResource>> records) {
                        runOnUiThread(() -> {
                            DocumentsActivity.this.records.addAll(records);
                            if (records.size() == 0) {
                                isLastPage = true;
                            }
                            isLoading = false;
                            documentAdapter.bindDocuments(records);
                            mDocumentsSRL.setRefreshing(false);
                        });
                    }

                    @Override
                    public void onError(D4LException exception) {
                        Log.e(TAG, exception.getMessage());
                        runOnUiThread(() -> mDocumentsSRL.setRefreshing(false));
                    }
                });
    }

    class DocumentAdapter extends RecyclerView.Adapter<DocumentAdapter.DocumentViewHolder> {
        List<Record<DocumentReference>> documents;

        DocumentAdapter(List<Record<DocumentReference>> documents) {
            this.documents = documents;
        }

        void bindDocuments(List<Record<DomainResource>> documents) {
            List<Record<DocumentReference>> docRefs = new ArrayList<>();
            for (Record record : documents) {
                if (record.getResource() instanceof DocumentReference) {
                    docRefs.add(record);
                }
            }

            this.documents.clear();
            this.documents.addAll(docRefs);
            notifyDataSetChanged();
        }

        @Override
        public int getItemCount() {
            return documents.size();
        }

        @Override
        public DocumentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_document, parent, false);
            return new DocumentViewHolder(view);
        }

        @Override
        public void onBindViewHolder(DocumentViewHolder holder, int position) {
            holder.bindDocument(documents.get(position));
        }

        class DocumentViewHolder extends RecyclerView.ViewHolder {
            private static final int DELETE_ACTION = 0;

            private TextView mIdentifierTV;
            private ConstraintLayout mRootCL;

            DocumentViewHolder(View itemView) {
                super(itemView);
                mIdentifierTV = itemView.findViewById(R.id.identifierTV);
                mRootCL = itemView.findViewById(R.id.rootCL);
            }

            void bindDocument(Record<DocumentReference> record) {
                DocumentReference document = record.getFhirResource();
                String title = DocumentReferenceExtension.getTitle(document);

                mIdentifierTV.setText(title);
                mRootCL.setOnClickListener(view -> navigateToDetailsActivity(view.getContext(), record));
                mRootCL.setOnLongClickListener(view -> {
                    showDialog(record);
                    return true;
                });
            }

            private void navigateToDetailsActivity(Context ctx, Record<DocumentReference> record) {
                Intent intent = DocumentDetailsActivity.getCallingIntent(ctx, record);
                startActivity(intent);
            }

            private void showDialog(Record<DocumentReference> record) {
                new AlertDialog.Builder(DocumentsActivity.this)
                        .setTitle("Choose action")
                        .setItems(new String[]{"Delete"}, (dialog, actionPosition) -> {
                            switch (actionPosition) {
                                case DELETE_ACTION:
                                    deleteRecord(record);
                                    dialog.dismiss();
                                    break;
                                default:
                                    throw new RuntimeException("Unexpected case!");
                            }
                        })
                        .show();
            }

            private void deleteRecord(Record<DocumentReference> record) {
                int recordPosition = documents.indexOf(record);

                client.deleteRecord(record.getFhirResource().id, new Callback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            documents.remove(record);
                            notifyItemRemoved(recordPosition);
                            notifyItemRangeChanged(recordPosition, getItemCount());
                        });
                    }

                    @Override
                    public void onError(D4LException exception) {
                        Log.d(TAG, exception.getMessage());
                    }
                });
            }
        }
    }

    DataRecord appdata;
    List<String> annotations = new ArrayList<>();

    private void createNewDataRecord() {
        if (appdata != null) {
            return;
        }
        mDocumentsSRL.setRefreshing(true);
        byte[] data = new byte[1];
        DataResource dataResource = new DataResource(data);
        annotations.add("test");
        annotations.add("test2");
        annotations.add("test3");

        client.getData().create(dataResource, annotations, new care.data4life.sdk.call.Callback<care.data4life.sdk.call.DataRecord<DataResource>>() {

            @Override
            public void onSuccess(DataRecord<DataResource> result) {
                appdata = result;
                mDocumentsSRL.setRefreshing(false);
            }

            @Override
            public void onError(@NotNull D4LException exception) {
                mDocumentsSRL.setRefreshing(false);
            }
        });
    }

    private void fetchDataRecord() {
        if (appdata == null) {
            return;
        }
        mDocumentsSRL.setRefreshing(true);

        client.getData().fetch(appdata.getIdentifier(), new care.data4life.sdk.call.Callback<DataRecord<DataResource>>() {
            @Override
            public void onSuccess(DataRecord<DataResource> result) {
                runOnUiThread(() -> {
                    boolean equal = appdata.equals(result)
                            && annotations.equals(result.getAnnotations());
                    Toast.makeText(
                            getApplicationContext(),
                            "DonorKey test successful: " + equal, Toast.LENGTH_LONG
                    ).show();
                    mDocumentsSRL.setRefreshing(false);
                });
            }

            @Override
            public void onError(@NotNull D4LException exception) {
                Toast.makeText(getApplicationContext(), exception.getMessage(), Toast.LENGTH_LONG).show();
                mDocumentsSRL.setRefreshing(false);
            }
        });
    }
}
