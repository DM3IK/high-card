package it.sara.demo.exception;

import it.sara.demo.dto.StatusDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GenericExceptionTest {

    @Test
    void constructorWithCodeAndMessage_createsStatusWithTraceIdAndExceptionMessage() {
        GenericException exception = new GenericException(400, "First name is required");

        assertAll(
                () -> assertEquals(400, exception.getStatus().getCode()),
                () -> assertEquals("First name is required", exception.getStatus().getMessage()),
                () -> assertNotNull(exception.getStatus().getTraceId()),
                () -> assertEquals("First name is required", exception.getMessage()),
                () -> assertNull(exception.getCause())
        );
    }

    @Test
    void constructorWithStatus_keepsTheGivenStatus() {
        StatusDTO status = new StatusDTO();
        status.setCode(404);
        status.setMessage("Not found");

        GenericException exception = new GenericException(status);

        assertAll(
                () -> assertSame(status, exception.getStatus()),
                () -> assertEquals("Not found", exception.getMessage())
        );
    }

    @Test
    @SuppressWarnings("ThrowableNotThrown")
    void constructorWithStatus_rejectsNullStatus() {
        assertThrows(NullPointerException.class, () -> new GenericException(null));
    }

    /**
     * Regression: all generic errors used to share one static, mutable status without a trace id.
     */
    @Test
    void genericError_returnsIndependentStatusOnEveryCall() {
        GenericException first = GenericException.genericError(new IllegalStateException("first"));
        GenericException second = GenericException.genericError(new IllegalStateException("second"));

        first.getStatus().setMessage("changed by a caller");

        assertAll(
                () -> assertNotSame(first.getStatus(), second.getStatus()),
                () -> assertNotEquals(first.getStatus().getTraceId(), second.getStatus().getTraceId()),
                () -> assertEquals("Generic error", second.getStatus().getMessage())
        );
    }

    @Test
    void genericErrorWithCause_keepsCauseButExposesOnlyTheGenericMessage() {
        IllegalStateException cause = new IllegalStateException("database connection lost");

        GenericException exception = GenericException.genericError(cause);

        assertAll(
                () -> assertSame(cause, exception.getCause()),
                () -> assertEquals(500, exception.getStatus().getCode()),
                () -> assertEquals("Generic error", exception.getStatus().getMessage()),
                () -> assertEquals("Generic error", exception.getMessage()),
                () -> assertNotNull(exception.getStatus().getTraceId())
        );
    }
}
