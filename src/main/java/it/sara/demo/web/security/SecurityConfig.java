package it.sara.demo.web.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * JWT security for the REST API. Every request needs a Bearer token that is validated against:
 * <ul>
 *     <li><b>signature</b>: HS256 with the configured secret;</li>
 *     <li><b>issuer</b>: the {@code iss} claim must match {@code security.jwt.issuer};</li>
 *     <li><b>expiration</b>: the {@code exp} claim is required and must be in the future, within the clock skew;</li>
 *     <li><b>policy</b>: {@code PUT /user/v1/user} requires the {@code users:write} scope,
 *     {@code POST /user/v1/user} requires {@code users:read}.</li>
 * </ul>
 * The API is stateless, so there is no session and no CSRF protection, which only applies to cookie-based sessions.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    private static final String USER_PATH = "/user/v1/user";
    private static final String READ_AUTHORITY = "SCOPE_users:read";
    private static final String WRITE_AUTHORITY = "SCOPE_users:write";

    /**
     * Defines the access rules and plugs in the JSON error handler for 401 and 403 errors.
     *
     * @param http                 the security builder
     * @param securityErrorHandler writes authentication and authorization errors as HTTP 200 with a status body
     * @return the security filter chain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityErrorHandler securityErrorHandler) {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(HttpMethod.PUT, USER_PATH).hasAuthority(WRITE_AUTHORITY)
                        .requestMatchers(HttpMethod.POST, USER_PATH).hasAuthority(READ_AUTHORITY)
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(securityErrorHandler)
                        .accessDeniedHandler(securityErrorHandler))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(securityErrorHandler)
                        .accessDeniedHandler(securityErrorHandler));
        return http.build();
    }

    /**
     * Creates the decoder that checks signature, issuer and expiration of every token.
     *
     * @param properties the JWT settings
     * @return a decoder for HS256 tokens signed with the configured secret
     */
    @Bean
    public JwtDecoder jwtDecoder(JwtProperties properties) {
        SecretKey key = new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusJwtDecoder returnValue = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        returnValue.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                requiredExpiration(),
                new JwtTimestampValidator(properties.clockSkew()),
                new JwtIssuerValidator(properties.issuer())));
        return returnValue;
    }

    /**
     * Makes the {@code exp} claim mandatory, because {@link JwtTimestampValidator} accepts tokens without it,
     * which would never expire. {@link JwtClaimValidator} rejects a token when the claim is missing and only then
     * applies its predicate, so a predicate that accepts any value turns it into a presence check.
     *
     * @return a validator that rejects tokens without {@code exp}
     */
    private static OAuth2TokenValidator<Jwt> requiredExpiration() {
        return new JwtClaimValidator<>(JwtClaimNames.EXP, value -> true);
    }
}
