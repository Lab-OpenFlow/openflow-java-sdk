package io.github.labopenflow.sdk.exception;

public class OpenFlowApiException extends OpenFlowException {
    private final int statusCode;
    private final String responseBody;

    public OpenFlowApiException(int statusCode, String responseBody) {
        super("OpenFlow API request failed with HTTP " + statusCode + ": " + responseBody);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }
}