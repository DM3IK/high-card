package it.sara.demo.web.security;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * JWT settings, bound from the {@code security.jwt.*} properties and checked at startup,
 * so that the application never runs with a missing issuer or a weak signing secret.
 *
 * @param issuer    the only issuer ({@code iss} claim) accepted in tokens
 * @param secret    the shared HS256 secret, at least 256 bits; set it with the {@code JWT_SECRET} environment variable
 * @param clockSkew the tolerance applied to the expiration time, to absorb small clock differences; defaults to 30 seconds
 */
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(String issuer, String secret, Duration clockSkew) {

    private static final int MIN_SECRET_BYTES = 32;
    private static final Duration DEFAULT_CLOCK_SKEW = Duration.ofSeconds(30);

    /**
     * Validates the settings and applies the default clock skew.
     *
     * @param issuer    the only issuer accepted in tokens, not blank
     * @param secret    the shared HS256 secret, at least 32 bytes
     * @param clockSkew the expiration tolerance, not negative; null means 30 seconds
     * @throws IllegalStateException if the issuer is blank, the secret is shorter than 256 bits
     *                               or the clock skew is negative
     */
    public JwtProperties {
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalStateException("security.jwt.issuer must be set");
        }
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "security.jwt.secret must be at least 256 bits (32 bytes); set it with the JWT_SECRET environment variable");
        }
        if (clockSkew == null) {
            clockSkew = DEFAULT_CLOCK_SKEW;
        }
        if (clockSkew.isNegative()) {
            throw new IllegalStateException("security.jwt.clock-skew must not be negative");
        }
    }

    /**
     * Describes the settings without the secret, which the default record {@code toString()} would print
     * in clear text if the object ever ended up in a log or an error message.
     *
     * @return the issuer and clock skew, with the secret masked
     */
    @Override
    public @NonNull String toString() {
        return "JwtProperties[issuer=" + issuer + ", secret=****, clockSkew=" + clockSkew + "]";
    }
}
