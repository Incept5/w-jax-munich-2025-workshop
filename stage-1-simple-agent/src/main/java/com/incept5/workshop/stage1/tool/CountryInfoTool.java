
package com.incept5.workshop.stage1.tool;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.incept5.workshop.stage1.util.HttpHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Country information tool that calls the REST Countries API.
 * 
 * REST Countries is a free, no-authentication-required API that provides
 * comprehensive information about countries worldwide.
 * 
 * Example API call:
 *   https://restcountries.com/v3.1/name/france
 * 
 * Returns JSON with country details: capital, population, languages, currencies, etc.
 */
public class CountryInfoTool implements Tool {
    private static final Logger logger = LoggerFactory.getLogger(CountryInfoTool.class);
    private static final String API_URL = "https://restcountries.com/v3.1/name/%s";
    
    private final HttpHelper httpHelper;
    
    public CountryInfoTool() {
        this.httpHelper = new HttpHelper();
    }
    
    public CountryInfoTool(HttpHelper httpHelper) {
        this.httpHelper = httpHelper;
    }
    
    @Override
    public String getName() {
        return "country_info";
    }
    
    @Override
    public String getDescription() {
        return """
                Gets information about a country.
                Parameters:
                  - country: Name of the country (e.g., "France", "Japan", "Brazil")
                Returns: Capital, population, region, and other details
                """;
    }
    
    @Override
    public String execute(Map<String, String> parameters) {
        String country = parameters.get("country");
        
        if (country == null || country.isBlank()) {
            return "Error: country parameter is required";
        }
        
        logger.info("Getting country info for: {}", country);
        
        try {
            String url = String.format(API_URL, country.replace(" ", "%20"));
            String jsonResponse = httpHelper.get(url);
            
            return parseCountryResponse(country, jsonResponse);
            
        } catch (HttpHelper.HttpException e) {
            logger.error("Failed to get country info for {}: {}", country, e.getMessage());
            return String.format("Error: Could not find information for country '%s'. %s", 
                    country, e.getMessage());
        }
    }
    
    /**
     * Parses the REST Countries API JSON response and extracts key information.
     */
    private String parseCountryResponse(String country, String jsonResponse) {
        try {
            // API returns an array of matching countries, take the first one
            JsonArray results = JsonParser.parseString(jsonResponse).getAsJsonArray();
            
            if (results.isEmpty()) {
                return String.format("Error: No information found for country '%s'", country);
            }
            
            JsonObject countryData = results.get(0).getAsJsonObject();
            
            // Extract common name
            String commonName = countryData.getAsJsonObject("name")
                    .getAsJsonObject("common")
                    .getAsString();
            
            // Extract capital (might be an array)
            String capital = "N/A";
            if (countryData.has("capital")) {
                JsonArray capitalArray = countryData.getAsJsonArray("capital");
                if (!capitalArray.isEmpty()) {
                    capital = capitalArray.get(0).getAsString();
                }
            }
            
            // Extract population
            long population = countryData.get("population").getAsLong();
            
            // Extract region
            String region = countryData.get("region").getAsString();
            
            // Extract subregion
            String subregion = "N/A";
            if (countryData.has("subregion")) {
                subregion = countryData.get("subregion").getAsString();
            }
            
            // Extract languages (might be multiple)
            String languages = extractLanguages(countryData);
            
            // Format a comprehensive response
            return String.format(
                    "%s - Capital: %s, Population: %,d, Region: %s (%s), Languages: %s",
                    commonName, capital, population, region, subregion, languages
            );
            
        } catch (Exception e) {
            logger.error("Failed to parse country response: {}", e.getMessage());
            return String.format("Error: Could not parse country data for '%s'", country);
        }
    }
    
    /**
     * Extracts languages from the country data.
     * Languages are stored as an object with language codes as keys.
     */
    private String extractLanguages(JsonObject countryData) {
        if (!countryData.has("languages")) {
            return "N/A";
        }
        
        try {
            JsonObject languagesObj = countryData.getAsJsonObject("languages");
            StringBuilder languages = new StringBuilder();
            
            int count = 0;
            for (Map.Entry<String, JsonElement> entry : languagesObj.entrySet()) {
                if (count > 0) {
                    languages.append(", ");
                }
                languages.append(entry.getValue().getAsString());
                count++;
                
                // Limit to first 3 languages to keep response concise
                if (count >= 3) {
                    if (languagesObj.size() > 3) {
                        languages.append(", ...");
                    }
                    break;
                }
            }
            
            return languages.toString();
            
        } catch (Exception e) {
            return "N/A";
        }
    }
}
