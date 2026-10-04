package it.sara.demo.web.assembler;

import it.sara.demo.service.user.criteria.CriteriaAddUser;
import it.sara.demo.web.user.request.AddUserRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AddUserAssemblerTest {

    private final AddUserAssembler assembler = new AddUserAssembler();

    @Test
    void toCriteria_mapsEveryFieldToTheMatchingCriteriaField() {
        AddUserRequest request = new AddUserRequest();
        request.setFirstName("Mario");
        request.setLastName("Rossi");
        request.setEmail("mario.rossi@example.com");
        request.setPhoneNumber("+393331234567");

        CriteriaAddUser criteria = assembler.toCriteria(request);

        assertAll(
                () -> assertEquals("Mario", criteria.getFirstName()),
                () -> assertEquals("Rossi", criteria.getLastName()),
                () -> assertEquals("mario.rossi@example.com", criteria.getEmail()),
                () -> assertEquals("+393331234567", criteria.getPhoneNumber())
        );
    }

    /**
     * Fields missing from the JSON body arrive as null. The assembler must pass them through
     * without failing, so that the service can reject them with a proper validation error.
     */
    @Test
    void toCriteria_withNullFields_passesNullsThroughWithoutFailing() {
        AddUserRequest request = new AddUserRequest();

        CriteriaAddUser criteria = assembler.toCriteria(request);

        assertAll(
                () -> assertNull(criteria.getFirstName()),
                () -> assertNull(criteria.getLastName()),
                () -> assertNull(criteria.getEmail()),
                () -> assertNull(criteria.getPhoneNumber())
        );
    }
}
