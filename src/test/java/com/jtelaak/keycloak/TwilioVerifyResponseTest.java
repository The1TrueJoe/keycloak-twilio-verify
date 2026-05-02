package com.jtelaak.keycloak;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TwilioVerifyResponseTest {

    @Test
    void approvedStatusPassesVerification() {
        TwilioVerifyResponse response = TwilioVerifyResponse.from(Map.of("status", "approved"));
        assertTrue(response.approved());
    }

    @Test
    void validFlagPassesVerification() {
        TwilioVerifyResponse response = TwilioVerifyResponse.from(Map.of("valid", true, "status", "pending"));
        assertTrue(response.approved());
    }

    @Test
    void pendingStatusDoesNotPassVerification() {
        TwilioVerifyResponse response = TwilioVerifyResponse.from(Map.of("status", "pending", "valid", false));
        assertFalse(response.approved());
    }
}
