package io.github.labopenflow.sdk;

import java.time.Duration;

public class OpenFlowConfig {
    private final String baseUrl;
    private final String apiKey;
    private final String bearerToken;
    private final Duration timeout;

    private OpenFlowConfig(Builder builder) {
        this.baseUrl = builder.baseUrl.endsWith("/") 
            ? builder.baseUrl.substring(0, builder.baseUrl.length() - 1) 
            : builder.baseUrl;
        this.apiKey = builder.apiKey;
        this.bearerToken = builder.bearerToken;
        this.timeout = builder.timeout != null ? builder.timeout : Duration.ofSeconds(30);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getBearerToken() {
        return bearerToken;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public static class Builder {
        private String baseUrl = "http://localhost:8080";
        private String apiKey;
        private String bearerToken;
        private Duration timeout = Duration.ofSeconds(30);

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder bearerToken(String bearerToken) {
            this.bearerToken = bearerToken;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public OpenFlowConfig build() {
            if (baseUrl == null || baseUrl.isBlank()) {
                throw new IllegalArgumentException("baseUrl must not be empty");
            }
            return new OpenFlowConfig(this);
        }
    }
}