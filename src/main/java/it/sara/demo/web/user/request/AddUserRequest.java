package it.sara.demo.web.user.request;

import it.sara.demo.web.request.GenericRequest;
import lombok.Getter;
import lombok.Setter;

/**
 * Body of the request that creates a user ({@code PUT /user/v1/user}). Every field is required.
 * The service validates the names, the email and the phone number, which must be a valid Italian number.
 */
@Getter
@Setter
public class AddUserRequest extends GenericRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
}
