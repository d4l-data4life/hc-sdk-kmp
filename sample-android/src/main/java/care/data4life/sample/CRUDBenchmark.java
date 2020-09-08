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

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.threeten.bp.LocalDate;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.core.content.ContextCompat;
import care.data4life.fhir.stu3.model.CarePlan;
import care.data4life.fhir.stu3.model.DiagnosticReport;
import care.data4life.fhir.stu3.model.DocumentReference;
import care.data4life.fhir.stu3.model.DomainResource;
import care.data4life.fhir.stu3.model.FhirElementFactory;
import care.data4life.fhir.stu3.model.Observation;
import care.data4life.sdk.Data4LifeClient;
import care.data4life.sdk.lang.D4LException;
import care.data4life.sdk.listener.ResultListener;
import care.data4life.sdk.model.CreateResult;
import care.data4life.sdk.model.DeleteResult;
import care.data4life.sdk.model.Record;

public class CRUDBenchmark {
    public static final String TAG = CRUDBenchmark.class.getSimpleName();

    private static final String LOG_FILE_NAME = "gc_log.txt";
    private static final int ITERATIONS = 1;
    private static final int[] RECORDS_NUMBER = {1};
    private static final int WAITING_TIME_MS = 5000;

    private Object lock = new Object();
    private int failedOperationsCnt = 0;
    private List<String> recordIds = new ArrayList<>();

    private Data4LifeClient client;
    private Context context;
    private Handler mainThread = new Handler(Looper.getMainLooper());
    private Thread jobExecutor;
    private PrintWriter printWriter;


    public CRUDBenchmark(Data4LifeClient client, Context ctx) {
        this.client = client;
        this.context = ctx;
    }

    public void start() {
        if (jobExecutor != null && jobExecutor.isAlive()) {
            showToastMsg("Benchmark is already running!");
            return;
        }
        showToastMsg("Benchmark started!");

        jobExecutor = new JobExecutor(createJob, fetchJob);
        jobExecutor.start();
    }

    public void stop() {
        if (jobExecutor != null && jobExecutor.isAlive()) {
            if (jobExecutor.isInterrupted()) showToastMsg("Benchmark is stopping!");
            else {
                jobExecutor.interrupt();
                showToastMsg("Benchmark stopped!");
            }
        } else showToastMsg("Benchmark is not running!");
    }

    private class JobExecutor extends Thread {
        private Runnable[] jobs;
        private Thread t;

        JobExecutor(Runnable... jobs) {
            this.jobs = jobs;
        }

        @Override
        public void run() {
            Log.d(TAG, "JobExecutor started!");
            createLog();

            for (Runnable job : jobs) {
                writeToLog("New job started!");
                t = new Thread(job);
                t.start();
                try {
                    while (t.isAlive()) {
                        t.join(WAITING_TIME_MS);
                    }
                } catch (InterruptedException e) {
                    Log.e(TAG, "JobExecutor benchmark was interrupted!");
                    break;
                }
            }

            cleanup();
            closeLog();
            Log.d(TAG, "JobExecutor finished!");
        }

        private void cleanup() {
            if (t.isAlive()) t.interrupt();
            while (t.isAlive()) {
                try {
                    t.join(WAITING_TIME_MS);
                } catch (InterruptedException ignore) {
                }
            }

            if (!recordIds.isEmpty()) {
                client.deleteRecords(new ArrayList<>(recordIds), deleteListener);
                recordIds.clear();
            }
        }

        private ResultListener<DeleteResult> deleteListener = new ResultListener<DeleteResult>() {
            @Override
            public void onSuccess(DeleteResult deleteResult) {
                String msg = "Cleanup finished";
                if (!deleteResult.getFailedDeletes().isEmpty()) {
                    msg += ", " + deleteResult.getFailedDeletes().size() + " record/s failed to delete.";
                }
                Log.d(TAG, msg);
            }

            @Override
            public void onError(D4LException e) {
                Log.e(TAG, "CleanUp failed\n" + e.getMessage());
            }
        };
    }

