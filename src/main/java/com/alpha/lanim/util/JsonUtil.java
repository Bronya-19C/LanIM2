package com.alpha.lanim.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.nio.charset.StandardCharsets;

public final class JsonUtil {

    private static final Gson GSON = new GsonBuilder().create();

    private JsonUtil() {
    }

    public static Gson gson() {
        return GSON;
    }

    public static String toJson(Object o) {
        return GSON.toJson(o);
    }

    public static byte[] toJsonBytes(Object o) {
        return toJson(o).getBytes(StandardCharsets.UTF_8);
    }

    public static <T> T fromJson(byte[] bytes, Class<T> clazz) {
        return GSON.fromJson(new String(bytes, StandardCharsets.UTF_8), clazz);
    }

    public static <T> T fromPayload(Object payload, Class<T> clazz) {
        String json = GSON.toJson(payload);
        return GSON.fromJson(json, clazz);
    }
}
