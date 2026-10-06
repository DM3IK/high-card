package it.sara.demo.web.response;

import lombok.Getter;
import lombok.Setter;

/**
 * Base type of the responses that return one page of a longer list.
 */
@Getter
@Setter
public class GenericPagedResponse extends GenericResponse {

    /** Total number of items matching the request, regardless of the requested page. */
    private int total;
}
