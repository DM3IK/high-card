package it.sara.demo.web.security;

import it.sara.demo.dto.StatusDTO;
import it.sara.demo.exception.GenericException;
import it.sara.demo.web.response.GenericResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

/**
 * Writes authentication (401) and authorization (403) errors in the same format as every other response:
 * HTTP status 200 and the real outcome in {@link StatusDTO}. Spring Security raises these errors in its filters,
 * before the controllers, so {@link it.sara.demo.web.handler.GlobalExceptionHandler} never sees them.
 * The reason (for example an expired token or a wrong issuer) is logged at WARN level with the trace id,
 * and is never sent to the client.
 */
@Slf4j
@NullMarked
@Component
@RequiredArgsConstructor
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final JsonMapper jsonMapper;

    /**
     * Handles a request without a valid token: missing, malformed, badly signed, expired or from another issuer.
     *
     * @param request       the rejected request
     * @param response      the response to write
     * @param authException the reason of the rejection
     * @throws IOException if the response cannot be written
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        write(response, new GenericException(401, "Missing or invalid token", authException));
    }

    /**
     * Handles a valid token that does not have the scope required by the endpoint.
     *
     * @param request               the rejected request
     * @param response              the response to write
     * @param accessDeniedException the reason of the rejection
     * @throws IOException if the response cannot be written
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        write(response, new GenericException(403, "Insufficient permissions", accessDeniedException));
    }

    private void write(HttpServletResponse response, GenericException error) throws IOException {
        StatusDTO status = error.getStatus();
        log.warn("Request rejected [code={}, traceId={}]: {} ({})",
                status.getCode(), status.getTraceId(), status.getMessage(), error.getCause().getMessage());
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        jsonMapper.writeValue(response.getOutputStream(), GenericResponse.error(status));
    }
}