    private abstract class CRUDJob implements Runnable {
        protected int recordsNum = -1;

        @Override
        public void run() {
            List<DomainResource> testResources = buildTestResources();
            long[] measuredTimes = new long[ITERATIONS];
            int[] failedOperations = new int[ITERATIONS];

            for (DomainResource resource : testResources) {
                for (int records : RECORDS_NUMBER) {

                    recordsNum = records;
                    for (int i = 0; i < ITERATIONS; i++) {

                        long startMS = System.currentTimeMillis();

                        boolean finishedSuccessfully = crudCall(resource);

                        if (!finishedSuccessfully) return; //Operation was interrupted
                        else {
                            measuredTimes[i] = haveAllOperationsFailed() ? -1 : System.currentTimeMillis() - startMS;
                            failedOperations[i] = failedOperationsCnt;
                            failedOperationsCnt = 0;
                        }
                    }
                    printStatistics(resource.getResourceType(), records, measuredTimes, failedOperations);
                }
            }
        }

        public abstract boolean crudCall(DomainResource resource);

        private boolean haveAllOperationsFailed() {
            return failedOperationsCnt == recordsNum;
        }

    }

    private Runnable createJob = new CRUDJob() {
        @Override
        public boolean crudCall(DomainResource resource) {
            for (int r = 0; r < recordsNum; r++) {
                if (Thread.interrupted()) return true;
                if (resource instanceof DocumentReference) {
                    ((DocumentReference) resource).content.get(0).attachment.id = null;
                }

                client.createRecord(resource, createListener);
                if (!pauseCurrentThread("CreateJob")) return false;
            }
            return true;
        }

        private ResultListener<Record<DomainResource>> createListener = new ResultListener<Record<DomainResource>>() {
            @Override
            public void onSuccess(Record<DomainResource> record) {
                recordIds.add(record.getFhirResource().id);
                resumeWaitingThread();
            }

            @Override
            public void onError(D4LException e) {
                ++failedOperationsCnt;
                resumeWaitingThread();
            }
        };
    };

    private Runnable batchCreateJob = new CRUDJob() {
        @Override
        public boolean crudCall(DomainResource resource) {
            client.createRecords(generateResourceList(resource.getResourceType(), recordsNum), batchCreateListener);
            return pauseCurrentThread("BatchCreateJob");
        }

        private ResultListener<CreateResult<DomainResource>> batchCreateListener = new ResultListener<CreateResult<DomainResource>>() {
            @Override
            public void onSuccess(CreateResult<DomainResource> result) {
                failedOperationsCnt = result.getFailedOperations().size();
                for (Record<DomainResource> r : result.getSuccessfulOperations()) {
                    recordIds.add(r.getFhirResource().id);
                }
                resumeWaitingThread();
            }

            @Override
            public void onError(D4LException e) {
                failedOperationsCnt = recordsNum; //all operations have failed
                resumeWaitingThread();
            }
        };
    };

    private Runnable fetchJob = new CRUDJob() {
        private List<Record<DomainResource>> fetchedRecords;

        @Override
        public boolean crudCall(DomainResource resource) {
            fetchedRecords = null;
            Class clazz = FhirElementFactory.getClassForFhirType(resource.getResourceType());
            client.fetchRecords(clazz, LocalDate.now(), null, recordsNum, 0, fetchListener);
            if (!pauseCurrentThread("FetchJob")) return false;

            if (resource instanceof DocumentReference && fetchedRecords != null) {
                for (Record<DomainResource> record : fetchedRecords) {
                    client.downloadRecord(record.getFhirResource().id, downloadListener);
                    if (!pauseCurrentThread("DownloadJob")) return false;
                }
            }
            return true;
        }

        private ResultListener<List<Record<DomainResource>>> fetchListener = new ResultListener<List<Record<DomainResource>>>() {
            @Override
            public void onSuccess(List<Record<DomainResource>> result) {
                fetchedRecords = result;
                resumeWaitingThread();
            }

            @Override
            public void onError(D4LException e) {
                failedOperationsCnt = recordsNum;
                resumeWaitingThread();
            }
        };

        private ResultListener<Record<DomainResource>> downloadListener = new ResultListener<Record<DomainResource>>() {
            @Override
            public void onSuccess(Record<DomainResource> result) {
                resumeWaitingThread();
            }

            @Override
            public void onError(D4LException e) {
                ++failedOperationsCnt;
                resumeWaitingThread();
            }
        };
    };

