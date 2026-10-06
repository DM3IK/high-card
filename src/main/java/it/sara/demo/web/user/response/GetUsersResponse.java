package it.sara.demo.web.user.response;

import it.sara.demo.dto.UserDTO;
import it.sara.demo.web.response.GenericPagedResponse;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Response of the user search endpoint: the outcome, one page of users and the total number of matches.
 */
@Getter
@Setter
public class GetUsersResponse extends GenericPagedResponse {

    /** The requested page of users, already filtered and sorted. */
    private List<UserDTO> users;
}
