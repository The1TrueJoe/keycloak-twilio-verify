package com.jtelaak.keycloak;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

public class TwilioVerifyResourceProviderFactory implements RealmResourceProviderFactory {

    private static final Logger LOG = Logger.getLogger(TwilioVerifyResourceProviderFactory.class);

    public static final String PROVIDER_ID = "twilio-verify";

    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        return new TwilioVerifyResourceProvider(session);
    }

    @Override
    public void init(Config.Scope config) {
        LOG.info("Twilio Verify native resource initialized");
    }

    @Override public void postInit(KeycloakSessionFactory factory) { }
    @Override public void close() { }
    @Override public String getId() { return PROVIDER_ID; }
}
