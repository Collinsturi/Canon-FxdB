package com.fxdb.transfer;

/**
 * Callbacks invoked from the transfer's background thread. A JavaFX caller must marshal these
 * back onto the FX thread itself (e.g. via Platform.runLater) - this interface has no
 * JavaFX dependency so it stays usable from plain unit tests too.
 */
public interface ProgressListener {

    void onTableStart(String tableName, long estimatedRows);

    void onBatchProgress(String tableName, long rowsDoneSoFar);

    void onTableComplete(TableTransferResult result);

    void onError(String tableName, String errorMessage);

    ProgressListener NO_OP = new ProgressListener() {
        @Override
        public void onTableStart(String tableName, long estimatedRows) {
        }

        @Override
        public void onBatchProgress(String tableName, long rowsDoneSoFar) {
        }

        @Override
        public void onTableComplete(TableTransferResult result) {
        }

        @Override
        public void onError(String tableName, String errorMessage) {
        }
    };
}
