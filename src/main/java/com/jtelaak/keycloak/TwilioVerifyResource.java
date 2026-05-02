package com.jtelaak.keycloak;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.ServiceUnavailableException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.Map;
import java.util.Optional;

final class TwilioVerifyResource {

    private static final Logger LOG = Logger.getLogger(TwilioVerifyResource.class);

    private final KeycloakSession session;

    TwilioVerifyResource(KeycloakSession session) {
        this.session = session;
    }

    /**
     * GET /realms/{realm}/twilio-verify/authentication-code?phoneNumber=+15551234567
     */
    @GET
    @Path("authentication-code")
    @Produces(MediaType.APPLICATION_JSON)
    public Response requestCode(@QueryParam("phoneNumber") String rawPhoneNumber,
            @QueryParam("phone_number") String rawPhoneNumberAlt) {
        RealmModel realm = session.getContext().getRealm();
        Optional<TwilioVerifyConfig> config = TwilioVerifyConfig.from(session, realm);
        if (config.isEmpty()) {
            LOG.warn("Twilio Verify native endpoint is missing required configuration");
            throw new ServiceUnavailableException("Twilio Verify is not configured");
        }

        String phoneNumber = TwilioVerifyUserSupport.normalizePhone(rawPhoneNumber, rawPhoneNumberAlt)
                .orElseThrow(() -> new BadRequestException("Phone number is invalid"));

        Optional<UserModel> user = TwilioVerifyUserSupport.findUserByPhone(session, realm, config.get(), phoneNumber);
        if (user.isPresent() && !user.get().isEnabled()) {
            throw new ForbiddenException("User is disabled");
        }
        if (user.isEmpty() && !config.get().allowUserCreation()) {
            throw new ForbiddenException("Phone number not found");
        }

        try {
            new TwilioVerifyClient(config.get()).sendVerification(phoneNumber);
            return Response.ok(Map.of("expires_in", config.get().codeExpiresInSeconds()), MediaType.APPLICATION_JSON).build();
        } catch (TwilioVerifyException exception) {
            LOG.warnf(exception, "Twilio Verify send failed: %s", exception.getAdminMessage());
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "twilio_verify_send_failed"))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
}