    private List<DomainResource> buildTestResources() {
        return Arrays.asList(
                FHIRModelFactory.getTestObservation(),
                FHIRModelFactory.getTestCarePlan(),
                FHIRModelFactory.getTestDocumentReference(),
                FHIRModelFactory.getTestObservationSampledData(),
                FHIRModelFactory.getTestDiagnosticReport()
        );
    }

    private DomainResource buildResourceFromType(String resourceType) {
        switch (resourceType) {
            case Observation.resourceType:
                return FHIRModelFactory.getTestObservation();
            case CarePlan.resourceType:
                return FHIRModelFactory.getTestCarePlan();
            case DocumentReference.resourceType:
                return FHIRModelFactory.getTestDocumentReference();
            case DiagnosticReport.resourceType:
                return FHIRModelFactory.getTestDiagnosticReport();
            default:
                throw new RuntimeException("Unexpected case!");
        }
    }

    private List<DomainResource> generateResourceList(String resourceType, int numOfResource) {
        List<DomainResource> result = new ArrayList<>();
        for (int i = 0; i < numOfResource; i++) result.add(buildResourceFromType(resourceType));
        return result;
    }

    private boolean pauseCurrentThread(String jobName) {
        synchronized (lock) {
            try {
                lock.wait();
            } catch (InterruptedException e) {
                Log.e(TAG, jobName + " was interrupted!");
                return false;
            }
        }
        return true;
    }

    private void resumeWaitingThread() {
        synchronized (lock) {
            lock.notify();
        }
    }

    private void printStatistics(String resourceType, long numOfRecords, long[] measuredTimes, int[] failedOperations) {
        writeToLog("\n" + resourceType + ": " + numOfRecords + " record/s");

        long totalTimeMS = 0;
        StringBuilder sb = new StringBuilder();
        int numberOfSuccessfulIterations = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            if (failedOperations[i] == 0) { // in case of SocketTimeoutException measuredTimes could be significantly inflated with failing requests so only fully successful batches are taking into calculating average time
                numberOfSuccessfulIterations++;
                totalTimeMS += measuredTimes[i];
            }
            sb.append(measuredTimes[i] == -1 ? "?" : measuredTimes[i]).append("ms");
            if (failedOperations[i] != 0)
                sb.append(" - ").append(failedOperations[i]).append(" failed,");
            else sb.append(", ");
        }

        float averageTimeMS = (numberOfSuccessfulIterations != 0 ? (float) totalTimeMS / numberOfSuccessfulIterations : -1f);
        writeToLog("\t\tMeasured times:" + sb.toString() + "\n"
                + "\t\tOn average took:" + averageTimeMS + "ms");
    }

    private void createLog() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            String msg = "Please enable storage permission through app settings!";
            Log.d(TAG, msg);
            showToastMsg(msg);
            return;
        }
        File log = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), LOG_FILE_NAME);

        if (log.exists()) log.delete();
        try {
            log.createNewFile();
            printWriter = new PrintWriter(log);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            if (printWriter != null) printWriter.close();
        }
    }

    private void closeLog() {
        if (printWriter == null) return;

        printWriter.flush();
        printWriter.close();
    }

    private void writeToLog(String msg) {
        Log.d(TAG, msg);
        if (printWriter == null) return;

        printWriter.println(msg);
        printWriter.flush();
    }

    private void showToastMsg(String msg) {
        mainThread.post(() -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show());
    }
}
