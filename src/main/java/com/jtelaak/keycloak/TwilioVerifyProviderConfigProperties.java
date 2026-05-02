package com.jtelaak.keycloak;

import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

final class TwilioVerifyProviderConfigProperties {

    private TwilioVerifyProviderConfigProperties() {
    }

    static List<ProviderConfigProperty> all() {
        return List.of(
                passwordProperty(TwilioVerifyConfig.ACCOUNT_SID,
                        "Twilio Account SID",
                        "Twilio Account SID. If blank, the realm attribute, TWILIO_ACCOUNT_SID, or twilio.accountSid is used."),
                passwordProperty(TwilioVerifyConfig.AUTH_TOKEN,
                        "Twilio Auth Token",
                        "Twilio Auth Token. If blank, the realm attribute, TWILIO_VERIFY_AUTH_TOKEN, or twilio.authToken is used."),
                passwordProperty(TwilioVerifyConfig.VERIFY_SERVICE_SID,
                        "Verify Service SID",
                        "Twilio Verify Service SID. If blank, the realm attribute, TWILIO_VERIFY_SERVICE_SID, or twilio.verifyServiceSid is used."),
                stringProperty(TwilioVerifyConfig.PHONE_ATTRIBUTE,
                        "Phone Number User Attribute",
                        "User attribute containing an E.164 phone number.",
                        TwilioVerifyConfig.DEFAULT_PHONE_ATTRIBUTE),
                listProperty(TwilioVerifyConfig.CHANNEL,
                        "Verification Channel",
                        "Twilio Verify channel used to deliver codes.",
                        TwilioVerifyConfig.DEFAULT_CHANNEL,
                        List.of("sms", "whatsapp", "call")),
                stringProperty(TwilioVerifyConfig.MAX_ATTEMPTS,
                        "Max Check Attempts",
                        "Maximum code check attempts during a browser authentication session.",
                        String.valueOf(TwilioVerifyConfig.DEFAULT_MAX_ATTEMPTS)),
                stringProperty(TwilioVerifyConfig.HTTP_TIMEOUT_SECONDS,
                        "HTTP Timeout Seconds",
                        "Timeout for Twilio Verify API calls.",
                        String.valueOf(TwilioVerifyConfig.DEFAULT_HTTP_TIMEOUT_SECONDS)),
                stringProperty(TwilioVerifyConfig.API_BASE_URL,
                        "Twilio Verify API Base URL",
                        "Override only for testing or private routing.",
                        TwilioVerifyConfig.DEFAULT_API_BASE_URL),
                stringProperty(TwilioVerifyConfig.ALLOW_USER_CREATION,
                        "Allow User Creation",
                        "Allow native direct grant to create a user after Twilio Verify approves the phone code.",
                        "false"),
                stringProperty(TwilioVerifyConfig.USER_CREATION_REALM_ROLE,
                        "User Creation Realm Role",
                        "Optional realm role granted after phone verification.",
                        ""),
                stringProperty(TwilioVerifyConfig.CODE_EXPIRES_SECONDS,
                        "Displayed Code Expiry Seconds",
                        "Expiry returned by the native send endpoint. Configure the actual TTL on the Twilio Verify service.",
                        String.valueOf(TwilioVerifyConfig.DEFAULT_CODE_EXPIRES_SECONDS))
        );
    }

    private static ProviderConfigProperty stringProperty(String name, String label, String helpText, String defaultValue) {
        return new ProviderConfigProperty(name, label, helpText, ProviderConfigProperty.STRING_TYPE, defaultValue);
    }

    private static ProviderConfigProperty passwordProperty(String name, String label, String helpText) {
        return new ProviderConfigProperty(name, label, helpText, ProviderConfigProperty.PASSWORD, null);
    }

    private static ProviderConfigProperty listProperty(String name, String label, String helpText, String defaultValue,
            List<String> options) {
        ProviderConfigProperty property = new ProviderConfigProperty(name, label, helpText,
                ProviderConfigProperty.LIST_TYPE, defaultValue);
        property.setOptions(options);
        return property;
    }
}
