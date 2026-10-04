package it.sara.demo.service.assembler;

import it.sara.demo.dto.UserDTO;
import it.sara.demo.service.database.model.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserAssemblerTest {

    private final UserAssembler assembler = new UserAssembler();

    @Test
    void toDTO_mapsEveryFieldIncludingFullEmailAndPhoneNumber() {
        User user = new User();
        user.setGuid("guid-1");
        user.setFirstName("Mario");
        user.setLastName("Rossi");
        user.setEmail("mario.rossi@example.com");
        user.setPhoneNumber("+393331234567");

        UserDTO dto = assembler.toDTO(user);

        assertAll(
                () -> assertEquals("guid-1", dto.getGuid()),
                () -> assertEquals("Mario", dto.getFirstName()),
                () -> assertEquals("Rossi", dto.getLastName()),
                () -> assertEquals("mario.rossi@example.com", dto.getEmail()),
                () -> assertEquals("+393331234567", dto.getPhoneNumber())
        );
    }

    /**
     * Regression: the original implementation called {@code substring} on the email
     * and threw a NullPointerException when the email was null.
     */
    @Test
    void toDTO_withNullFields_mapsNullsWithoutFailing() {
        User user = new User();

        UserDTO dto = assembler.toDTO(user);

        assertAll(
                () -> assertNull(dto.getGuid()),
                () -> assertNull(dto.getFirstName()),
                () -> assertNull(dto.getLastName()),
                () -> assertNull(dto.getEmail()),
                () -> assertNull(dto.getPhoneNumber())
        );
    }
}
