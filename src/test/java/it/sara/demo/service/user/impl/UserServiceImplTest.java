package it.sara.demo.service.user.impl;

import it.sara.demo.exception.GenericException;
import it.sara.demo.service.database.UserRepository;
import it.sara.demo.service.database.model.User;
import it.sara.demo.service.user.criteria.CriteriaAddUser;
import it.sara.demo.service.user.result.AddUserResult;
import it.sara.demo.service.user.validator.UserValidator;
import it.sara.demo.service.util.StringUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(new StringUtil(), new UserValidator(), userRepository);
    }

    @Test
    void addUser_withValidCriteria_savesUserWithAllFields() throws GenericException {
        when(userRepository.save(any(User.class))).thenReturn(true);

        AddUserResult result = userService.addUser(validCriteria());

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals("Mario", saved.getValue().getFirstName()),
                () -> assertEquals("Rossi", saved.getValue().getLastName()),
                () -> assertEquals("mario.rossi@example.com", saved.getValue().getEmail()),
                () -> assertEquals("+393331234567", saved.getValue().getPhoneNumber())
        );
    }

    /**
     * Regression: validation errors used to be swallowed and turned into a generic 500.
     */
    @ParameterizedTest(name = "{1}")
    @MethodSource("missingRequiredFields")
    void addUser_withMissingRequiredField_throws400WithFieldMessage(Consumer<CriteriaAddUser> clearField,
                                                                     String expectedMessage) {
        CriteriaAddUser criteria = validCriteria();
        clearField.accept(criteria);

        GenericException exception = assertThrows(GenericException.class, () -> userService.addUser(criteria));

        assertAll(
                () -> assertEquals(400, exception.getStatus().getCode()),
                () -> assertEquals(expectedMessage, exception.getStatus().getMessage())
        );
        verify(userRepository, never()).save(any());
    }

    static Stream<Arguments> missingRequiredFields() {
        return Stream.of(
                Arguments.of((Consumer<CriteriaAddUser>) c -> c.setFirstName(null), "First name is required"),
                Arguments.of((Consumer<CriteriaAddUser>) c -> c.setLastName(null), "Last name is required"),
                Arguments.of((Consumer<CriteriaAddUser>) c -> c.setEmail(null), "Email is required"),
                Arguments.of((Consumer<CriteriaAddUser>) c -> c.setPhoneNumber(null), "Phone is required")
        );
    }

    @Test
    void addUser_withFormattedPhoneNumber_savesItNormalized() throws GenericException {
        when(userRepository.save(any(User.class))).thenReturn(true);
        CriteriaAddUser criteria = validCriteria();
        criteria.setPhoneNumber("0039 333-123-4567");

        userService.addUser(criteria);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertEquals("+393331234567", saved.getValue().getPhoneNumber());
    }

    @Test
    void addUser_withSpacesAroundEmail_savesItTrimmed() throws GenericException {
        when(userRepository.save(any(User.class))).thenReturn(true);
        CriteriaAddUser criteria = validCriteria();
        criteria.setEmail(" mario.rossi@example.com ");

        userService.addUser(criteria);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertEquals("mario.rossi@example.com", saved.getValue().getEmail());
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("invalidFormats")
    void addUser_withInvalidEmailOrPhoneNumber_throws400WithFieldMessage(Consumer<CriteriaAddUser> setInvalidValue,
                                                                          String expectedMessage) {
        CriteriaAddUser criteria = validCriteria();
        setInvalidValue.accept(criteria);

        GenericException exception = assertThrows(GenericException.class, () -> userService.addUser(criteria));

        assertAll(
                () -> assertEquals(400, exception.getStatus().getCode()),
                () -> assertEquals(expectedMessage, exception.getStatus().getMessage())
        );
        verify(userRepository, never()).save(any());
    }

    static Stream<Arguments> invalidFormats() {
        return Stream.of(
                Arguments.of((Consumer<CriteriaAddUser>) c -> c.setEmail("mario.rossi"), "Invalid email"),
                Arguments.of((Consumer<CriteriaAddUser>) c -> c.setPhoneNumber("+44 333 1234567"), "Invalid phone number")
        );
    }

    /**
     * Regression: whitespace-only values used to pass validation.
     * One field is enough: every field goes through the same {@link StringUtil#isNullOrBlank(String)} check.
     */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    void addUser_withNullEmptyOrBlankFirstName_throws400(String firstName) {
        CriteriaAddUser criteria = validCriteria();
        criteria.setFirstName(firstName);

        GenericException exception = assertThrows(GenericException.class, () -> userService.addUser(criteria));

        assertEquals(400, exception.getStatus().getCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void addUser_whenRepositoryDoesNotSave_throws500WithSpecificMessage() {
        when(userRepository.save(any(User.class))).thenReturn(false);

        GenericException exception = assertThrows(GenericException.class, () -> userService.addUser(validCriteria()));

        assertAll(
                () -> assertEquals(500, exception.getStatus().getCode()),
                () -> assertEquals("Error saving user", exception.getStatus().getMessage())
        );
    }

    @Test
    void addUser_whenRepositoryFailsUnexpectedly_throwsGenericErrorKeepingCause() {
        RuntimeException failure = new IllegalStateException("database connection lost");
        when(userRepository.save(any(User.class))).thenThrow(failure);

        GenericException exception = assertThrows(GenericException.class, () -> userService.addUser(validCriteria()));

        assertAll(
                () -> assertEquals(500, exception.getStatus().getCode()),
                () -> assertEquals("Generic error", exception.getStatus().getMessage()),
                () -> assertNotNull(exception.getStatus().getTraceId()),
                () -> assertSame(failure, exception.getCause())
        );
    }

    private static CriteriaAddUser validCriteria() {
        CriteriaAddUser criteria = new CriteriaAddUser();
        criteria.setFirstName("Mario");
        criteria.setLastName("Rossi");
        criteria.setEmail("mario.rossi@example.com");
        criteria.setPhoneNumber("+393331234567");
        return criteria;
    }
}
