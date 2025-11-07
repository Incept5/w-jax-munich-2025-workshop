package com.incept5.workshop.stage3.tool;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Country information tool that fetches data from REST Countries API.
 * 
 * This is an MCP-compatible version of the CountryInfoTool from Stage 1,
 * now exposing a JSON schema for parameter validation.
 */
public class CountryInfoTool implements Tool {
    private static final Logger logger = LoggerFactory.getLogger(CountryInfoTool.class);
    private static final String API_URL = "https://restcountries.com/v3.1/name/%s";
    
    private final HttpClient httpClient;
    
    public CountryInfoTool() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }
    
    @Override
    public String getName() {
        return "country_info";
    }
    
    @Override
    public String getDescription() {
        return "Gets detailed information about a country including capital, population, " +
               "region, languages, and currencies.";
    }
    
    @Override
    public String getParameterSchema() {
        return """
                {
                  "type": "object",
                  "properties": {
                    "country": {
                      "type": "string",
                      "description": "Name of the country (e.g., 'France', 'Japan', 'Brazil')"
                    }
                  },
                  "required": ["country"]
                }
                """;
    }
    
    @Override
    public String execute(Map<String, String> parameters) throws ToolExecutionException {
        String country = parameters.get("country");
        
        if (country == null || country.isBlank()) {
            throw new ToolExecutionException("country parameter is required");
        }
        
        logger.info("Fetching information for country: {}", country);
        
        try {
            String url = String.format(API_URL, country.replace(" ", "%20"));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 404) {
                throw new ToolExecutionException("Country '" + country + "' not found");
            }
            
            if (response.statusCode() != 200) {
                throw new ToolExecutionException(
                        "Country API returned status " + response.statusCode());
            }
            
            return parseCountryResponse(response.body());
            
        } catch (IOException | InterruptedException e) {
            logger.error("Failed to fetch country info for {}: {}", country, e.getMessage());
            throw new ToolExecutionException(
                    "Could not fetch country information for '" + country + "'", e);
        }
    }
    
    /**
     * Parses the REST Countries API response and extracts key information.
     */
    private String parseCountryResponse(String jsonResponse) throws ToolExecutionException {
        try {
            JsonArray countries = JsonParser.parseString(jsonResponse).getAsJsonArray();
            if (countries.isEmpty()) {
                throw new ToolExecutionException("No country data found");
            }
            
            // Take the first match
            JsonObject country = countries.get(0).getAsJsonObject();
            
            String name = country.getAsJsonObject("name")
                    .get("common")
                    .getAsString();
            
            // Extract capital (might be an array)
            String capital = "N/A";
            if (country.has("capital")) {
                JsonArray capitalArray = country.getAsJsonArray("capital");
                if (!capitalArray.isEmpty()) {
                    capital = capitalArray.get(0).getAsString();
                }
            }
            
            long population = country.get("population").getAsLong();
            String region = country.get("region").getAsString();
            
            // Extract languages
            String languages = "N/A";
            if (country.has("languages")) {
                JsonObject languagesObj = country.getAsJsonObject("languages");
                languages = StreamSupport.stream(languagesObj.entrySet().spliterator(), false)
                        .map(Map.Entry::getValue)
                        .map(JsonElement::getAsString)
                        .collect(Collectors.joining(", "));
            }
            
            // Extract currencies
            String currencies = "N/A";
            if (country.has("currencies")) {
                JsonObject currenciesObj = country.getAsJsonObject("currencies");
                currencies = StreamSupport.stream(currenciesObj.entrySet().spliterator(), false)
                        .map(e -> {
                            JsonObject curr = e.getValue().getAsJsonObject();
                            String currName = curr.get("name").getAsString();
                            String symbol = curr.has("symbol") ? 
                                    curr.get("symbol").getAsString() : "";
                            return symbol.isEmpty() ? currName : currName + " (" + symbol + ")";
                        })
                        .collect(Collectors.joining(", "));
            }
            
            return String.format(
                    "%s - Capital: %s, Population: %,d, Region: %s, Languages: %s, Currencies: %s",
                    name, capital, population, region, languages, currencies
            );
            
        } catch (Exception e) {
            logger.error("Failed to parse country response: {}", e.getMessage());
            throw new ToolExecutionException("Could not parse country data", e);
        }
    }
}
