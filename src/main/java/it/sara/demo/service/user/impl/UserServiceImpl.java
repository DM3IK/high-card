package it.sara.demo.service.user.impl;

import it.sara.demo.exception.GenericException;
import it.sara.demo.service.database.UserRepository;
import it.sara.demo.service.database.model.User;
import it.sara.demo.service.user.UserService;
import it.sara.demo.service.user.criteria.CriteriaAddUser;
import it.sara.demo.service.user.criteria.CriteriaGetUsers;
import it.sara.demo.service.user.result.AddUserResult;
import it.sara.demo.service.user.result.GetUsersResult;
import it.sara.demo.service.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final StringUtil stringUtil;

    private final UserRepository userRepository;

    /**
     * Validates the criteria and stores a new user.
     *
     * @param criteria the data of the user to create
     * @return the result of the operation
     * @throws GenericException with code 400 if a required field is null or blank,
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

            user = new User();
            user.setFirstName(criteria.getFirstName());
            user.setLastName(criteria.getLastName());
            user.setEmail(criteria.getEmail());
            user.setPhoneNumber(criteria.getPhoneNumber());

            if (!userRepository.save(user)) {
                throw new GenericException(500, "Error saving user");
            }

        } catch (GenericException e) {
            throw e;
        } catch (Exception e) {
            GenericException error = GenericException.genericError(e);
            log.error("Unexpected error [traceId={}]", error.getStatus().getTraceId(), e);
            throw error;
        }
        return returnValue;
    }

    @Override
    public GetUsersResult getUsers(CriteriaGetUsers criteriaGetUsers) throws GenericException {
        return null;
    }
}
