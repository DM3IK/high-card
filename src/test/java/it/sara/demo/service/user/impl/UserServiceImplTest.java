package it.sara.demo.service.user.impl;

import it.sara.demo.dto.UserDTO;
import it.sara.demo.exception.GenericException;
import it.sara.demo.service.assembler.UserAssembler;
import it.sara.demo.service.database.UserRepository;
import it.sara.demo.service.database.model.User;
import it.sara.demo.service.user.criteria.CriteriaAddUser;
import it.sara.demo.service.user.criteria.CriteriaGetUsers;
import it.sara.demo.service.user.criteria.CriteriaGetUsers.OrderType;
import it.sara.demo.service.user.result.AddUserResult;
import it.sara.demo.service.user.result.GetUsersResult;
import it.sara.demo.service.user.validator.UserValidator;
import it.sara.demo.service.util.StringUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
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
        userService = new UserServiceImpl(new StringUtil(), new UserValidator(), userRepository, new UserAssembler());
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
    void addUser_withSpacesAroundNamesAndEmail_savesThemTrimmed() throws GenericException {
        when(userRepository.save(any(User.class))).thenReturn(true);
        CriteriaAddUser criteria = validCriteria();
        criteria.setFirstName(" Mario ");
        criteria.setLastName(" Rossi ");
        criteria.setEmail(" mario.rossi@example.com ");

        userService.addUser(criteria);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertAll(
                () -> assertEquals("Mario", saved.getValue().getFirstName()),
                () -> assertEquals("Rossi", saved.getValue().getLastName()),
                () -> assertEquals("mario.rossi@example.com", saved.getValue().getEmail())
        );
    }

    /**
     * Regression: names accepted any text, including SQL injection payloads.
     */
    @ParameterizedTest(name = "{1}")
    @MethodSource("invalidFormats")
    void addUser_withInvalidOrInjectionInput_throws400WithFieldMessage(Consumer<CriteriaAddUser> setInvalidValue,
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
                Arguments.of((Consumer<CriteriaAddUser>) c -> c.setFirstName("Robert'); DROP TABLE users;--"),
                        "Invalid first name"),
                Arguments.of((Consumer<CriteriaAddUser>) c -> c.setLastName("' OR '1'='1"), "Invalid last name"),
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

    @Test
    void getUsers_withNoParameters_returnsFirstTenUsersByLastNameAndTotal() throws GenericException {
        List<User> users = new ArrayList<>();
        for (char lastName = 'L'; lastName >= 'A'; lastName--) {
            users.add(user("g" + lastName, "Mario", String.valueOf(lastName), "mario@example.com"));
        }
        when(userRepository.getAll()).thenReturn(users);

        GetUsersResult result = userService.getUsers(new CriteriaGetUsers());

        assertAll(
                () -> assertEquals(12, result.getTotal()),
                () -> assertEquals(List.of("A", "B", "C", "D", "E", "F", "G", "H", "I", "J"),
                        result.getUsers().stream().map(UserDTO::getLastName).toList())
        );
    }

    @ParameterizedTest(name = "\"{0}\"")
    @CsvSource(delimiter = '|', value = {
            "ROSSI     | Anna Rossi;Mario Rossi",
            "mario     | Mario Rossi",
            "@OTHER.IT | Paolo Ávila;Giulia Bianchi",
            "' luca '  | Luca Romano",
            "zzz       | ''"
    })
    void getUsers_withQuery_filtersCaseInsensitivelyOnNamesAndEmail(String query, String expectedNames)
            throws GenericException {
        when(userRepository.getAll()).thenReturn(sampleUsers());
        CriteriaGetUsers criteria = new CriteriaGetUsers();
        criteria.setQuery(query);

        GetUsersResult result = userService.getUsers(criteria);

        List<String> expected = expectedNames.isEmpty() ? List.of() : List.of(expectedNames.split(";"));
        assertAll(
                () -> assertEquals(expected, fullNames(result)),
                () -> assertEquals(expected.size(), result.getTotal())
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void getUsers_withNullOrBlankQuery_returnsEveryUser(String query) throws GenericException {
        when(userRepository.getAll()).thenReturn(sampleUsers());
        CriteriaGetUsers criteria = new CriteriaGetUsers();
        criteria.setQuery(query);

        assertEquals(5, userService.getUsers(criteria).getTotal());
    }

    /**
     * Covers the tie-break (the two "Rossi" are ordered by first name) and the Italian collation
     * ("Ávila" sorts before "Bianchi", while plain string comparison would put it last).
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("sortOrders")
    void getUsers_withOrder_sortsByTheRequestedFieldAndDirection(OrderType order, List<String> expectedNames)
            throws GenericException {
        when(userRepository.getAll()).thenReturn(sampleUsers());
        CriteriaGetUsers criteria = new CriteriaGetUsers();
        criteria.setOrder(order);

        assertEquals(expectedNames, fullNames(userService.getUsers(criteria)));
    }

    static Stream<Arguments> sortOrders() {
        return Stream.of(
                Arguments.of(OrderType.BY_LASTNAME,
                        List.of("Paolo Ávila", "Giulia Bianchi", "Luca Romano", "Anna Rossi", "Mario Rossi")),
                Arguments.of(OrderType.BY_LASTNAME_DESC,
                        List.of("Mario Rossi", "Anna Rossi", "Luca Romano", "Giulia Bianchi", "Paolo Ávila")),
                Arguments.of(OrderType.BY_FIRSTNAME,
                        List.of("Anna Rossi", "Giulia Bianchi", "Luca Romano", "Mario Rossi", "Paolo Ávila")),
                Arguments.of(OrderType.BY_FIRSTNAME_DESC,
                        List.of("Paolo Ávila", "Mario Rossi", "Luca Romano", "Giulia Bianchi", "Anna Rossi"))
        );
    }

    @ParameterizedTest(name = "offset={0}, limit={1}")
    @CsvSource(delimiter = '|', value = {
            "0 | 2   | Anna Rossi;Giulia Bianchi",
            "1 | 2   | Giulia Bianchi;Luca Romano",
            "4 | 100 | Paolo Ávila",
            "5 | 1   | ''",
            "0 | 1   | Anna Rossi"
    })
    void getUsers_withOffsetAndLimit_returnsTheRequestedPageAndTheFullTotal(int offset, int limit,
                                                                            String expectedNames)
            throws GenericException {
        when(userRepository.getAll()).thenReturn(sampleUsers());
        CriteriaGetUsers criteria = new CriteriaGetUsers();
        criteria.setOffset(offset);
        criteria.setLimit(limit);
        criteria.setOrder(OrderType.BY_FIRSTNAME);

        GetUsersResult result = userService.getUsers(criteria);

        List<String> expected = expectedNames.isEmpty() ? List.of() : List.of(expectedNames.split(";"));
        assertAll(
                () -> assertEquals(expected, fullNames(result)),
                () -> assertEquals(5, result.getTotal())
        );
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("invalidSearchParameters")
    void getUsers_withInvalidParameter_throws400WithParameterMessage(Consumer<CriteriaGetUsers> setInvalidValue,
                                                                      String expectedMessage) {
        CriteriaGetUsers criteria = new CriteriaGetUsers();
        setInvalidValue.accept(criteria);

        GenericException exception = assertThrows(GenericException.class, () -> userService.getUsers(criteria));

        assertAll(
                () -> assertEquals(400, exception.getStatus().getCode()),
                () -> assertEquals(expectedMessage, exception.getStatus().getMessage())
        );
        verify(userRepository, never()).getAll();
    }

    static Stream<Arguments> invalidSearchParameters() {
        return Stream.of(
                Arguments.of((Consumer<CriteriaGetUsers>) c -> c.setOffset(-1), "Invalid offset"),
                Arguments.of((Consumer<CriteriaGetUsers>) c -> c.setLimit(0), "Invalid limit"),
                Arguments.of((Consumer<CriteriaGetUsers>) c -> c.setLimit(101), "Invalid limit"),
                Arguments.of((Consumer<CriteriaGetUsers>) c -> c.setQuery("a".repeat(101)), "Invalid query")
        );
    }

    @Test
    void getUsers_withParametersAtTheirLimits_isAccepted() throws GenericException {
        when(userRepository.getAll()).thenReturn(sampleUsers());
        CriteriaGetUsers criteria = new CriteriaGetUsers();
        criteria.setOffset(0);
        criteria.setLimit(100);
        criteria.setQuery("a".repeat(100));

        assertEquals(0, userService.getUsers(criteria).getTotal());
    }

    private static List<User> sampleUsers() {
        return List.of(
                user("g1", "Mario", "Rossi", "mario.rossi@example.com"),
                user("g2", "Giulia", "Bianchi", "giulia@other.it"),
                user("g3", "Luca", "Romano", "luca.romano@example.com"),
                user("g4", "Anna", "Rossi", "anna.r@example.com"),
                user("g5", "Paolo", "Ávila", "paolo@other.it")
        );
    }

    private static User user(String guid, String firstName, String lastName, String email) {
        User user = new User();
        user.setGuid(guid);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPhoneNumber("+393331234567");
        return user;
    }

    private static List<String> fullNames(GetUsersResult result) {
        return result.getUsers().stream()
                .map(dto -> dto.getFirstName() + " " + dto.getLastName())
                .toList();
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
