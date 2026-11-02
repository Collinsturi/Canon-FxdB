package com.fxdb.core.exception;

public class TransferException extends FxDbException {
    public TransferException(String message) {
        super(message);
    }

    public TransferException(String message, Throwable cause) {
        super(message, cause);
    }
}
