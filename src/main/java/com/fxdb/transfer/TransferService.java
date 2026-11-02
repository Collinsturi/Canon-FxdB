package com.fxdb.transfer;

import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.util.List;
import java.util.function.Consumer;

/** JavaFX Service wrapper around TransferTask, giving the UI restart()/cancel() and bindable state properties. */
public class TransferService extends Service<MigrationRunResult> {

    private final List<TableJob> jobs;
    private final ErrorPolicy policy;
    private final int batchSize;
    private final Consumer<TableTransferResult> onTableComplete;
    private final Consumer<String> onLogLine;

    private TransferTask currentTask;

    public TransferService(List<TableJob> jobs, ErrorPolicy policy, int batchSize,
                            Consumer<TableTransferResult> onTableComplete, Consumer<String> onLogLine) {
        this.jobs = jobs;
        this.policy = policy;
        this.batchSize = batchSize;
        this.onTableComplete = onTableComplete;
        this.onLogLine = onLogLine;
    }

    @Override
    protected Task<MigrationRunResult> createTask() {
        currentTask = new TransferTask(jobs, policy, batchSize, onTableComplete, onLogLine);
        return currentTask;
    }

    public void requestCancel() {
        if (currentTask != null) {
            currentTask.requestCancel();
        }
    }
}
