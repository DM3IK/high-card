package it.sara.demo.service.database.model;

import lombok.Getter;
import lombok.Setter;

/**
 * User stored in the in-memory table of {@link it.sara.demo.service.database.FakeDatabase}.
 * Values are stored already validated and normalized: names and email trimmed, phone number as {@code +39}
 * followed by digits. It is never returned to clients directly: {@link it.sara.demo.service.assembler.UserAssembler}
 * converts it to a {@link it.sara.demo.dto.UserDTO}.
 */
@Getter
@Setter
public class User {

    /** Unique identifier, assigned by {@link it.sara.demo.service.database.UserRepository#save(User)}. */
    private String guid;

    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
}
