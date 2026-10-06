package it.sara.demo.service.user.result;

import it.sara.demo.dto.UserDTO;
import it.sara.demo.service.result.GenericPagedResult;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Result of the user search: one page of users, plus the total number of matches inherited from
 * {@link GenericPagedResult}.
 */
@Getter
@Setter
public class GetUsersResult extends GenericPagedResult {

    /** The requested page of users, already filtered and sorted. */
    private List<UserDTO> users;
}
