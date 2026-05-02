package com.jtelaak.keycloak;

import java.util.Map;

public record TwilioVerifyResponse(String sid, String status, boolean valid) {

    public boolean approved() {
        return valid || "approved".equalsIgnoreCase(status);
    }

    public static TwilioVerifyResponse from(Map<String, Object> payload) {
        return new TwilioVerifyResponse(
                stringValue(payload, "sid"),
                stringValue(payload, "status"),
                booleanValue(payload, "valid"));
    }

    private static String stringValue(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static boolean booleanValue(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value instanceof Boolean booleanValue && booleanValue;
    }
}
