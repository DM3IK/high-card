package it.sara.demo.web.user.request;

import it.sara.demo.web.request.GenericRequest;
import lombok.Getter;
import lombok.Setter;

/**
 * Body of the user search request. Every field is optional.
 */
@Getter
@Setter
public class GetUsersRequest extends GenericRequest {

    /** Text searched case-insensitively in first name, last name and email; defaults to no filter. */
    private String query;

    /** Index of the first result to return; defaults to 0. */
    private Integer offset;

    /** Maximum number of results to return, from 1 to 100; defaults to 10. */
    private Integer limit;

    /** Name of an {@code OrderType} constant, for example {@code BY_FIRSTNAME}; defaults to {@code BY_LASTNAME}. */
    private String order;
}
