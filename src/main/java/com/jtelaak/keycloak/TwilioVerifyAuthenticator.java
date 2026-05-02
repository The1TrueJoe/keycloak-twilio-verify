package com.jtelaak.keycloak;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.validation.Validation;

import java.util.Optional;

public class TwilioVerifyAuthenticator implements Authenticator {

    private static final Logger LOG = Logger.getLogger(TwilioVerifyAuthenticator.class);

    private static final String TEMPLATE = "login-twilio-verify.ftl";
    private static final String NOTE_SENT_TO = "TWILIO_VERIFY_SENT_TO";
    private static final String NOTE_ATTEMPTS = "TWILIO_VERIFY_ATTEMPTS";
    private static final String NOTE_LAST_STATUS = "TWILIO_VERIFY_LAST_STATUS";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        Optional<TwilioVerifyConfig> config = TwilioVerifyConfig.from(context);
        if (config.isEmpty()) {
            failConfiguration(context);
            return;
        }

        Optional<String> phoneNumber = findPhoneNumber(context.getUser(), config.get());
        if (phoneNumber.isEmpty()) {
            handleMissingPhone(context, config.get());
            return;
        }

        String sentTo = context.getAuthenticationSession().getAuthNote(NOTE_SENT_TO);
        if (!phoneNumber.get().equals(sentTo)) {
            if (!sendVerification(context, config.get(), phoneNumber.get(), false)) {
                return;
            }
        }

        context.challenge(challenge(context, config.get(), phoneNumber.get(), null));
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        Optional<TwilioVerifyConfig> config = TwilioVerifyConfig.from(context);
        if (config.isEmpty()) {
            failConfiguration(context);
            return;
        }

        Optional<String> phoneNumber = findPhoneNumber(context.getUser(), config.get());
        if (phoneNumber.isEmpty()) {
            handleMissingPhone(context, config.get());
            return;
        }

        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        if (formData.containsKey("resend")) {
            if (sendVerification(context, config.get(), phoneNumber.get(), true)) {
                context.challenge(challenge(context, config.get(), phoneNumber.get(), "twilioVerifyCodeResent"));
            }
            return;
        }

        String code = sanitizeCode(formData.getFirst("code"));
        if (Validation.isBlank(code)) {
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS,
                    challenge(context, config.get(), phoneNumber.get(), Messages.MISSING_TOTP));
            return;
        }

        int attempts = incrementAttempts(context);
        if (attempts > config.get().maxAttempts()) {
            context.getEvent().error(Errors.INVALID_USER_CREDENTIALS);
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS,
                    challenge(context, config.get(), phoneNumber.get(), "twilioVerifyTooManyAttempts"));
            return;
        }

        try {
            TwilioVerifyResponse response = new TwilioVerifyClient(config.get()).checkVerification(phoneNumber.get(), code);
            context.getAuthenticationSession().setAuthNote(NOTE_LAST_STATUS, response.status());

            if (response.approved()) {
                clearNotes(context);
                context.success();
                return;
            }

            context.getEvent().error(Errors.INVALID_USER_CREDENTIALS);
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS,
                    challenge(context, config.get(), phoneNumber.get(), Messages.INVALID_TOTP));
        } catch (TwilioVerifyException exception) {
            LOG.warnf(exception, "Twilio Verify check failed: %s", exception.getAdminMessage());
            context.getEvent().error(Errors.INVALID_USER_CREDENTIALS);
            context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR,
                    challenge(context, config.get(), phoneNumber.get(), "twilioVerifyUnavailable"));
        }
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    }

    @Override
    public void close() {
    }

    private boolean sendVerification(AuthenticationFlowContext context, TwilioVerifyConfig config, String phoneNumber,
            boolean resent) {
        try {
            TwilioVerifyResponse response = new TwilioVerifyClient(config).sendVerification(phoneNumber);
            context.getAuthenticationSession().setAuthNote(NOTE_SENT_TO, phoneNumber);
            context.getAuthenticationSession().setAuthNote(NOTE_ATTEMPTS, "0");
            context.getAuthenticationSession().setAuthNote(NOTE_LAST_STATUS, response.status());
            return true;
        } catch (TwilioVerifyException exception) {
            LOG.warnf(exception, "Twilio Verify send failed: %s", exception.getAdminMessage());
            String message = resent ? "twilioVerifyResendFailed" : "twilioVerifyUnavailable";
            context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR, challenge(context, config, phoneNumber, message));
            return false;
        }
    }

    private Response challenge(AuthenticationFlowContext context, TwilioVerifyConfig config, String phoneNumber,
            String errorMessage) {
        LoginFormsProvider form = context.form();
        if (errorMessage != null) {
            form.setError(errorMessage);
        }

        return form
                .setAttribute("maskedPhoneNumber", PhoneNumberMasker.mask(phoneNumber))
                .setAttribute("phoneAttribute", config.phoneAttribute())
                .setAttribute("channel", config.channel())
                .setAttribute("remainingAttempts", remainingAttempts(context, config))
                .createForm(TEMPLATE);
    }

    private void failConfiguration(AuthenticationFlowContext context) {
        LOG.warn("Twilio Verify authenticator is missing required configuration");
        context.getEvent().error(Errors.IDENTITY_PROVIDER_ERROR);
        context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR,
                context.form().setError("twilioVerifyMisconfigured").createForm(TEMPLATE));
    }

    private void handleMissingPhone(AuthenticationFlowContext context, TwilioVerifyConfig config) {
        if (isAlternative(context)) {
            context.attempted();
            return;
        }

        context.getEvent().error(Errors.INVALID_USER_CREDENTIALS);
        context.failureChallenge(AuthenticationFlowError.INVALID_USER,
                context.form()
                        .setAttribute("phoneAttribute", config.phoneAttribute())
                        .setError("twilioVerifyMissingPhone")
                        .createForm(TEMPLATE));
    }

    private Optional<String> findPhoneNumber(UserModel user, TwilioVerifyConfig config) {
        if (user == null) {
            return Optional.empty();
        }

        return user.getAttributeStream(config.phoneAttribute())
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .findFirst();
    }

    private boolean isAlternative(AuthenticationFlowContext context) {
        AuthenticationExecutionModel execution = context.getExecution();
        return execution != null && execution.isAlternative();
    }

    private int incrementAttempts(AuthenticationFlowContext context) {
        int attempts = parseAttempts(context) + 1;
        context.getAuthenticationSession().setAuthNote(NOTE_ATTEMPTS, String.valueOf(attempts));
        return attempts;
    }

    private int remainingAttempts(AuthenticationFlowContext context, TwilioVerifyConfig config) {
        return Math.max(config.maxAttempts() - parseAttempts(context), 0);
    }

    private int parseAttempts(AuthenticationFlowContext context) {
        String value = context.getAuthenticationSession().getAuthNote(NOTE_ATTEMPTS);
        if (value == null || value.isBlank()) {
            return 0;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private void clearNotes(AuthenticationFlowContext context) {
        context.getAuthenticationSession().removeAuthNote(NOTE_SENT_TO);
        context.getAuthenticationSession().removeAuthNote(NOTE_ATTEMPTS);
        context.getAuthenticationSession().removeAuthNote(NOTE_LAST_STATUS);
    }

    private String sanitizeCode(String code) {
        return code == null ? null : code.replaceAll("\\s+", "").trim();
    }
}
