package com.jtelaak.keycloak;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

public final class TwilioVerifyResourceProvider implements RealmResourceProvider {

    private final KeycloakSession session;

    public TwilioVerifyResourceProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        return new TwilioVerifyResource(session);
    }

    @Override
    public void close() {
    }
}
