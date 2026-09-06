package io.github.labopenflow.sdk;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RequestOptions {
    private final String idempotencyKey;
    private final String correlationId;
    private final Map<String, String> headers;

    private RequestOptions(Builder builder) {
        this.idempotencyKey = builder.idempotencyKey;
        this.correlationId = builder.correlationId;
        this.headers = Collections.unmodifiableMap(new HashMap<>(builder.headers));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static RequestOptions empty() {
        return builder().build();
    }

    public static RequestOptions withIdempotencyKey(String idempotencyKey) {
        return builder().idempotencyKey(idempotencyKey).build();
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public static class Builder {
        private String idempotencyKey;
        private String correlationId;
        private final Map<String, String> headers = new HashMap<>();

        public Builder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder header(String key, String value) {
            this.headers.put(key, value);
            return this;
        }

        public RequestOptions build() {
            return new RequestOptions(this);
        }
    }
}