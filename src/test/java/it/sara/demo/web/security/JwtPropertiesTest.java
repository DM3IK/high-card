package it.sara.demo.web.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtPropertiesTest {

    private static final String ISSUER = "https://auth.high-card.test";
    private static final String SECRET_32_BYTES = "a".repeat(32);

    @Test
    void constructor_withValidValues_keepsThem() {
        JwtProperties properties = new JwtProperties(ISSUER, SECRET_32_BYTES, Duration.ofSeconds(10));

        assertAll(
                () -> assertEquals(ISSUER, properties.issuer()),
                () -> assertEquals(SECRET_32_BYTES, properties.secret()),
                () -> assertEquals(Duration.ofSeconds(10), properties.clockSkew())
        );
    }

    @Test
    void constructor_withoutClockSkew_usesThirtySeconds() {
        assertEquals(Duration.ofSeconds(30), new JwtProperties(ISSUER, SECRET_32_BYTES, null).clockSkew());
    }

    /**
     * A missing or short secret must stop the application at startup instead of accepting weak signatures.
     */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"short-secret"})
    void constructor_withMissingOrShortSecret_fails(String secret) {
        assertThrows(IllegalStateException.class, () -> new JwtProperties(ISSUER, secret, null));
    }

    @Test
    void constructor_atSecretLengthLimit_acceptsThirtyTwoBytesAndRejectsThirtyOne() {
        assertAll(
                () -> assertEquals(SECRET_32_BYTES, new JwtProperties(ISSUER, SECRET_32_BYTES, null).secret()),
                () -> assertThrows(IllegalStateException.class,
                        () -> new JwtProperties(ISSUER, "a".repeat(31), null))
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void constructor_withMissingIssuer_fails(String issuer) {
        assertThrows(IllegalStateException.class, () -> new JwtProperties(issuer, SECRET_32_BYTES, null));
    }

    /**
     * The default record {@code toString()} prints every component, so the secret would end up in clear text
     * in any log or error message that prints the settings.
     */
    @Test
    void toString_doesNotContainTheSecret() {
        String description = new JwtProperties(ISSUER, SECRET_32_BYTES, null).toString();

        assertAll(
                () -> assertFalse(description.contains(SECRET_32_BYTES), description),
                () -> assertTrue(description.contains(ISSUER), description)
        );
    }

    @Test
    void constructor_withNegativeClockSkew_fails() {
        assertThrows(IllegalStateException.class,
                () -> new JwtProperties(ISSUER, SECRET_32_BYTES, Duration.ofSeconds(-1)));
    }
}
