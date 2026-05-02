package com.jtelaak.keycloak;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

public class TwilioVerifyAuthenticatorFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "twilio-verify-authenticator";

    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.ALTERNATIVE,
            AuthenticationExecutionModel.Requirement.DISABLED
    };

    private static final TwilioVerifyAuthenticator SINGLETON = new TwilioVerifyAuthenticator();

    @Override
    public String getDisplayType() {
        return "Twilio Verify";
    }

    @Override
    public String getReferenceCategory() {
        return "twilio-verify";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return "Sends a one-time verification with Twilio Verify and validates the submitted code.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of(
                passwordProperty(TwilioVerifyConfig.ACCOUNT_SID,
                        "Twilio Account SID",
                        "Twilio Account SID. If blank, TWILIO_ACCOUNT_SID or twilio.accountSid is used."),
                passwordProperty(TwilioVerifyConfig.AUTH_TOKEN,
                        "Twilio Auth Token",
                        "Twilio Auth Token. If blank, TWILIO_VERIFY_AUTH_TOKEN or twilio.authToken is used."),
                passwordProperty(TwilioVerifyConfig.VERIFY_SERVICE_SID,
                        "Verify Service SID",
                        "Twilio Verify Service SID. If blank, TWILIO_VERIFY_SERVICE_SID or twilio.verifyServiceSid is used."),
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
                        "Maximum code check attempts during a single authentication session.",
                        String.valueOf(TwilioVerifyConfig.DEFAULT_MAX_ATTEMPTS)),
                stringProperty(TwilioVerifyConfig.HTTP_TIMEOUT_SECONDS,
                        "HTTP Timeout Seconds",
                        "Timeout for Twilio Verify API calls.",
                        String.valueOf(TwilioVerifyConfig.DEFAULT_HTTP_TIMEOUT_SECONDS)),
                stringProperty(TwilioVerifyConfig.API_BASE_URL,
                        "Twilio Verify API Base URL",
                        "Override only for testing or private routing.",
                        TwilioVerifyConfig.DEFAULT_API_BASE_URL)
        );
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
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
