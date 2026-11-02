package com.fxdb.core.exception;

public class FxDbException extends RuntimeException {
    public FxDbException(String message) {
        super(message);
    }

    public FxDbException(String message, Throwable cause) {
        super(message, cause);
    }
}
