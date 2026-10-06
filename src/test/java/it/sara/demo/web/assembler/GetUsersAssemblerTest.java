package it.sara.demo.web.assembler;

import it.sara.demo.exception.GenericException;
import it.sara.demo.service.user.criteria.CriteriaGetUsers;
import it.sara.demo.service.user.criteria.CriteriaGetUsers.OrderType;
import it.sara.demo.web.user.request.GetUsersRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetUsersAssemblerTest {

    private final GetUsersAssembler assembler = new GetUsersAssembler();

    @Test
    void toCriteria_mapsEveryFieldToTheMatchingCriteriaField() throws GenericException {
        GetUsersRequest request = new GetUsersRequest();
        request.setQuery("rossi");
        request.setOffset(20);
        request.setLimit(5);
        request.setOrder("BY_FIRSTNAME_DESC");

        CriteriaGetUsers criteria = assembler.toCriteria(request);

        assertAll(
                () -> assertEquals("rossi", criteria.getQuery()),
                () -> assertEquals(20, criteria.getOffset()),
                () -> assertEquals(5, criteria.getLimit()),
                () -> assertEquals(OrderType.BY_FIRSTNAME_DESC, criteria.getOrder())
        );
    }

    /**
     * Missing values must stay null, so that the service, not the web layer, decides the defaults.
     */
    @Test
    void toCriteria_withEmptyRequest_leavesEveryFieldNull() throws GenericException {
        CriteriaGetUsers criteria = assembler.toCriteria(new GetUsersRequest());

        assertAll(
                () -> assertNull(criteria.getQuery()),
                () -> assertNull(criteria.getOffset()),
                () -> assertNull(criteria.getLimit()),
                () -> assertNull(criteria.getOrder())
        );
    }

    @Test
    void toCriteria_withoutBody_behavesLikeEmptyRequest() throws GenericException {
        CriteriaGetUsers criteria = assembler.toCriteria(null);

        assertAll(
                () -> assertNull(criteria.getQuery()),
                () -> assertNull(criteria.getOffset()),
                () -> assertNull(criteria.getLimit()),
                () -> assertNull(criteria.getOrder())
        );
    }

    @ParameterizedTest
    @EnumSource(OrderType.class)
    void toCriteria_withOrderTypeName_mapsItToTheEnumConstant(OrderType orderType) throws GenericException {
        GetUsersRequest request = new GetUsersRequest();
        request.setOrder(orderType.name());

        assertEquals(orderType, assembler.toCriteria(request).getOrder());
    }

    @ParameterizedTest
    @ValueSource(strings = {"BY_AGE", "", "by_lastname", " BY_LASTNAME", "by lastName"})
    void toCriteria_withUnknownOrder_throws400InvalidOrder(String order) {
        GetUsersRequest request = new GetUsersRequest();
        request.setOrder(order);

        GenericException exception = assertThrows(GenericException.class, () -> assembler.toCriteria(request));

        assertAll(
                () -> assertEquals(400, exception.getStatus().getCode()),
                () -> assertEquals("Invalid order", exception.getStatus().getMessage())
        );
    }
}
