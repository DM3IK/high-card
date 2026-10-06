package it.sara.demo.web.security;

import java.time.Duration;
import java.time.Instant;

/**
 * Prints a token for calling the running application by hand, for example with curl or Postman.
 * It lives in the test sources on purpose: the production code exposes no endpoint that issues tokens.
 * <p>
 * Usage: set the {@code JWT_SECRET} environment variable to the same value used to start the application,
 * then run this class from the IDE. The token is valid for one hour, has the issuer of
 * {@code src/main/resources/application.properties} and both scopes ({@code users:read} and {@code users:write}).
 * Send it as {@code Authorization: Bearer <token>}.
 */
public final class JwtTokenGenerator {

    private static final String ISSUER = "https://auth.high-card.local";

    private JwtTokenGenerator() {
    }

    /**
     * Prints the token to standard output.
     *
     * @param args not used
     */
    public static void main(String[] args) {
        String secret = System.getenv("JWT_SECRET");
        if (secret == null || secret.isBlank()) {
            System.err.println("Set the JWT_SECRET environment variable to the secret used by the application.");
            System.exit(1);
        }
        System.out.println(TestTokens.token(secret, ISSUER, Instant.now().plus(Duration.ofHours(1)),
                "users:read", "users:write"));
    }
}
