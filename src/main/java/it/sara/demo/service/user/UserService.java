package it.sara.demo.service.user;

import it.sara.demo.exception.GenericException;
import it.sara.demo.service.user.criteria.CriteriaAddUser;
import it.sara.demo.service.user.criteria.CriteriaGetUsers;
import it.sara.demo.service.user.result.AddUserResult;
import it.sara.demo.service.user.result.GetUsersResult;

/**
 * User operations of the service layer. It works only with service-layer types (criteria and results),
 * never with web requests or responses.
 */
public interface UserService {

    /**
     * Validates and stores a new user.
     *
     * @param criteria the data of the user to create
     * @return the result of the operation
     * @throws GenericException with code 400 if the data is invalid, or 500 if the user cannot be stored
     */
    AddUserResult addUser(CriteriaAddUser criteria) throws GenericException;

    /**
     * Searches users with filtering, sorting and pagination.
     *
     * @param criteriaGetUsers the search parameters; null values use the defaults
     * @return the requested page of users and the total number of matches
     * @throws GenericException with code 400 if a parameter is invalid, or 500 for an unexpected failure
     */
    GetUsersResult getUsers(CriteriaGetUsers criteriaGetUsers) throws GenericException;
}
