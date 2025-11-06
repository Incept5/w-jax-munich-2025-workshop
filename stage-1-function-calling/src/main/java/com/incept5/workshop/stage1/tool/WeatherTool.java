
package com.incept5.workshop.stage1.tool;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Function tool to get weather for a specific location.
 * 
 * This tool demonstrates function calling with parameters. It accepts
 * a city name and returns mock weather data. This is useful for testing
 * LLM function calling capabilities without requiring external API calls.
 * 
 * Note: This returns mock data, not real weather information.
 */
public class WeatherTool implements FunctionTool {
    private static final Gson gson = new Gson();

    @Override
    public String getName() {
        return "get_weather";
    }

    @Override
    public String getDescription() {
        return "Find the weather for a specific location, usually a town or city";
    }

    @Override
    public JsonObject getParametersSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");

        // Define the 'city' parameter
        JsonObject properties = new JsonObject();
        JsonObject cityProperty = new JsonObject();
        cityProperty.addProperty("type", "string");
        cityProperty.addProperty("description", "The city or town name for the weather forecast");
        properties.add("city", cityProperty);

        schema.add("properties", properties);

        // Mark 'city' as required
        JsonArray required = new JsonArray();
        required.add("city");
        schema.add("required", required);

        return schema;
    }

    @Override
    public String execute(JsonObject arguments) {
        // Extract the city parameter
        String city = arguments.has("city") ? arguments.get("city").getAsString() : "";

        // Return mock weather data based on the city
        // Special case for Paris to match test expectations
        if (city.equalsIgnoreCase("paris")) {
            return "Pas Mal";
        } else {
            return "Merd!";
        }
    }

    @Override
    public JsonObject toJson() {
        JsonObject tool = new JsonObject();
        tool.addProperty("type", "function");

        JsonObject function = new JsonObject();
        function.addProperty("name", getName());
        function.addProperty("description", getDescription());
        function.add("parameters", getParametersSchema());

        tool.add("function", function);
        return tool;
    }
}
