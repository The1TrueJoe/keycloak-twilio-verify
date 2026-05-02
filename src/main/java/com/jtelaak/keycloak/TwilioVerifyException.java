package com.jtelaak.keycloak;

public class TwilioVerifyException extends Exception {

    private final String adminMessage;

    public TwilioVerifyException(String adminMessage) {
        super(adminMessage);
        this.adminMessage = adminMessage;
    }

    public TwilioVerifyException(String adminMessage, Throwable cause) {
        super(adminMessage, cause);
        this.adminMessage = adminMessage;
    }

    public String getAdminMessage() {
        return adminMessage;
    }
}
