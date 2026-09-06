package io.github.labopenflow.sdk.exception;

public class OpenFlowException extends RuntimeException {
    public OpenFlowException(String message) {
        super(message);
    }

    public OpenFlowException(String message, Throwable cause) {
        super(message, cause);
    }
}