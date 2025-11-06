package com.incept5.workshop.stage2.tool;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Weather tool that fetches real weather data from wttr.in API.
 * 
 * This is an MCP-compatible version of the WeatherTool from Stage 1,
 * now exposing a JSON schema for parameter validation.
 */
public class WeatherTool implements Tool {
    private static final Logger logger = LoggerFactory.getLogger(WeatherTool.class);
    private static final String API_URL = "https://wttr.in/%s?format=j1";
    
    private final HttpClient httpClient;
    
    public WeatherTool() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }
    
    @Override
    public String getName() {
        return "weather";
    }
    
    @Override
    public String getDescription() {
        return "Gets current weather conditions for a specified city. " +
               "Returns temperature, conditions, humidity, and wind speed.";
    }
    
    @Override
    public String getParameterSchema() {
        return """
                {
                  "type": "object",
                  "properties": {
                    "city": {
                      "type": "string",
                      "description": "Name of the city (e.g., 'Paris', 'Tokyo', 'New York')"
                    }
                  },
                  "required": ["city"]
                }
                """;
    }
    
    @Override
    public String execute(Map<String, String> parameters) throws ToolExecutionException {
        String city = parameters.get("city");
        
        if (city == null || city.isBlank()) {
            throw new ToolExecutionException("city parameter is required");
        }
        
        logger.info("Fetching weather for: {}", city);
        
        try {
            String url = String.format(API_URL, city.replace(" ", "+"));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                throw new ToolExecutionException(
                        "Weather API returned status " + response.statusCode());
            }
            
            return parseWeatherResponse(city, response.body());
            
        } catch (IOException | InterruptedException e) {
            logger.error("Failed to fetch weather for {}: {}", city, e.getMessage());
            throw new ToolExecutionException("Could not fetch weather for '" + city + "'", e);
        }
    }
    
    /**
     * Parses the wttr.in JSON response and extracts key weather information.
     */
    private String parseWeatherResponse(String city, String jsonResponse) 
            throws ToolExecutionException {
        try {
            JsonObject root = JsonParser.parseString(jsonResponse).getAsJsonObject();
            
            JsonArray currentCondition = root.getAsJsonArray("current_condition");
            if (currentCondition == null || currentCondition.isEmpty()) {
                throw new ToolExecutionException("No weather data available for '" + city + "'");
            }
            
            JsonObject current = currentCondition.get(0).getAsJsonObject();
            
            String tempC = current.get("temp_C").getAsString();
            JsonArray weatherDesc = current.getAsJsonArray("weatherDesc");
            String description = weatherDesc.get(0)
                    .getAsJsonObject()
                    .get("value")
                    .getAsString();
            String feelsLikeC = current.get("FeelsLikeC").getAsString();
            String humidity = current.get("humidity").getAsString();
            String windSpeedKmph = current.get("windspeedKmph").getAsString();
            
            return String.format(
                    "%s: %s°C (feels like %s°C), %s. Humidity: %s%%, Wind: %s km/h",
                    city, tempC, feelsLikeC, description, humidity, windSpeedKmph
            );
            
        } catch (Exception e) {
            logger.error("Failed to parse weather response: {}", e.getMessage());
            throw new ToolExecutionException("Could not parse weather data for '" + city + "'", e);
        }
    }
}
