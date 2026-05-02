package com.jtelaak.keycloak;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;

import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

final class TwilioVerifyUserSupport {

    private static final Pattern E164 = Pattern.compile("^\\+[1-9]\\d{7,14}$");

    private TwilioVerifyUserSupport() {
    }

    static Optional<String> normalizePhone(String primary, String fallback) {
        String raw = primary == null || primary.isBlank() ? fallback : primary;
        if (raw == null) {
            return Optional.empty();
        }

        String phoneNumber = raw.trim().replaceAll("[\\s().-]+", "");
        return E164.matcher(phoneNumber).matches() ? Optional.of(phoneNumber) : Optional.empty();
    }

    static Optional<UserModel> findUserByPhone(KeycloakSession session, RealmModel realm, TwilioVerifyConfig config,
            String phoneNumber) {
        Stream<UserModel> configuredAttributeMatches = session.users()
                .searchForUserByUserAttributeStream(realm, config.phoneAttribute(), phoneNumber);
        Stream<UserModel> commonAttributeMatches = "phoneNumber".equals(config.phoneAttribute())
                ? Stream.empty()
                : session.users().searchForUserByUserAttributeStream(realm, "phoneNumber", phoneNumber);

        Optional<UserModel> byAttribute = Stream.concat(configuredAttributeMatches, commonAttributeMatches)
                .findFirst();
        if (byAttribute.isPresent()) {
            return byAttribute;
        }

        return Optional.ofNullable(session.users().getUserByUsername(realm, phoneNumber));
    }

    static UserModel findOrCreateUserByPhone(KeycloakSession session, RealmModel realm, TwilioVerifyConfig config,
            String phoneNumber) {
        return findUserByPhone(session, realm, config, phoneNumber)
                .orElseGet(() -> createUser(session, realm, config, phoneNumber));
    }

    static void markPhoneVerified(UserModel user, TwilioVerifyConfig config, String phoneNumber) {
        user.setSingleAttribute(config.phoneAttribute(), phoneNumber);
        user.setSingleAttribute("phoneNumber", phoneNumber);
        user.setSingleAttribute("phoneNumberVerified", "true");
        user.setSingleAttribute("phone_number", phoneNumber);
        user.setSingleAttribute("phone_number_verified", "true");
    }

    static void grantConfiguredRole(RealmModel realm, UserModel user, TwilioVerifyConfig config) {
        String roleName = config.userCreationRealmRole();
        if (roleName == null || roleName.isBlank()) {
            return;
        }

        RoleModel role = realm.getRole(roleName.trim());
        if (role != null && !user.hasRole(role)) {
            user.grantRole(role);
        }
    }

    private static UserModel createUser(KeycloakSession session, RealmModel realm, TwilioVerifyConfig config,
            String phoneNumber) {
        UserModel user = session.users().addUser(realm, phoneNumber);
        user.setEnabled(true);
        markPhoneVerified(user, config, phoneNumber);
        grantConfiguredRole(realm, user, config);
        return user;
    }
}
