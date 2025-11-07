
package com.incept5.workshop.stage2.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Simple HTTP helper for making GET requests to REST APIs.
 * 
 * Wraps Java 11+ HttpClient with sensible defaults for calling public APIs.
 * Uses synchronous requests for simplicity - perfect for tool calling where
 * we want to wait for the result before continuing.
 */
public class HttpHelper {
    private static final Logger logger = LoggerFactory.getLogger(HttpHelper.class);
    
    private final HttpClient httpClient;
    private final Duration timeout;
    
    /**
     * Creates a new HTTP helper with default 10-second timeout.
     */
    public HttpHelper() {
        this(Duration.ofSeconds(10));
    }
    
    /**
     * Creates a new HTTP helper with custom timeout.
     * 
     * @param timeout maximum time to wait for a response
     */
    public HttpHelper(Duration timeout) {
        this.timeout = timeout;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }
    
    /**
     * Performs a GET request to the specified URL.
     * 
     * @param url the URL to request
     * @return the response body as a string
     * @throws HttpException if the request fails or returns non-2xx status
     */
    public String get(String url) throws HttpException {
        logger.debug("GET {}", url);
        
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(timeout)
                    .header("User-Agent", "W-JAX-Workshop-Agent/1.0")
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(
                    request, 
                    HttpResponse.BodyHandlers.ofString()
            );
            
            int statusCode = response.statusCode();
            String body = response.body();
            
            logger.debug("Response status: {}", statusCode);
            
            if (statusCode >= 200 && statusCode < 300) {
                return body;
            } else {
                throw new HttpException(
                        String.format("HTTP %d: %s", statusCode, body)
                );
            }
            
        } catch (HttpException e) {
            throw e;
        } catch (Exception e) {
            logger.error("HTTP request failed: {}", e.getMessage());
            throw new HttpException("Request failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Exception thrown when HTTP requests fail.
     */
    public static class HttpException extends Exception {
        public HttpException(String message) {
            super(message);
        }
        
        public HttpException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
