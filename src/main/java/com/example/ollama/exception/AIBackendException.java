package com.example.ollama.exception;

/**
 * Custom exception for AI backend-related errors using sealed classes (Java 17+).
 * Sealed classes restrict which classes can extend this exception.
 * Used by all backend implementations: Ollama, LM Studio, MLX-VLM
 */
public sealed class AIBackendException extends Exception
        permits AIBackendException.ConnectionException,
                AIBackendException.ModelNotFoundException,
                AIBackendException.InvalidResponseException {

    public AIBackendException(String message) {
        super(message);
    }

    public AIBackendException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Exception thrown when connection to backend server fails
     */
    public static final class ConnectionException extends AIBackendException {
        public ConnectionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Exception thrown when the requested model is not found
     */
    public static final class ModelNotFoundException extends AIBackendException {
        private final String modelName;

        public ModelNotFoundException(String modelName) {
            super("Model not found: " + modelName);
            this.modelName = modelName;
        }

        public String getModelName() {
            return modelName;
        }
    }

    /**
     * Exception thrown when the API response is invalid or cannot be parsed
     */
    public static final class InvalidResponseException extends AIBackendException {
        private final int statusCode;

        public InvalidResponseException(String message, int statusCode) {
            super(message + " (HTTP " + statusCode + ")");
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }
}
