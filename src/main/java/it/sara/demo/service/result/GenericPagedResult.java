package it.sara.demo.service.result;

import lombok.Getter;
import lombok.Setter;

/**
 * Base result for paginated service operations.
 */
@Getter
@Setter
public class GenericPagedResult extends GenericResult {

    /** Total number of items matching the search, regardless of the requested page. */
    private int total;
}
