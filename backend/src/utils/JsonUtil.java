package utils;

import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * JsonUtil — JSON serialization/deserialization using Gson.
 * Extension beyond Sem III: used because building a JSON parser from scratch is impractical.
 */
public class JsonUtil {

    private static final Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .serializeNulls()
            .create();

    /** Convert an object to a JSON string */
    public static String toJson(Object obj) {
        return gson.toJson(obj);
    }

    /** Parse a JSON string to a specific class */
    public static <T> T fromJson(String json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }

    /** Parse a JSON string to a JsonObject for flexible field access */
    public static JsonObject parseObject(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    /** Read the request body as a string */
    public static String readBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int bytesRead;
        while ((bytesRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, bytesRead);
        }
        return buffer.toString(StandardCharsets.UTF_8.name());
    }

    /** Read the request body and parse as JsonObject */
    public static JsonObject readJsonBody(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        if (body == null || body.trim().isEmpty()) {
            return new JsonObject();
        }
        return parseObject(body);
    }

    /** Send a JSON response */
    public static void sendJson(HttpExchange exchange, int statusCode, Object data) throws IOException {
        String json = toJson(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    /** Send a JSON error response */
    public static void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        sendJson(exchange, statusCode, error);
    }

    /** Get query parameters from the exchange */
    public static Map<String, String> getQueryParams(HttpExchange exchange) {
        return parseQueryParams(exchange.getRequestURI());
    }

    /** Parse query parameters from a URI */
    public static Map<String, String> parseQueryParams(java.net.URI uri) {
        Map<String, String> params = new HashMap<>();
        if (uri == null) return params;
        String query = uri.getQuery();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=", 2);
                if (pair.length == 2) {
                    try {
                        params.put(
                            java.net.URLDecoder.decode(pair[0], "UTF-8"),
                            java.net.URLDecoder.decode(pair[1], "UTF-8")
                        );
                    } catch (UnsupportedEncodingException e) {
                        params.put(pair[0], pair[1]);
                    }
                }
            }
        }
        return params;
    }

    /** Get a path segment (e.g., /api/leave/123 → "123" at index 3) */
    public static String getPathSegment(HttpExchange exchange, int index) {
        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");
        if (index < parts.length) {
            return parts[index];
        }
        return null;
    }

    /** Get an integer from a JsonObject, with a default value */
    public static int getInt(JsonObject obj, String key, int defaultVal) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsInt();
        }
        return defaultVal;
    }

    /** Get a string from a JsonObject, with a default value */
    public static String getString(JsonObject obj, String key, String defaultVal) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return defaultVal;
    }

    /** Get a boolean from a JsonObject */
    public static boolean getBoolean(JsonObject obj, String key, boolean defaultVal) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsBoolean();
        }
        return defaultVal;
    }

    /** Get a double from a JsonObject */
    public static double getDouble(JsonObject obj, String key, double defaultVal) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsDouble();
        }
        return defaultVal;
    }
}
