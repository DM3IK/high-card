package it.sara.demo.service.user.result;

import it.sara.demo.service.result.GenericResult;
import lombok.Getter;
import lombok.Setter;

/**
 * Result of the user creation. It carries no data: success means that no exception was thrown.
 */
@Getter
@Setter
public class AddUserResult extends GenericResult {
}
