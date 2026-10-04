package it.sara.demo.service.util;

import org.springframework.stereotype.Component;

/**
 * String helpers shared by the service layer.
 */
@Component
public class StringUtil {

    /**
     * Checks whether a string has no meaningful content.
     *
     * @param str the string to check
     * @return {@code true} if the string is null, empty or contains only whitespace
     */
    public boolean isNullOrBlank(String str) {
        return str == null || str.isBlank();
    }
}
