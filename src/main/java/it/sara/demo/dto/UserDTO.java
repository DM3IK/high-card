package it.sara.demo.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * User data returned to clients by the search endpoint, built from the stored user by
 * {@link it.sara.demo.service.assembler.UserAssembler}.
 */
@Getter
@Setter
public class UserDTO {

    /** Unique identifier assigned when the user was stored. */
    private String guid;

    private String firstName;
    private String lastName;
    private String email;

    /** Italian phone number in the stored format: {@code +39} followed by digits only. */
    private String phoneNumber;
}
