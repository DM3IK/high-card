package it.sara.demo.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Outcome of a request, returned in the body of every response, errors included.
 */
@Getter
@Setter
public class StatusDTO {

    /**
     * Real outcome of the request, as an HTTP status code (for example 200, 400, 401, 403 or 500).
     * The HTTP status of the response itself is always 200.
     */
    private int code;

    /** Description of the outcome for the client; for errors it never contains internal details. */
    private String message;

    /** Random identifier of the request; errors are logged with the same value, so they can be found on the server. */
    private String traceId;
}
