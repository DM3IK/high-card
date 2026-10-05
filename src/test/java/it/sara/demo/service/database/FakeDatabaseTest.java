package it.sara.demo.service.database;

import it.sara.demo.service.user.validator.UserValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FakeDatabaseTest {

    private final UserValidator validator = new UserValidator();

    /**
     * Regression: the seed phone numbers were "+39" followed by a single digit, which no Italian number matches.
     */
    @Test
    void seedUsers_haveValidEmailAndNormalizedItalianPhoneNumber() {
        assertAll(FakeDatabase.TABLE_USER.stream()
                .<Executable>map(user -> () -> {
                    assertEquals(Optional.of(user.getEmail()), validator.normalizeEmail(user.getEmail()));
                    assertEquals(Optional.of(user.getPhoneNumber()), validator.normalizePhoneNumber(user.getPhoneNumber()));
                }));
    }
}
