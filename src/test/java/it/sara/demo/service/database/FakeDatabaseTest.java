package it.sara.demo.service.database;

import it.sara.demo.service.user.validator.UserValidator;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FakeDatabaseTest {

    private final UserValidator validator = new UserValidator();

    /**
     * Regression: the seed names contained digits ("First name 0") and the phone numbers were "+39"
     * followed by a single digit, so the seed data did not pass the validation applied to new users.
     */
    @Test
    void seedUsers_passTheSameValidationAsNewUsers() {
        assertAll(FakeDatabase.TABLE_USER.stream()
                .map(user -> () -> {
                    assertEquals(Optional.of(user.getFirstName()), validator.normalizeName(user.getFirstName()));
                    assertEquals(Optional.of(user.getLastName()), validator.normalizeName(user.getLastName()));
                    assertEquals(Optional.of(user.getEmail()), validator.normalizeEmail(user.getEmail()));
                    assertEquals(Optional.of(user.getPhoneNumber()), validator.normalizePhoneNumber(user.getPhoneNumber()));
                }));
    }
}
