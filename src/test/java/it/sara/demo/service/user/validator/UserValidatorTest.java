package it.sara.demo.service.user.validator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserValidatorTest {

    private final UserValidator validator = new UserValidator();

    @ParameterizedTest
    @ValueSource(strings = {
            "Mario",
            "De Luca",
            "Anna-Maria",
            "D'Angelo",
            "Dell'Orto",
            "Niccolò",
            "José",
            "Łukasz",
            "Maria Assunta Concetta"
    })
    void normalizeName_withValidName_returnsItUnchanged(String name) {
        assertEquals(Optional.of(name), validator.normalizeName(name));
    }

    @Test
    void normalizeName_withSurroundingSpacesOrDecomposedAccent_returnsTrimmedComposedName() {
        assertAll(
                () -> assertEquals(Optional.of("Mario"), validator.normalizeName("  Mario  ")),
                () -> assertEquals(Optional.of("Niccolò"), validator.normalizeName("Niccolò"))
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "   ",
            "Robert'); DROP TABLE users;--",
            "' OR '1'='1",
            "1=1",
            "Mario;",
            "Mario--",
            "Mario/*",
            "<script>",
            "\"Mario\"",
            "Mario2",
            "Mario_Rossi",
            "O''Brien",
            "'Mario",
            "Mario'",
            "-Mario",
            "Mario  Rossi",
            "Mario\tRossi"
    })
    void normalizeName_withInvalidOrInjectionInput_returnsEmpty(String name) {
        assertTrue(validator.normalizeName(name).isEmpty());
    }

    @Test
    void normalizeName_atLengthLimit_acceptsFiftyCharactersAndRejectsFiftyOne() {
        assertAll(
                () -> assertEquals(Optional.of("a".repeat(50)), validator.normalizeName("a".repeat(50))),
                () -> assertTrue(validator.normalizeName("a".repeat(51)).isEmpty())
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "mario.rossi@example.com",
            "mario+news@example.it",
            "m_rossi-2@mail.example.co.uk",
            "MARIO%ROSSI@EXAMPLE.COM",
            "a@b.co"
    })
    void normalizeEmail_withWellFormedAddress_returnsItUnchanged(String email) {
        assertEquals(Optional.of(email), validator.normalizeEmail(email));
    }

    @ParameterizedTest
    @ValueSource(strings = {" mario@example.com", "mario@example.com ", "  mario@example.com  "})
    void normalizeEmail_withLeadingOrTrailingSpaces_returnsTrimmedAddress(String email) {
        assertEquals(Optional.of("mario@example.com"), validator.normalizeEmail(email));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            " ",
            "mario.rossi",
            "mario@",
            "@example.com",
            "mario@@example.com",
            "mario..rossi@example.com",
            ".mario@example.com",
            "mario.@example.com",
            "mario@example",
            "mario@example.c",
            "mario@example.123",
            "mario@-example.com",
            "mario@example-.com",
            "mario@exa_mple.com",
            "mario@example..com",
            "mario rossi@example.com",
            "mario@example .com",
            "mario'--@example.com",
            "mario;drop@example.com",
            "\"mario\"@example.com"
    })
    void normalizeEmail_withMalformedAddress_returnsEmpty(String email) {
        assertTrue(validator.normalizeEmail(email).isEmpty());
    }

    @Test
    void normalizeEmail_atLengthLimits_acceptsMaximumAndRejectsOneMore() {
        String longest = email(64, 254);
        String localPartTooLong = email(65, 254);
        String totalTooLong = email(64, 255);

        assertAll(
                () -> assertEquals(254, longest.length()),
                () -> assertEquals(255, totalTooLong.length()),
                () -> assertEquals(Optional.of(longest), validator.normalizeEmail(longest)),
                () -> assertTrue(validator.normalizeEmail(localPartTooLong).isEmpty()),
                () -> assertTrue(validator.normalizeEmail(totalTooLong).isEmpty())
        );
    }

    @ParameterizedTest
    @CsvSource({
            "+393331234567,       +393331234567",
            "3331234567,          +393331234567",
            "0039 333 123 4567,   +393331234567",
            "+39 333-123-4567,    +393331234567",
            "333123456,           +39333123456",
            "0612345678,          +390612345678",
            "+39 06 1234 5678,    +390612345678",
            "012345,              +39012345",
            "01234567890,         +3901234567890"
    })
    void normalizePhoneNumber_withValidItalianNumber_returnsPlus39AndDigits(String phoneNumber, String expected) {
        assertEquals(Optional.of(expected), validator.normalizePhoneNumber(phoneNumber));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "   ",
            "+39",
            "12345678",
            "33312345",
            "33312345678",
            "01234",
            "012345678901",
            "+44 333 1234567",
            "+393331234567a",
            "+39  3331234567",
            "-3331234567",
            "3331234567-",
            "+39(333)1234567",
            "++393331234567",
            "0039+3331234567",
            "333.123.4567"
    })
    void normalizePhoneNumber_withInvalidNumber_returnsEmpty(String phoneNumber) {
        assertTrue(validator.normalizePhoneNumber(phoneNumber).isEmpty());
    }

    private static String email(int localPartLength, int totalLength) {
        StringBuilder domain = new StringBuilder("it");
        int remaining = totalLength - localPartLength - 1 - domain.length();
        while (remaining > 0) {
            int label = Math.min(63, remaining - 1);
            domain.insert(0, "b".repeat(label) + ".");
            remaining -= label + 1;
        }
        return "a".repeat(localPartLength) + "@" + domain;
    }
}
