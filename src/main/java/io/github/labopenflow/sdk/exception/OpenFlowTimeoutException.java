package io.github.labopenflow.sdk.exception;

public class OpenFlowTimeoutException extends OpenFlowException {
    public OpenFlowTimeoutException(String message) {
        super(message);
    }

    public OpenFlowTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}