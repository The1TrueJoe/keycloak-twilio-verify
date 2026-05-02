package com.jtelaak.keycloak;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

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
        String apiBaseUrl,
        boolean allowUserCreation,
        String userCreationRealmRole,
        int codeExpiresInSeconds) {

    public static final String ACCOUNT_SID = "twilioAccountSid";
    public static final String AUTH_TOKEN = "twilioAuthToken";
    public static final String VERIFY_SERVICE_SID = "twilioVerifyServiceSid";
    public static final String PHONE_ATTRIBUTE = "phoneNumberAttribute";
    public static final String CHANNEL = "twilioVerifyChannel";
    public static final String MAX_ATTEMPTS = "maxAttempts";
    public static final String HTTP_TIMEOUT_SECONDS = "httpTimeoutSeconds";
    public static final String API_BASE_URL = "twilioVerifyApiBaseUrl";
    public static final String ALLOW_USER_CREATION = "twilioVerifyAllowUserCreation";
    public static final String USER_CREATION_REALM_ROLE = "twilioVerifyUserCreationRealmRole";
    public static final String CODE_EXPIRES_SECONDS = "twilioVerifyCodeExpiresSeconds";

    public static final String DEFAULT_PHONE_ATTRIBUTE = "phone_number";
    public static final String DEFAULT_CHANNEL = "sms";
    public static final int DEFAULT_MAX_ATTEMPTS = 3;
    public static final int DEFAULT_HTTP_TIMEOUT_SECONDS = 10;
    public static final int DEFAULT_CODE_EXPIRES_SECONDS = 600;
    public static final String DEFAULT_API_BASE_URL = "https://verify.twilio.com/v2";

    private static final int MIN_TIMEOUT_SECONDS = 1;
    private static final int MAX_TIMEOUT_SECONDS = 60;

    public static Optional<TwilioVerifyConfig> from(AuthenticationFlowContext context) {
        AuthenticatorConfigModel model = context.getAuthenticatorConfig();
        Map<String, String> values = model == null ? Map.of() : model.getConfig();
        return from(values, context.getRealm());
    }

    public static Optional<TwilioVerifyConfig> from(KeycloakSession session, RealmModel realm) {
        return from(Map.of(), realm);
    }

    private static Optional<TwilioVerifyConfig> from(Map<String, String> values, RealmModel realm) {

        String accountSid = resolveSecret(values, realm, ACCOUNT_SID, "TWILIO_ACCOUNT_SID", "twilio.accountSid");
        String authToken = resolveSecret(values, realm, AUTH_TOKEN, "TWILIO_VERIFY_AUTH_TOKEN", "twilio.authToken");
        String serviceSid = resolveSecret(values, realm, VERIFY_SERVICE_SID, "TWILIO_VERIFY_SERVICE_SID",
                "twilio.verifyServiceSid");

        if (isBlank(accountSid) || isBlank(authToken) || isBlank(serviceSid)) {
            return Optional.empty();
        }

        String phoneAttribute = valueOrDefault(values, realm, PHONE_ATTRIBUTE,
                "TWILIO_VERIFY_PHONE_ATTRIBUTE", "twilio.verify.phoneAttribute", DEFAULT_PHONE_ATTRIBUTE);
        String channel = valueOrDefault(values, realm, CHANNEL, "TWILIO_VERIFY_CHANNEL",
                "twilio.verify.channel", DEFAULT_CHANNEL).toLowerCase();
        int maxAttempts = boundedInt(valueOrNull(values, realm, MAX_ATTEMPTS,
                "TWILIO_VERIFY_MAX_ATTEMPTS", "twilio.verify.maxAttempts"), DEFAULT_MAX_ATTEMPTS, 1, 10);
        int timeoutSeconds = boundedInt(valueOrNull(values, realm, HTTP_TIMEOUT_SECONDS,
                "TWILIO_VERIFY_HTTP_TIMEOUT_SECONDS", "twilio.verify.httpTimeoutSeconds"), DEFAULT_HTTP_TIMEOUT_SECONDS,
                MIN_TIMEOUT_SECONDS, MAX_TIMEOUT_SECONDS);
        String apiBaseUrl = trimTrailingSlash(valueOrDefault(values, realm, API_BASE_URL,
                "TWILIO_VERIFY_API_BASE_URL", "twilio.verify.apiBaseUrl", DEFAULT_API_BASE_URL));
        boolean allowUserCreation = booleanValue(valueOrNull(values, realm, ALLOW_USER_CREATION,
                "TWILIO_VERIFY_ALLOW_USER_CREATION", "twilio.verify.allowUserCreation"), false);
        String userCreationRealmRole = valueOrNull(values, realm, USER_CREATION_REALM_ROLE,
                "TWILIO_VERIFY_USER_CREATION_REALM_ROLE", "twilio.verify.userCreationRealmRole");
        int codeExpiresInSeconds = boundedInt(valueOrNull(values, realm, CODE_EXPIRES_SECONDS,
                "TWILIO_VERIFY_CODE_EXPIRES_SECONDS", "twilio.verify.codeExpiresSeconds"),
                DEFAULT_CODE_EXPIRES_SECONDS, 60, 3600);

        return Optional.of(new TwilioVerifyConfig(accountSid, authToken, serviceSid, phoneAttribute, channel,
                maxAttempts, Duration.ofSeconds(timeoutSeconds), apiBaseUrl, allowUserCreation,
                userCreationRealmRole, codeExpiresInSeconds));
    }

    private static String resolveSecret(Map<String, String> values, RealmModel realm, String configKey,
            String environmentKey, String propertyKey) {
        String configured = valueOrNull(values, realm, configKey, environmentKey, propertyKey);
        return isBlank(configured) ? null : configured.trim();
    }

    private static String valueOrDefault(Map<String, String> values, RealmModel realm, String key,
            String environmentKey, String propertyKey, String defaultValue) {
        String value = valueOrNull(values, realm, key, environmentKey, propertyKey);
        return isBlank(value) ? defaultValue : value.trim();
    }

    private static String valueOrNull(Map<String, String> values, RealmModel realm, String key, String environmentKey,
            String propertyKey) {
        String configured = values.get(key);
        if (!isBlank(configured)) {
            return configured;
        }

        String realmValue = realm == null ? null : realm.getAttribute(key);
        if (!isBlank(realmValue)) {
            return realmValue;
        }

        String environment = System.getenv(environmentKey);
        if (!isBlank(environment)) {
            return environment;
        }

        return System.getProperty(propertyKey);
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

    private static boolean booleanValue(String value, boolean defaultValue) {
        if (isBlank(value)) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value.trim()) || "1".equals(value.trim()) || "yes".equalsIgnoreCase(value.trim());
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
