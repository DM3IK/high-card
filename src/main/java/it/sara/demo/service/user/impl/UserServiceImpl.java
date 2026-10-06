package it.sara.demo.service.user.impl;

import it.sara.demo.exception.GenericException;
import it.sara.demo.service.assembler.UserAssembler;
import it.sara.demo.service.database.UserRepository;
import it.sara.demo.service.database.model.User;
import it.sara.demo.service.user.UserService;
import it.sara.demo.service.user.criteria.CriteriaAddUser;
import it.sara.demo.service.user.criteria.CriteriaGetUsers;
import it.sara.demo.service.user.criteria.CriteriaGetUsers.OrderType;
import it.sara.demo.service.user.result.AddUserResult;
import it.sara.demo.service.user.result.GetUsersResult;
import it.sara.demo.service.user.validator.UserValidator;
import it.sara.demo.service.util.StringUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * User operations: creation with input validation, and search with filtering, sorting and pagination.
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 100;
    private static final int QUERY_MAX_LENGTH = 100;
    private static final OrderType DEFAULT_ORDER = OrderType.BY_LASTNAME;

    private final StringUtil stringUtil;

    private final UserValidator userValidator;

    private final UserRepository userRepository;

    private final UserAssembler userAssembler;

    /**
     * Validates the criteria and stores a new user, with names and email trimmed and the phone number
     * in the normalized {@code +39} format. Every field is checked against a whitelist, so input that could
     * be used for injection attacks is rejected before reaching storage.
     *
     * @param criteria the data of the user to create
     * @return the result of the operation
     * @throws GenericException with code 400 if a required field is null or blank,
     *                          if a name, the email or the phone number is not valid,
     *                          with code 500 and a specific message if the repository does not save the user,
     *                          or a {@link GenericException#genericError(Throwable) generic error}
     *                          that keeps the original cause for any unexpected failure
     */
    @Override
    public AddUserResult addUser(CriteriaAddUser criteria) throws GenericException {

        AddUserResult returnValue;
        User user;

        try {

            returnValue = new AddUserResult();

            if (stringUtil.isNullOrBlank(criteria.getFirstName())) {
                throw new GenericException(400, "First name is required");
            }
            if (stringUtil.isNullOrBlank(criteria.getLastName())) {
                throw new GenericException(400, "Last name is required");
            }
            if (stringUtil.isNullOrBlank(criteria.getEmail())) {
                throw new GenericException(400, "Email is required");
            }
            if (stringUtil.isNullOrBlank(criteria.getPhoneNumber())) {
                throw new GenericException(400, "Phone is required");
            }
            String firstName = userValidator.normalizeName(criteria.getFirstName())
                    .orElseThrow(() -> new GenericException(400, "Invalid first name"));
            String lastName = userValidator.normalizeName(criteria.getLastName())
                    .orElseThrow(() -> new GenericException(400, "Invalid last name"));
            String email = userValidator.normalizeEmail(criteria.getEmail())
                    .orElseThrow(() -> new GenericException(400, "Invalid email"));
            String phoneNumber = userValidator.normalizePhoneNumber(criteria.getPhoneNumber())
                    .orElseThrow(() -> new GenericException(400, "Invalid phone number"));

            user = new User();
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEmail(email);
            user.setPhoneNumber(phoneNumber);

            if (!userRepository.save(user)) {
                throw new GenericException(500, "Error saving user");
            }

        } catch (GenericException e) {
            throw e;
        } catch (Exception e) {
            throw GenericException.genericError(e);
        }
        return returnValue;
    }

    /**
     * Searches users: filters them with a case-insensitive {@code contains} on first name, last name and email,
     * sorts them and returns the requested page. Missing parameters use the defaults: offset 0, limit 10,
     * order {@link OrderType#BY_LASTNAME}. Users with the same sort key are ordered by the other name and then
     * by guid, so the order is stable across pages.
     *
     * @param criteriaGetUsers the search parameters
     * @return the requested page of users and the total number of matches
     * @throws GenericException with code 400 if the offset is negative, the limit is not between 1 and 100
     *                          or the query is longer than 100 characters,
     *                          or a {@link GenericException#genericError(Throwable) generic error}
     *                          that keeps the original cause for any unexpected failure
     */
    @Override
    public GetUsersResult getUsers(CriteriaGetUsers criteriaGetUsers) throws GenericException {

        GetUsersResult returnValue;

        try {

            int offset = criteriaGetUsers.getOffset() != null ? criteriaGetUsers.getOffset() : 0;
            int limit = criteriaGetUsers.getLimit() != null ? criteriaGetUsers.getLimit() : DEFAULT_LIMIT;
            String query = criteriaGetUsers.getQuery() != null ? criteriaGetUsers.getQuery().trim() : "";
            OrderType order = criteriaGetUsers.getOrder() != null ? criteriaGetUsers.getOrder() : DEFAULT_ORDER;

            if (offset < 0) {
                throw new GenericException(400, "Invalid offset");
            }
            if (limit < 1 || limit > MAX_LIMIT) {
                throw new GenericException(400, "Invalid limit");
            }
            if (query.length() > QUERY_MAX_LENGTH) {
                throw new GenericException(400, "Invalid query");
            }

            String needle = query.toLowerCase(Locale.ROOT);
            List<User> found = userRepository.getAll().stream()
                    .filter(user -> needle.isEmpty() || matches(user, needle))
                    .sorted(comparatorFor(order))
                    .toList();

            returnValue = new GetUsersResult();
            returnValue.setTotal(found.size());
            returnValue.setUsers(found.stream()
                    .skip(offset)
                    .limit(limit)
                    .map(userAssembler::toDTO)
                    .toList());

        } catch (GenericException e) {
            throw e;
        } catch (Exception e) {
            throw GenericException.genericError(e);
        }
        return returnValue;
    }

    private static boolean matches(User user, String needle) {
        return containsIgnoreCase(user.getFirstName(), needle)
                || containsIgnoreCase(user.getLastName(), needle)
                || containsIgnoreCase(user.getEmail(), needle);
    }

    private static boolean containsIgnoreCase(String value, String lowerCaseNeedle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(lowerCaseNeedle);
    }

    private static Comparator<User> comparatorFor(OrderType order) {
        Collator collator = Collator.getInstance(Locale.ITALIAN);
        Comparator<User> byFirstName = Comparator.comparing(User::getFirstName, collator);
        Comparator<User> byLastName = Comparator.comparing(User::getLastName, collator);
        Comparator<User> byGuid = Comparator.comparing(User::getGuid, Comparator.nullsLast(Comparator.naturalOrder()));
        return switch (order) {
            case BY_FIRSTNAME -> byFirstName.thenComparing(byLastName).thenComparing(byGuid);
            case BY_FIRSTNAME_DESC -> byFirstName.thenComparing(byLastName).thenComparing(byGuid).reversed();
            case BY_LASTNAME -> byLastName.thenComparing(byFirstName).thenComparing(byGuid);
            case BY_LASTNAME_DESC -> byLastName.thenComparing(byFirstName).thenComparing(byGuid).reversed();
        };
    }
}
