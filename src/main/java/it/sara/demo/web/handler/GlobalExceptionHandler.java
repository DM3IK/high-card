package it.sara.demo.web.handler;

import it.sara.demo.dto.StatusDTO;
import it.sara.demo.exception.GenericException;
import it.sara.demo.web.response.GenericResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Turns every exception raised while handling a request into a {@link GenericResponse}.
 * The HTTP status is always 200; the real outcome is in the response {@link StatusDTO}.
 * This is the only place where request errors are logged: 5xx errors at ERROR level with the stack trace,
 * 4xx errors at WARN level with the message only. Both include the trace id returned to the client.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles errors raised by the service layer, keeping their code and message.
     *
     * @param e the service error
     * @return a response with the status carried by the exception
     */
    @ExceptionHandler(GenericException.class)
    public ResponseEntity<GenericResponse> handleGenericException(GenericException e) {
        return toResponse(e);
    }

    /**
     * Handles a missing or malformed JSON body, without exposing the parser details.
     *
     * @param e the parsing error
     * @return a response with code 400
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<GenericResponse> handleMalformedBody(HttpMessageNotReadableException e) {
        return toResponse(new GenericException(400, "Malformed request body", e));
    }

    /**
     * Handles a request sent with an HTTP method the endpoint does not support.
     *
     * @param e the method error
     * @return a response with code 405
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<GenericResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return toResponse(new GenericException(405, "Method not supported", e));
    }

    /**
     * Handles a request body sent with a content type the endpoint does not accept.
     *
     * @param e the content type error
     * @return a response with code 415
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<GenericResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        return toResponse(new GenericException(415, "Media type not supported", e));
    }

    /**
     * Handles a request to a path that does not exist.
     *
     * @param e the missing resource error
     * @return a response with code 404
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<GenericResponse> handleNotFound(NoResourceFoundException e) {
        return toResponse(new GenericException(404, "Resource not found", e));
    }

    /**
     * Handles any other error as a generic error, so that internal details never reach the client.
     *
     * @param e the unexpected error
     * @return a response with code 500 and the message "Generic error"
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse> handleUnexpected(Exception e) {
        return toResponse(GenericException.genericError(e));
    }

    private ResponseEntity<GenericResponse> toResponse(GenericException e) {
        StatusDTO status = e.getStatus();
        if (status.getCode() >= 500) {
            log.error("Request failed [code={}, traceId={}]", status.getCode(), status.getTraceId(), e);
        } else {
            log.warn("Request rejected [code={}, traceId={}]: {}", status.getCode(), status.getTraceId(), status.getMessage());
        }
        return ResponseEntity.ok(GenericResponse.error(status));
    }
}
