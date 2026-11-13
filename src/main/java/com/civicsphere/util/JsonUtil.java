package com.civicsphere.util;

import com.google.gson.*;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility for JSON serialization/deserialization and HTTP response formatting.
 */
public class JsonUtil {

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private static final Gson gson = new GsonBuilder()
            .setDateFormat(DATE_FORMAT)
            .registerTypeAdapter(Timestamp.class, new JsonSerializer<Timestamp>() {
                @Override
                public JsonElement serialize(Timestamp src, Type typeOfSrc, JsonSerializationContext context) {
                    return src == null ? JsonNull.INSTANCE : new JsonPrimitive(new SimpleDateFormat(DATE_FORMAT).format(src));
                }
            })
            .registerTypeAdapter(Timestamp.class, new JsonDeserializer<Timestamp>() {
                @Override
                public Timestamp deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                        throws JsonParseException {
                    try {
                        return json == null ? null : new Timestamp(new SimpleDateFormat(DATE_FORMAT).parse(json.getAsString()).getTime());
                    } catch (Exception e) {
                        return null;
                    }
                }
            })
            .create();

    public static Gson getGson() {
        return gson;
    }

    public static String toJson(Object obj) {
        return gson.toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> classOfT) {
        return gson.fromJson(json, classOfT);
    }

    public static <T> T fromJson(Reader reader, Class<T> classOfT) {
        return gson.fromJson(reader, classOfT);
    }

    public static void sendJsonResponse(HttpServletResponse response, int statusCode, Object data) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(statusCode);
        try (PrintWriter out = response.getWriter()) {
            out.print(gson.toJson(data));
            out.flush();
        }
    }

    public static void sendSuccessResponse(HttpServletResponse response, int statusCode, String message, Object data) throws IOException {
        Map<String, Object> map = new HashMap<>();
        map.put("success", true);
        map.put("message", message);
        if (data != null) {
            map.put("data", data);
        }
        sendJsonResponse(response, statusCode, map);
    }

    public static void sendErrorResponse(HttpServletResponse response, int statusCode, String errorMessage) throws IOException {
        Map<String, Object> map = new HashMap<>();
        map.put("success", false);
        map.put("error", errorMessage);
        sendJsonResponse(response, statusCode, map);
    }
}

