package it.sara.demo.web.user;

import it.sara.demo.exception.GenericException;
import it.sara.demo.service.user.UserService;
import it.sara.demo.service.user.criteria.CriteriaAddUser;
import it.sara.demo.service.user.criteria.CriteriaGetUsers;
import it.sara.demo.service.user.result.GetUsersResult;
import it.sara.demo.web.assembler.AddUserAssembler;
import it.sara.demo.web.assembler.GetUsersAssembler;
import it.sara.demo.web.response.GenericResponse;
import it.sara.demo.web.user.request.AddUserRequest;
import it.sara.demo.web.user.request.GetUsersRequest;
import it.sara.demo.web.user.response.AddUserResponse;
import it.sara.demo.web.user.response.GetUsersResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for users. Errors are turned into responses by
 * {@link it.sara.demo.web.handler.GlobalExceptionHandler}, always with HTTP status 200.
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    private final AddUserAssembler addUserAssembler;

    private final GetUsersAssembler getUsersAssembler;

    /**
     * Creates a new user.
     *
     * @param request the user data
     * @return a response with code 200 and the message "User added."
     * @throws GenericException if the data is invalid or the user cannot be saved
     */
    @RequestMapping(value = {"/v1/user"}, method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AddUserResponse> addUser(@RequestBody AddUserRequest request) throws GenericException {
        CriteriaAddUser criteria = addUserAssembler.toCriteria(request);
        userService.addUser(criteria);
        AddUserResponse returnValue = new AddUserResponse();
        returnValue.setStatus(GenericResponse.successStatus("User added."));
        return ResponseEntity.ok(returnValue);
    }

    /**
     * Searches users, with case-insensitive filtering, sorting and pagination.
     *
     * @param request the search parameters; the body and every field in it are optional
     * @return a response with the requested page of users and the total number of matches
     * @throws GenericException if a search parameter is invalid
     */
    @RequestMapping(value = {"/v1/user"}, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GetUsersResponse> getUsers(@RequestBody(required = false) GetUsersRequest request)
            throws GenericException {
        CriteriaGetUsers criteria = getUsersAssembler.toCriteria(request);
        GetUsersResult result = userService.getUsers(criteria);
        GetUsersResponse returnValue = new GetUsersResponse();
        returnValue.setStatus(GenericResponse.successStatus("Search completed."));
        returnValue.setUsers(result.getUsers());
        returnValue.setTotal(result.getTotal());
        return ResponseEntity.ok(returnValue);
    }
}
