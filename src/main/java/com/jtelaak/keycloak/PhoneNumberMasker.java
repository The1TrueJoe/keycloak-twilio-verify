package com.jtelaak.keycloak;

public final class PhoneNumberMasker {

    private PhoneNumberMasker() {
    }

    public static String mask(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return "";
        }

        String trimmed = phoneNumber.trim();
        String digits = trimmed.replaceAll("\\D", "");
        if (digits.length() <= 4) {
            return "****";
        }

        String suffix = digits.substring(digits.length() - 4);
        String prefix = trimmed.startsWith("+") ? "+" : "";
        return prefix + "*******" + suffix;
    }
}
