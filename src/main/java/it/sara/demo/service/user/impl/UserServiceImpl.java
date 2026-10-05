package it.sara.demo.service.user.impl;

import it.sara.demo.exception.GenericException;
import it.sara.demo.service.database.UserRepository;
import it.sara.demo.service.database.model.User;
import it.sara.demo.service.user.UserService;
import it.sara.demo.service.user.criteria.CriteriaAddUser;
import it.sara.demo.service.user.criteria.CriteriaGetUsers;
import it.sara.demo.service.user.result.AddUserResult;
import it.sara.demo.service.user.result.GetUsersResult;
import it.sara.demo.service.user.validator.UserValidator;
import it.sara.demo.service.util.StringUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final StringUtil stringUtil;

    private final UserValidator userValidator;

    private final UserRepository userRepository;

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

    @Override
    public GetUsersResult getUsers(CriteriaGetUsers criteriaGetUsers) throws GenericException {
        return null;
    }
}
