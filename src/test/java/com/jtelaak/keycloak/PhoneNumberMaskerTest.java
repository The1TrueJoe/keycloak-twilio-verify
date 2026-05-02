package com.jtelaak.keycloak;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PhoneNumberMaskerTest {

    @Test
    void masksE164Numbers() {
        assertEquals("+*******4567", PhoneNumberMasker.mask("+15551234567"));
    }

    @Test
    void masksShortNumbersCompletely() {
        assertEquals("****", PhoneNumberMasker.mask("1234"));
    }

    @Test
    void stripsFormattingBeforeMasking() {
        assertEquals("*******4567", PhoneNumberMasker.mask("(555) 123-4567"));
    }
}
