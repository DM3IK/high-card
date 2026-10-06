package it.sara.demo.web.security;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import it.sara.demo.service.database.FakeDatabase;
import it.sara.demo.service.database.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class JwtSecurityIntegrationTest {

    private static final String USER_URL = "/user/v1/user";
    private static final String NEW_USER = """
            {"firstName":"Mario","lastName":"Verdi","email":"mario.verdi@example.com","phoneNumber":"3331234567"}
            """;

    @Autowired
    private MockMvc mockMvc;

    private final List<User> seed = List.copyOf(FakeDatabase.TABLE_USER);

    @AfterEach
    void restoreTable() {
        FakeDatabase.TABLE_USER.retainAll(seed);
    }

    @Test
    void search_withReadScope_isAllowed() throws Exception {
        search(TestTokens.valid("users:read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(200));
    }

    @Test
    void addUser_withWriteScope_isAllowed() throws Exception {
        addUser(TestTokens.valid("users:write"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(200));

        assertEquals(seed.size() + 1, FakeDatabase.TABLE_USER.size());
    }

    @Test
    void request_withoutToken_returnsHttp200WithCode401() throws Exception {
        mockMvc.perform(post(USER_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(401))
                .andExpect(jsonPath("$.status.message").value("Missing or invalid token"))
                .andExpect(jsonPath("$.status.traceId").isNotEmpty());
    }

    /**
     * Checks the logged reason as well as the code: a token can be rejected for a different reason
     * than the one under test (for example an inconsistent issue time instead of the expiration).
     * The reason must be logged at WARN level with the trace id and never sent to the client.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidTokens")
    void search_withInvalidToken_returnsCode401AndLogsTheExpectedReason(String description, String token,
                                                                        String expectedReason,
                                                                        CapturedOutput output) throws Exception {
        String body = search(token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(401))
                .andExpect(jsonPath("$.status.message").value("Missing or invalid token"))
                .andReturn().getResponse().getContentAsString();

        String logLine = logLineWithTraceIdOf(body, output);
        assertAll(
                () -> assertTrue(logLine.contains("WARN"), logLine),
                () -> assertTrue(logLine.contains(expectedReason), logLine),
                () -> assertFalse(body.contains(expectedReason), body)
        );
    }

    static Stream<Arguments> invalidTokens() {
        Instant inOneHour = Instant.now().plus(Duration.ofHours(1));
        return Stream.of(
                Arguments.of("expired beyond the clock skew",
                        TestTokens.token(TestTokens.SECRET, TestTokens.ISSUER,
                                Instant.now().minus(Duration.ofSeconds(60)), "users:read"),
                        "Jwt expired at"),
                Arguments.of("without expiration",
                        TestTokens.token(TestTokens.SECRET, TestTokens.ISSUER, null, "users:read"),
                        "The exp claim is not valid"),
                Arguments.of("wrong issuer",
                        TestTokens.token(TestTokens.SECRET, "https://attacker.example", inOneHour, "users:read"),
                        "The iss claim is not valid"),
                Arguments.of("wrong signature",
                        TestTokens.token("another-secret-that-is-long-enough-0123456789", TestTokens.ISSUER,
                                inOneHour, "users:read"),
                        "Invalid signature"),
                Arguments.of("unsigned (alg none)", unsignedToken(inOneHour), "Unsupported algorithm of none"),
                Arguments.of("malformed", "not-a-jwt", "Malformed token")
        );
    }

    @Test
    void search_withTokenExpiredWithinClockSkew_isAllowed() throws Exception {
        String token = TestTokens.token(TestTokens.SECRET, TestTokens.ISSUER,
                Instant.now().minus(Duration.ofSeconds(10)), "users:read");

        search(token).andExpect(jsonPath("$.status.code").value(200));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("missingScopes")
    void request_withoutRequiredScope_returnsHttp200WithCode403(String description, boolean isAddUser, String token)
            throws Exception {
        ResultActions result = isAddUser ? addUser(token) : search(token);

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(403))
                .andExpect(jsonPath("$.status.message").value("Insufficient permissions"));
        assertEquals(seed.size(), FakeDatabase.TABLE_USER.size());
    }

    static Stream<Arguments> missingScopes() {
        return Stream.of(
                Arguments.of("PUT with read scope only", true, TestTokens.valid("users:read")),
                Arguments.of("POST with write scope only", false, TestTokens.valid("users:write")),
                Arguments.of("POST without scopes", false, TestTokens.valid())
        );
    }

    private static String logLineWithTraceIdOf(String responseBody, CapturedOutput output) {
        String traceId = responseBody.replaceAll(".*\"traceId\":\"([^\"]+)\".*", "$1");
        return output.getAll().lines().filter(line -> line.contains(traceId)).findFirst().orElse("");
    }

    private ResultActions search(String token) throws Exception {
        return mockMvc.perform(post(USER_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private ResultActions addUser(String token) throws Exception {
        return mockMvc.perform(put(USER_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(NEW_USER));
    }

    private static String unsignedToken(Instant expiresAt) {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(TestTokens.ISSUER)
                .expirationTime(Date.from(expiresAt))
                .claim("scope", "users:read")
                .build();
        return new PlainJWT(claims).serialize();
    }
}
