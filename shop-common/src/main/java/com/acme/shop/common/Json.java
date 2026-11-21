package com.acme.shop.common;

import tools.jackson.databind.json.JsonMapper;

public final class Json {
    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private Json() {}
    public static String write(Object o) { return MAPPER.writeValueAsString(o); }
    public static <T> T read(String json, Class<T> type) { return MAPPER.readValue(json, type); }
}
