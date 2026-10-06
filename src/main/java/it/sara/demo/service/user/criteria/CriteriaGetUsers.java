package it.sara.demo.service.user.criteria;

import it.sara.demo.service.criteria.GenericCriteria;
import lombok.Getter;
import lombok.Setter;

/**
 * Search parameters for the user list. Null values mean "use the default", which the service decides.
 */
@Getter
@Setter
public class CriteriaGetUsers extends GenericCriteria {

    /** Text searched case-insensitively in first name, last name and email; null or blank means no filter. */
    private String query;

    /** Index of the first result to return. */
    private Integer offset;

    /** Maximum number of results to return. */
    private Integer limit;

    /** Sort order of the results. */
    private OrderType order;

    /**
     * Sort order of the user search results; each constant has a display name
     * describing the sorted field and the direction.
     */
    @Getter
    public enum OrderType {
        /** First name, A to Z. */
        BY_FIRSTNAME("by firstName"),
        /** First name, Z to A. */
        BY_FIRSTNAME_DESC("by firstName desc"),
        /** Last name, A to Z; the default order. */
        BY_LASTNAME("by lastName"),
        /** Last name, Z to A. */
        BY_LASTNAME_DESC("by lastName desc");
        private final String displayName;

        OrderType(String displayName) {
            this.displayName = displayName;
        }
    }

}
