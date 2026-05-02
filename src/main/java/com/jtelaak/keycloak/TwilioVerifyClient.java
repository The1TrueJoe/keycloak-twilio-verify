package com.jtelaak.keycloak;

import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

public class TwilioVerifyClient {

    private final TwilioVerifyConfig config;
    private final HttpClient httpClient;

    public TwilioVerifyClient(TwilioVerifyConfig config) {
        this(config, HttpClient.newBuilder()
                .connectTimeout(config.httpTimeout())
                .build());
    }

    TwilioVerifyClient(TwilioVerifyConfig config, HttpClient httpClient) {
        this.config = config;
        this.httpClient = httpClient;
    }

    public TwilioVerifyResponse sendVerification(String phoneNumber) throws TwilioVerifyException {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("To", phoneNumber);
        form.put("Channel", config.channel());
        return post("/Services/" + encodePath(config.verifyServiceSid()) + "/Verifications", form);
    }

    public TwilioVerifyResponse checkVerification(String phoneNumber, String code) throws TwilioVerifyException {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("To", phoneNumber);
        form.put("Code", code);
        return post("/Services/" + encodePath(config.verifyServiceSid()) + "/VerificationCheck", form);
    }

    private TwilioVerifyResponse post(String path, Map<String, String> form) throws TwilioVerifyException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(config.apiBaseUrl() + path))
                .timeout(config.httpTimeout())
                .header("Accept", "application/json")
                .header("Authorization", basicAuthHeader())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formEncode(form), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new TwilioVerifyException("Twilio Verify network request failed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new TwilioVerifyException("Twilio Verify network request was interrupted", exception);
        }

        Map<String, Object> payload = parsePayload(response.body());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new TwilioVerifyException("Twilio Verify returned HTTP " + response.statusCode()
                    + ": " + payloadValue(payload, "message"));
        }

        return TwilioVerifyResponse.from(payload);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parsePayload(String body) throws TwilioVerifyException {
        if (body == null || body.isBlank()) {
            return Map.of();
        }

        try {
            return JsonSerialization.readValue(body, Map.class);
        } catch (IOException exception) {
            throw new TwilioVerifyException("Twilio Verify returned an unreadable response", exception);
        }
    }

    private String basicAuthHeader() {
        String credentials = config.accountSid() + ":" + config.authToken();
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private String formEncode(Map<String, String> form) {
        StringJoiner body = new StringJoiner("&");
        form.forEach((name, value) -> body.add(urlEncode(name) + "=" + urlEncode(value)));
        return body.toString();
    }

    private String encodePath(String value) {
        return urlEncode(value).replace("+", "%20");
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String payloadValue(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value == null ? "no message" : String.valueOf(value);
    }
}
