package com.fxdb.transfer;

import javafx.application.Platform;
import javafx.concurrent.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Runs every TableJob in order off the FX thread, reporting progress through Task's own
 * thread-safe updateProgress()/updateMessage() plus two extra callbacks for things Task
 * doesn't model (per-table completion, log lines) - those are marshaled onto the FX thread
 * with Platform.runLater since they mutate UI-owned state the caller provides.
 */
public class TransferTask extends Task<MigrationRunResult> {

    private final List<TableJob> jobs;
    private final ErrorPolicy policy;
    private final int batchSize;
    private final Consumer<TableTransferResult> onTableComplete;
    private final Consumer<String> onLogLine;
    private final AtomicBoolean cancelFlag = new AtomicBoolean(false);

    private volatile long currentTableEstimate = -1;

    public TransferTask(List<TableJob> jobs, ErrorPolicy policy, int batchSize,
                         Consumer<TableTransferResult> onTableComplete, Consumer<String> onLogLine) {
        this.jobs = jobs;
        this.policy = policy;
        this.batchSize = batchSize;
        this.onTableComplete = onTableComplete;
        this.onLogLine = onLogLine;
    }

    /** Requests cooperative cancellation; the running table finishes its current batch, then stops. */
    public void requestCancel() {
        cancelFlag.set(true);
        cancel();
    }

    @Override
    protected MigrationRunResult call() {
        TransferEngine engine = new TransferEngine();
        List<TableTransferResult> results = new ArrayList<>();

        ProgressListener listener = new ProgressListener() {
            @Override
            public void onTableStart(String tableName, long estimatedRows) {
                currentTableEstimate = estimatedRows;
                updateMessage("Starting " + tableName);
            }

            @Override
            public void onBatchProgress(String tableName, long rowsDoneSoFar) {
                if (currentTableEstimate > 0) {
                    updateProgress(rowsDoneSoFar, currentTableEstimate);
                } else {
                    updateProgress(-1, -1);
                }
                updateMessage(tableName + ": " + rowsDoneSoFar + " rows transferred");
            }

            @Override
            public void onTableComplete(TableTransferResult result) {
                Platform.runLater(() -> onTableComplete.accept(result));
            }

            @Override
            public void onError(String tableName, String errorMessage) {
                Platform.runLater(() -> onLogLine.accept(errorMessage));
            }
        };

        for (TableJob job : jobs) {
            if (isCancelled()) {
                cancelFlag.set(true);
                break;
            }
            TableTransferResult result = engine.runWithProgress(
                    job.source(), job.sink(), job.mapping(), policy, listener, cancelFlag, batchSize);
            results.add(result);
            if (result.status() == TableTransferResult.Status.FAILED) {
                break;
            }
        }
        return new MigrationRunResult(results);
    }
}
