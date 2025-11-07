
package com.incept5.workshop.stage2.tool;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.incept5.workshop.stage2.util.HttpHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Weather tool that calls the wttr.in API for real weather data.
 * 
 * wttr.in is a free, no-authentication-required weather service that provides
 * current weather conditions for any city worldwide.
 * 
 * Example API call:
 *   https://wttr.in/Paris?format=j1
 * 
 * Returns JSON with current conditions, temperature, humidity, wind, etc.
 */
public class WeatherTool implements Tool {
    private static final Logger logger = LoggerFactory.getLogger(WeatherTool.class);
    private static final String API_URL = "https://wttr.in/%s?format=j1";
    
    private final HttpHelper httpHelper;
    
    public WeatherTool() {
        this.httpHelper = new HttpHelper();
    }
    
    public WeatherTool(HttpHelper httpHelper) {
        this.httpHelper = httpHelper;
    }
    
    @Override
    public String getName() {
        return "weather";
    }
    
    @Override
    public String getDescription() {
        return """
                Gets current weather for a city.
                Parameters:
                  - city: Name of the city (e.g., "Paris", "Tokyo", "New York")
                Returns: Current temperature and conditions
                """;
    }
    
    @Override
    public String execute(Map<String, String> parameters) {
        String city = parameters.get("city");
        
        if (city == null || city.isBlank()) {
            return "Error: city parameter is required";
        }
        
        logger.info("Getting weather for: {}", city);
        
        try {
            String url = String.format(API_URL, city.replace(" ", "+"));
            String jsonResponse = httpHelper.get(url);
            
            return parseWeatherResponse(city, jsonResponse);
            
        } catch (HttpHelper.HttpException e) {
            logger.error("Failed to get weather for {}: {}", city, e.getMessage());
            return String.format("Error: Could not get weather for '%s'. %s", 
                    city, e.getMessage());
        }
    }
    
    /**
     * Parses the wttr.in JSON response and extracts key weather information.
     */
    private String parseWeatherResponse(String city, String jsonResponse) {
        try {
            JsonObject root = JsonParser.parseString(jsonResponse).getAsJsonObject();
            
            // Get current condition (first element in current_condition array)
            JsonArray currentCondition = root.getAsJsonArray("current_condition");
            if (currentCondition == null || currentCondition.isEmpty()) {
                return String.format("Error: No weather data available for '%s'", city);
            }
            
            JsonObject current = currentCondition.get(0).getAsJsonObject();
            
            // Extract temperature in Celsius
            String tempC = current.get("temp_C").getAsString();
            
            // Extract weather description
            JsonArray weatherDesc = current.getAsJsonArray("weatherDesc");
            String description = weatherDesc.get(0)
                    .getAsJsonObject()
                    .get("value")
                    .getAsString();
            
            // Extract additional details
            String feelsLikeC = current.get("FeelsLikeC").getAsString();
            String humidity = current.get("humidity").getAsString();
            String windSpeedKmph = current.get("windspeedKmph").getAsString();
            
            // Format a nice response
            return String.format(
                    "%s: %s°C (feels like %s°C), %s. Humidity: %s%%, Wind: %s km/h",
                    city, tempC, feelsLikeC, description, humidity, windSpeedKmph
            );
            
        } catch (Exception e) {
            logger.error("Failed to parse weather response: {}", e.getMessage());
            return String.format("Error: Could not parse weather data for '%s'", city);
        }
    }
}
