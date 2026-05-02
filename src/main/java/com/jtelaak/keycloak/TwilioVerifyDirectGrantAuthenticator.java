package com.jtelaak.keycloak;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.OAuthErrorException;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.authenticators.directgrant.AbstractDirectGrantAuthenticator;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;
import java.util.Optional;

public class TwilioVerifyDirectGrantAuthenticator extends AbstractDirectGrantAuthenticator {

    public static final String PROVIDER_ID = "twilio-verify-direct-grant";

    private static final Logger LOG = Logger.getLogger(TwilioVerifyDirectGrantAuthenticator.class);
    private static final Requirement[] REQUIREMENT_CHOICES = new Requirement[] {
            Requirement.REQUIRED,
            Requirement.ALTERNATIVE,
            Requirement.DISABLED,
    };

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> form = context.getHttpRequest().getDecodedFormParameters();
        Optional<String> phoneNumber = TwilioVerifyUserSupport.normalizePhone(
                form.getFirst("phone_number"),
                form.getFirst("phoneNumber"));

        if (phoneNumber.isEmpty()) {
            context.attempted();
            return;
        }

        String code = sanitizeCode(form.getFirst("code"));
        context.getEvent()
                .detail(Details.AUTH_METHOD, "twilio_verify")
                .detail("phone_number", phoneNumber.get());

        if (code == null || code.isBlank()) {
            fail(context, AuthenticationFlowError.INVALID_CREDENTIALS, Response.Status.BAD_REQUEST,
                    OAuthErrorException.INVALID_GRANT, "Missing phone verification code");
            return;
        }

        Optional<TwilioVerifyConfig> config = TwilioVerifyConfig.from(context);
        if (config.isEmpty()) {
            LOG.warn("Twilio Verify direct grant is missing required configuration");
            fail(context, AuthenticationFlowError.INTERNAL_ERROR, Response.Status.SERVICE_UNAVAILABLE,
                    "temporarily_unavailable", "Twilio Verify is not configured");
            return;
        }

        try {
            TwilioVerifyResponse response = new TwilioVerifyClient(config.get())
                    .checkVerification(phoneNumber.get(), code);
            if (!response.approved()) {
                fail(context, AuthenticationFlowError.INVALID_CREDENTIALS, Response.Status.UNAUTHORIZED,
                        OAuthErrorException.INVALID_GRANT, "Invalid or expired phone verification code");
                return;
            }

            UserModel user = TwilioVerifyUserSupport.findUserByPhone(
                    context.getSession(), context.getRealm(), config.get(), phoneNumber.get())
                    .orElse(null);
            if (user == null) {
                if (!config.get().allowUserCreation()) {
                    fail(context, AuthenticationFlowError.INVALID_USER, Response.Status.UNAUTHORIZED,
                            OAuthErrorException.INVALID_GRANT, "Invalid or expired phone verification code");
                    return;
                }
                user = TwilioVerifyUserSupport.findOrCreateUserByPhone(
                        context.getSession(), context.getRealm(), config.get(), phoneNumber.get());
            }

            if (!user.isEnabled()) {
                fail(context, AuthenticationFlowError.INVALID_USER, Response.Status.FORBIDDEN,
                        OAuthErrorException.INVALID_GRANT, "User is disabled");
                return;
            }

            TwilioVerifyUserSupport.markPhoneVerified(user, config.get(), phoneNumber.get());
            TwilioVerifyUserSupport.grantConfiguredRole(context.getRealm(), user, config.get());
            context.getEvent().user(user);
            context.setUser(user);
            context.success();
        } catch (TwilioVerifyException exception) {
            LOG.warnf(exception, "Twilio Verify check failed: %s", exception.getAdminMessage());
            fail(context, AuthenticationFlowError.INVALID_CREDENTIALS, Response.Status.UNAUTHORIZED,
                    OAuthErrorException.INVALID_GRANT, "Invalid or expired phone verification code");
        }
    }

    private void fail(AuthenticationFlowContext context, AuthenticationFlowError flowError, Response.Status status,
            String oauthError, String description) {
        context.getEvent().error(Errors.INVALID_USER_CREDENTIALS);
        context.failure(flowError, errorResponse(status.getStatusCode(), oauthError, description));
    }

    private String sanitizeCode(String code) {
        return code == null ? null : code.replaceAll("\\s+", "").trim();
    }

    @Override public boolean requiresUser() { return false; }
    @Override public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) { return true; }
    @Override public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) { }
    @Override public boolean isUserSetupAllowed() { return false; }
    @Override public String getDisplayType() { return "Twilio Verify Direct Grant"; }
    @Override public String getReferenceCategory() { return "twilio-verify"; }
    @Override public boolean isConfigurable() { return true; }
    @Override public Requirement[] getRequirementChoices() { return REQUIREMENT_CHOICES; }
    @Override public String getHelpText() { return "Validates native phone OTP requests using Twilio Verify and phone_number + code form parameters."; }
    @Override public List<ProviderConfigProperty> getConfigProperties() { return TwilioVerifyProviderConfigProperties.all(); }
    @Override public String getId() { return PROVIDER_ID; }
}
