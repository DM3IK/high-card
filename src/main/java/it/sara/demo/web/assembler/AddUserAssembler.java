package it.sara.demo.web.assembler;

import it.sara.demo.service.user.criteria.CriteriaAddUser;
import it.sara.demo.web.user.request.AddUserRequest;
import org.springframework.stereotype.Component;

/**
 * Converts the web-layer {@link AddUserRequest} into the service-layer {@link CriteriaAddUser},
 * so that the service never depends on web-exposed objects.
 */
@Component
public class AddUserAssembler {

    /**
     * Builds the criteria used by the service to create a new user.
     *
     * @param addUserRequest the incoming request body
     * @return a new {@link CriteriaAddUser} carrying the request fields unchanged
     */
    public CriteriaAddUser toCriteria(AddUserRequest addUserRequest) {
        CriteriaAddUser returnValue = new CriteriaAddUser();
        returnValue.setEmail(addUserRequest.getEmail());
        returnValue.setFirstName(addUserRequest.getFirstName());
        returnValue.setLastName(addUserRequest.getLastName());
        returnValue.setPhoneNumber(addUserRequest.getPhoneNumber());
        return returnValue;
    }
}
