package it.sara.demo.service.user.criteria;

import it.sara.demo.service.criteria.GenericCriteria;
import lombok.Getter;
import lombok.Setter;

/**
 * Data of a user to create, as received from the client. Validation and normalization happen in the service.
 */
@Getter
@Setter
public class CriteriaAddUser extends GenericCriteria {
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
}
