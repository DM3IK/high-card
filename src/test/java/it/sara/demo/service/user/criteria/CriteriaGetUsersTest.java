package it.sara.demo.service.user.criteria;

import it.sara.demo.service.user.criteria.CriteriaGetUsers.OrderType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CriteriaGetUsersTest {

    /**
     * Regression: {@code BY_LASTNAME_DESC} had the ascending label "by lastName".
     */
    @ParameterizedTest
    @CsvSource({
            "BY_FIRSTNAME,      by firstName",
            "BY_FIRSTNAME_DESC, by firstName desc",
            "BY_LASTNAME,       by lastName",
            "BY_LASTNAME_DESC,  by lastName desc"
    })
    void orderType_hasLabelMatchingItsSortDirection(OrderType orderType, String expectedLabel) {
        assertEquals(expectedLabel, orderType.getDisplayName());
    }
}
