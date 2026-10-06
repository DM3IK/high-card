package it.sara.demo.web.response;

import it.sara.demo.dto.StatusDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Base type of every response body. The outcome of the request is always in {@link #status},
 * because the HTTP status code is 200 for both successes and errors.
 */
@Getter
@Setter
public class GenericResponse {

    /** Outcome of the request: code, message and trace id. */
    private StatusDTO status;

    /**
     * Creates the status of a successful request, for responses that extend this class.
     *
     * @param message the message for the client, or null for the default "Success"
     * @return a new status with code 200 and a random trace id
     */
    public static StatusDTO successStatus(String message) {
        StatusDTO returnValue = new StatusDTO();
        returnValue.setCode(200);
        returnValue.setMessage(message != null ? message : "Success");
        returnValue.setTraceId(UUID.randomUUID().toString());
        return returnValue;
    }

    /**
     * Creates an error response.
     *
     * @param status the status describing the error
     * @return a new response carrying the given status
     */
    public static GenericResponse error(StatusDTO status) {
        GenericResponse returnValue = new GenericResponse();
        returnValue.setStatus(status);
        return returnValue;
    }
}
