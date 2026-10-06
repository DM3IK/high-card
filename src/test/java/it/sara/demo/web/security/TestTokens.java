package it.sara.demo.web.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Builds HS256 tokens for tests and for {@link JwtTokenGenerator}.
 * The issuer and secret match {@code src/test/resources/application.properties}.
 */
public final class TestTokens {

    public static final String ISSUER = "https://auth.high-card.test";
    public static final String SECRET = "test-secret-for-unit-tests-only-0123456789";

    private TestTokens() {
    }

    /**
     * Creates a token accepted by the test configuration, valid for one hour.
     *
     * @param scopes the scopes to grant, for example {@code users:read}
     * @return the serialized token
     */
    public static String valid(String... scopes) {
        return token(SECRET, ISSUER, Instant.now().plus(Duration.ofHours(1)), scopes);
    }

    /**
     * Creates a signed token with the given values. The token is issued one hour before it expires,
     * so that an already expired token is still consistent ({@code iat} before {@code exp}) and is rejected
     * only because of its expiration.
     *
     * @param secret    the HS256 signing secret
     * @param issuer    the {@code iss} claim
     * @param expiresAt the {@code exp} claim, or null to leave it out
     * @param scopes    the scopes, written as a space-separated {@code scope} claim
     * @return the serialized token
     */
    public static String token(String secret, String issuer, Instant expiresAt, String... scopes) {
        Instant issuedAt = expiresAt != null ? expiresAt.minus(Duration.ofHours(1)) : Instant.now();
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .subject("test-user")
                .issuer(issuer)
                .issueTime(Date.from(issuedAt));
        if (expiresAt != null) {
            claims.expirationTime(Date.from(expiresAt));
        }
        if (scopes.length > 0) {
            claims.claim("scope", String.join(" ", scopes));
        }
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims.build());
        try {
            jwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
        } catch (JOSEException e) {
            throw new IllegalStateException("Cannot sign the test token", e);
        }
        return jwt.serialize();
    }
}
