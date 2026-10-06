package it.sara.demo.web.assembler;

import it.sara.demo.exception.GenericException;
import it.sara.demo.service.user.criteria.CriteriaGetUsers;
import it.sara.demo.service.user.criteria.CriteriaGetUsers.OrderType;
import it.sara.demo.web.user.request.GetUsersRequest;
import org.springframework.stereotype.Component;

/**
 * Converts the web-layer {@link GetUsersRequest} into the service-layer {@link CriteriaGetUsers}.
 */
@Component
public class GetUsersAssembler {

    /**
     * Builds the criteria used by the service to search users. Missing values stay null,
     * so that the service applies its defaults. A missing body is treated like an empty one.
     *
     * @param getUsersRequest the incoming request body, or null if the request has no body
     * @return a new {@link CriteriaGetUsers} carrying the request fields
     * @throws GenericException with code 400 if {@code order} is not the name of an {@link OrderType} constant
     */
    public CriteriaGetUsers toCriteria(GetUsersRequest getUsersRequest) throws GenericException {
        CriteriaGetUsers returnValue = new CriteriaGetUsers();
        if (getUsersRequest == null) {
            return returnValue;
        }
        returnValue.setQuery(getUsersRequest.getQuery());
        returnValue.setOffset(getUsersRequest.getOffset());
        returnValue.setLimit(getUsersRequest.getLimit());
        returnValue.setOrder(toOrderType(getUsersRequest.getOrder()));
        return returnValue;
    }

    private OrderType toOrderType(String order) throws GenericException {
        if (order == null) {
            return null;
        }
        try {
            return OrderType.valueOf(order);
        } catch (IllegalArgumentException e) {
            throw new GenericException(400, "Invalid order");
        }
    }
}
