package com.jtelaak.keycloak;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.AuthenticatorConfigModel;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

public record TwilioVerifyConfig(
        String accountSid,
        String authToken,
        String verifyServiceSid,
        String phoneAttribute,
        String channel,
        int maxAttempts,
        Duration httpTimeout,
        String apiBaseUrl) {

    public static final String ACCOUNT_SID = "twilioAccountSid";
    public static final String AUTH_TOKEN = "twilioAuthToken";
    public static final String VERIFY_SERVICE_SID = "twilioVerifyServiceSid";
    public static final String PHONE_ATTRIBUTE = "phoneNumberAttribute";
    public static final String CHANNEL = "twilioVerifyChannel";
    public static final String MAX_ATTEMPTS = "maxAttempts";
    public static final String HTTP_TIMEOUT_SECONDS = "httpTimeoutSeconds";
    public static final String API_BASE_URL = "twilioVerifyApiBaseUrl";

    public static final String DEFAULT_PHONE_ATTRIBUTE = "phone_number";
    public static final String DEFAULT_CHANNEL = "sms";
    public static final int DEFAULT_MAX_ATTEMPTS = 3;
    public static final int DEFAULT_HTTP_TIMEOUT_SECONDS = 10;
    public static final String DEFAULT_API_BASE_URL = "https://verify.twilio.com/v2";

    private static final int MIN_TIMEOUT_SECONDS = 1;
    private static final int MAX_TIMEOUT_SECONDS = 60;

    public static Optional<TwilioVerifyConfig> from(AuthenticationFlowContext context) {
        AuthenticatorConfigModel model = context.getAuthenticatorConfig();
        Map<String, String> values = model == null ? Map.of() : model.getConfig();

        String accountSid = resolveSecret(values, ACCOUNT_SID, "TWILIO_ACCOUNT_SID", "twilio.accountSid");
        String authToken = resolveSecret(values, AUTH_TOKEN, "TWILIO_AUTH_TOKEN", "twilio.authToken");
        String serviceSid = resolveSecret(values, VERIFY_SERVICE_SID, "TWILIO_VERIFY_SERVICE_SID",
                "twilio.verifyServiceSid");

        if (isBlank(accountSid) || isBlank(authToken) || isBlank(serviceSid)) {
            return Optional.empty();
        }

        String phoneAttribute = valueOrDefault(values, PHONE_ATTRIBUTE, DEFAULT_PHONE_ATTRIBUTE);
        String channel = valueOrDefault(values, CHANNEL, DEFAULT_CHANNEL).toLowerCase();
        int maxAttempts = boundedInt(values.get(MAX_ATTEMPTS), DEFAULT_MAX_ATTEMPTS, 1, 10);
        int timeoutSeconds = boundedInt(values.get(HTTP_TIMEOUT_SECONDS), DEFAULT_HTTP_TIMEOUT_SECONDS,
                MIN_TIMEOUT_SECONDS, MAX_TIMEOUT_SECONDS);
        String apiBaseUrl = trimTrailingSlash(valueOrDefault(values, API_BASE_URL, DEFAULT_API_BASE_URL));

        return Optional.of(new TwilioVerifyConfig(accountSid, authToken, serviceSid, phoneAttribute, channel,
                maxAttempts, Duration.ofSeconds(timeoutSeconds), apiBaseUrl));
    }

    private static String resolveSecret(Map<String, String> values, String configKey, String environmentKey,
            String propertyKey) {
        String configured = values.get(configKey);
        if (!isBlank(configured)) {
            return configured.trim();
        }

        String environment = System.getenv(environmentKey);
        if (!isBlank(environment)) {
            return environment.trim();
        }

        String property = System.getProperty(propertyKey);
        return isBlank(property) ? null : property.trim();
    }

    private static String valueOrDefault(Map<String, String> values, String key, String defaultValue) {
        String value = values.get(key);
        return isBlank(value) ? defaultValue : value.trim();
    }

    private static int boundedInt(String value, int defaultValue, int minimum, int maximum) {
        if (isBlank(value)) {
            return defaultValue;
        }

        try {
            int parsed = Integer.parseInt(value.trim());
            return Math.min(Math.max(parsed, minimum), maximum);
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private static String trimTrailingSlash(String value) {
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
