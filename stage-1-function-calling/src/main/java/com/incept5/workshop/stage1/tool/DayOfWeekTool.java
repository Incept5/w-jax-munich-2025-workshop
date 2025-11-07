
package com.incept5.workshop.stage1.tool;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Function tool to get the current day of the week.
 * 
 * This is a simple tool that demonstrates function calling without
 * requiring any parameters. It returns the current day of the week
 * as a string (e.g., "Monday", "Tuesday", etc.).
 */
public class DayOfWeekTool implements FunctionTool {
    private static final Gson gson = new Gson();

    @Override
    public String getName() {
        return "get_current_day";
    }

    @Override
    public String getDescription() {
        return "Get the current day of the week";
    }

    @Override
    public JsonObject getParametersSchema() {
        // This function takes no parameters, so we return an empty schema
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.add("properties", new JsonObject());
        schema.add("required", gson.toJsonTree(new String[0]));
        return schema;
    }

    @Override
    public String execute(JsonObject arguments) {
        // Get the current date and return the day of the week
        LocalDate now = LocalDate.now();
        return now.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
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
