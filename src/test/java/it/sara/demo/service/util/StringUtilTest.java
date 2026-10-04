package it.sara.demo.service.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StringUtilTest {

    private final StringUtil stringUtil = new StringUtil();

    /**
     * Regression: whitespace-only strings used to be accepted as "not empty".
     */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\t", "\n", " \t\r\n "})
    void isNullOrBlank_withNullEmptyOrWhitespace_returnsTrue(String value) {
        assertTrue(stringUtil.isNullOrBlank(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "Mario", " a ", "\tMario\n"})
    void isNullOrBlank_withAtLeastOneNonWhitespaceCharacter_returnsFalse(String value) {
        assertFalse(stringUtil.isNullOrBlank(value));
    }
}
