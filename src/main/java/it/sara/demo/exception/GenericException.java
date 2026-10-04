package it.sara.demo.exception;

import it.sara.demo.dto.StatusDTO;
import lombok.Getter;

import java.util.Objects;
import java.util.UUID;

/**
 * Checked exception thrown by the service layer.
 * It carries the {@link StatusDTO} (code, message and trace id) that describes the error to the client.
 */
@Getter
public class GenericException extends Exception {

    private static final int GENERIC_ERROR_CODE = 500;
    private static final String GENERIC_ERROR_MESSAGE = "Generic error";

    /** Outcome of the failed operation, owned by this exception instance. */
    private final StatusDTO status;

    /**
     * Creates an exception from an existing status.
     *
     * @param status the status describing the error, not null
     */
    public GenericException(StatusDTO status) {
        this(status, null);
    }

    /**
     * Creates an exception with a new status and a random trace id.
     *
     * @param code    the error code, for example 400 for invalid input
     * @param message the message returned to the client
     */
    public GenericException(int code, String message) {
        this(createStatus(code, message));
    }

    /**
     * Creates an exception with a new status and a random trace id, keeping the original cause.
     * The cause appears only in the stack trace; the client still receives just {@code message}.
     *
     * @param code    the error code, for example 500 for an unexpected failure
     * @param message the message returned to the client
     * @param cause   the original exception
     */
    public GenericException(int code, String message, Throwable cause) {
        this(createStatus(code, message), cause);
    }

    private GenericException(StatusDTO status, Throwable cause) {
        super(Objects.requireNonNull(status, "status must not be null").getMessage(), cause);
        this.status = status;
    }

    /**
     * Creates the exception used for unexpected errors, keeping the original cause in the stack trace.
     * Each call returns a new instance with its own status and trace id.
     * The client still receives only "Generic error", never the details of the cause.
     *
     * @param cause the unexpected exception
     * @return a new exception with code 500, its own trace id and the given cause
     */
    public static GenericException genericError(Throwable cause) {
        return new GenericException(GENERIC_ERROR_CODE, GENERIC_ERROR_MESSAGE, cause);
    }

    private static StatusDTO createStatus(int code, String message) {
        StatusDTO status = new StatusDTO();
        status.setCode(code);
        status.setMessage(message);
        status.setTraceId(UUID.randomUUID().toString());
        return status;
    }
}
