package it.sara.demo.service.assembler;

import it.sara.demo.dto.UserDTO;
import it.sara.demo.service.database.model.User;
import org.springframework.stereotype.Component;

/**
 * Converts the persistence model {@link User} into the {@link UserDTO} returned to callers.
 */
@Component
public class UserAssembler {

    /**
     * Maps every field of a user to a new DTO.
     *
     * @param user the stored user
     * @return a new {@link UserDTO} with guid, first name, last name, email and phone number
     */
    public UserDTO toDTO(User user) {
        UserDTO returnValue = new UserDTO();
        returnValue.setGuid(user.getGuid());
        returnValue.setFirstName(user.getFirstName());
        returnValue.setLastName(user.getLastName());
        returnValue.setEmail(user.getEmail());
        returnValue.setPhoneNumber(user.getPhoneNumber());
        return returnValue;
    }
}
