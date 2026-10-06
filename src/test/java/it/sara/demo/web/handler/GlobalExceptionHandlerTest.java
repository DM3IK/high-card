package it.sara.demo.web.handler;

import com.jayway.jsonpath.JsonPath;
import it.sara.demo.exception.GenericException;
import it.sara.demo.service.user.UserService;
import it.sara.demo.web.assembler.AddUserAssembler;
import it.sara.demo.web.assembler.GetUsersAssembler;
import it.sara.demo.web.user.UserController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({AddUserAssembler.class, GetUsersAssembler.class})
@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {

    private static final String USER_URL = "/user/v1/user";

    private static final String VALID_BODY = """
            {"firstName":"Mario","lastName":"Rossi","email":"mario.rossi@example.com","phoneNumber":"+393331234567"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void successfulRequest_isNotAlteredByTheHandler() throws Exception {
        mockMvc.perform(put(USER_URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(200))
                .andExpect(jsonPath("$.status.message").value("User added."))
                .andExpect(jsonPath("$.status.traceId").isNotEmpty());
    }

    @Test
    void genericExceptionWith4xxCode_returnsHttp200WithItsStatusAndLogsWarningWithoutStackTrace(CapturedOutput output)
            throws Exception {
        GenericException error = new GenericException(400, "Email is required");
        when(userService.addUser(any())).thenThrow(error);

        mockMvc.perform(put(USER_URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(400))
                .andExpect(jsonPath("$.status.message").value("Email is required"))
                .andExpect(jsonPath("$.status.traceId").value(error.getStatus().getTraceId()));

        assertAll(
                () -> assertTrue(logLineContaining(output, error.getStatus().getTraceId()).contains("WARN")),
                () -> assertFalse(output.getAll().contains(GenericException.class.getName() + ": Email is required"))
        );
    }

    /**
     * The trace id returned to the client must also be in the log, so that a reported error
     * can be found on the server side.
     */
    @Test
    void genericExceptionWith5xxCode_returnsGenericErrorAndLogsErrorWithTraceIdAndCause(CapturedOutput output)
            throws Exception {
        GenericException error = GenericException.genericError(new IllegalStateException("database connection lost"));
        when(userService.addUser(any())).thenThrow(error);

        mockMvc.perform(put(USER_URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(500))
                .andExpect(jsonPath("$.status.message").value("Generic error"))
                .andExpect(jsonPath("$.status.traceId").value(error.getStatus().getTraceId()))
                .andExpect(content().string(not(containsString("database connection lost"))));

        assertAll(
                () -> assertTrue(logLineContaining(output, error.getStatus().getTraceId()).contains("ERROR")),
                () -> assertTrue(output.getAll().contains("database connection lost"))
        );
    }

    @Test
    void unexpectedException_returnsGenericErrorWithoutInternalDetailsAndLogsIt(CapturedOutput output)
            throws Exception {
        when(userService.addUser(any())).thenThrow(new IllegalStateException("internal detail"));

        String body = mockMvc.perform(put(USER_URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(500))
                .andExpect(jsonPath("$.status.message").value("Generic error"))
                .andExpect(jsonPath("$.status.traceId").isNotEmpty())
                .andExpect(content().string(not(containsString("internal detail"))))
                .andReturn().getResponse().getContentAsString();

        String traceId = JsonPath.read(body, "$.status.traceId");
        assertAll(
                () -> assertTrue(logLineContaining(output, traceId).contains("ERROR")),
                () -> assertTrue(output.getAll().contains("internal detail"))
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{\"firstName\":", "not json"})
    void missingOrMalformedBody_returns400WithoutParserDetails(String body) throws Exception {
        mockMvc.perform(put(USER_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(400))
                .andExpect(jsonPath("$.status.message").value("Malformed request body"))
                .andExpect(content().string(not(containsString("JSON"))));

        verifyNoInteractions(userService);
    }

    @ParameterizedTest(name = "{1} {2}")
    @MethodSource("requestErrors")
    void requestErrorsDetectedBySpring_returnHttp200WithMatchingCode(MockHttpServletRequestBuilder request,
                                                                     int expectedCode,
                                                                     String expectedMessage) throws Exception {
        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(expectedCode))
                .andExpect(jsonPath("$.status.message").value(expectedMessage))
                .andExpect(jsonPath("$.status.traceId").isNotEmpty());

        verifyNoInteractions(userService);
    }

    static Stream<Arguments> requestErrors() {
        return Stream.of(
                Arguments.of(delete(USER_URL), 405, "Method not supported"),
                Arguments.of(put(USER_URL).contentType(MediaType.TEXT_PLAIN).content("Mario"), 415,
                        "Media type not supported"),
                Arguments.of(get("/user/v1/unknown"), 404, "Resource not found")
        );
    }

    private static String logLineContaining(CapturedOutput output, String text) {
        return output.getAll().lines().filter(line -> line.contains(text)).findFirst().orElse("");
    }
}
