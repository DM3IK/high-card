package it.sara.demo.service.user.validator;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberType;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Format rules for user data: names, email addresses and Italian phone numbers.
 * The rules are whitelists, so characters used in injection attacks (quotes in unexpected positions,
 * {@code ;}, {@code --}, {@code /*}, {@code =}, brackets) are rejected before the data reaches storage.
 */
@Component
public class UserValidator {

    private static final int NAME_MAX_LENGTH = 50;

    /**
     * Words made of letters of any alphabet, accented letters included, joined by a single space,
     * hyphen or apostrophe (e.g. "De Luca", "Anna-Maria", "D'Angelo").
     */
    private static final Pattern NAME = Pattern.compile("^\\p{L}+(?:[ '-]\\p{L}+)*$");

    private static final int EMAIL_MAX_LENGTH = 254;
    private static final int EMAIL_LOCAL_PART_MAX_LENGTH = 64;

    /**
     * Local part: letters, digits and {@code _ % + -}, in dot-separated groups.
     * Domain: labels of letters, digits and hyphens (not at the edges), ending with a top-level domain
     * of at least two letters.
     */
    private static final Pattern EMAIL = Pattern.compile(
            "^[A-Za-z0-9_%+-]+(\\.[A-Za-z0-9_%+-]+)*"
                    + "@(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\.)+[A-Za-z]{2,63}$");

    /** Optional leading {@code +}, then digit groups separated by a single space or hyphen. */
    private static final Pattern PHONE_FORMAT = Pattern.compile("^\\+?\\d+([ -]\\d+)*$");

    /**
     * Google libphonenumber metadata, used to check that a number exists in the Italian numbering plan:
     * valid mobile prefixes, area codes and lengths.
     */
    private static final PhoneNumberUtil PHONE_NUMBER_UTIL = PhoneNumberUtil.getInstance();

    private static final String ITALIAN_REGION = "IT";

    /**
     * Number types a user can be reached on. Service numbers that exist in the numbering plan
     * (toll-free, premium rate, voicemail access) are rejected.
     */
    private static final Set<PhoneNumberType> ALLOWED_NUMBER_TYPES =
            EnumSet.of(PhoneNumberType.FIXED_LINE, PhoneNumberType.MOBILE, PhoneNumberType.FIXED_LINE_OR_MOBILE);

    private static final String ITALIAN_PREFIX = "+39";
    private static final String ITALIAN_PREFIX_WITH_ZEROS = "0039";

    /**
     * Removes leading and trailing spaces from a first or last name, then checks it against the name whitelist
     * and the maximum length of 50 characters. Accented letters are converted to their composed Unicode form,
     * so the same name is always stored the same way.
     *
     * @param name the name to check
     * @return the trimmed name, or an empty optional if the name is not valid
     */
    public Optional<String> normalizeName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        String trimmed = Normalizer.normalize(name.trim(), Normalizer.Form.NFC);
        if (trimmed.length() > NAME_MAX_LENGTH || !NAME.matcher(trimmed).matches()) {
            return Optional.empty();
        }
        return Optional.of(trimmed);
    }

    /**
     * Removes leading and trailing spaces from an email address, then checks that it is well formed
     * and within the standard length limits (64 characters for the local part, 254 in total).
     * Spaces inside the address are not allowed.
     *
     * @param email the email address to check
     * @return the trimmed email, or an empty optional if the email is not valid
     */
    public Optional<String> normalizeEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        String trimmed = email.trim();
        if (trimmed.length() > EMAIL_MAX_LENGTH) {
            return Optional.empty();
        }
        int at = trimmed.indexOf('@');
        if (at <= 0 || at > EMAIL_LOCAL_PART_MAX_LENGTH || !EMAIL.matcher(trimmed).matches()) {
            return Optional.empty();
        }
        return Optional.of(trimmed);
    }

    /**
     * Removes leading and trailing spaces from an Italian phone number, validates it and converts it to the stored
     * format: {@code +39} followed by digits only. The international prefix ({@code +39} or {@code 0039}) is
     * optional, and digit groups can be separated by single spaces or hyphens. The number must be a landline or
     * mobile number that exists in the Italian numbering plan.
     *
     * @param phoneNumber the phone number to check
     * @return the normalized number, or an empty optional if the number is not a valid Italian number
     */
    public Optional<String> normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return Optional.empty();
        }
        String trimmed = phoneNumber.trim();
        if (!PHONE_FORMAT.matcher(trimmed).matches()) {
            return Optional.empty();
        }
        String digits = trimmed.replaceAll("[ -]", "");
        String nationalNumber;
        if (digits.startsWith(ITALIAN_PREFIX)) {
            nationalNumber = digits.substring(ITALIAN_PREFIX.length());
        } else if (digits.startsWith(ITALIAN_PREFIX_WITH_ZEROS)) {
            nationalNumber = digits.substring(ITALIAN_PREFIX_WITH_ZEROS.length());
        } else if (digits.startsWith("+")) {
            return Optional.empty();
        } else {
            nationalNumber = digits;
        }
        String internationalNumber = ITALIAN_PREFIX + nationalNumber;
        if (!isInItalianNumberingPlan(internationalNumber)) {
            return Optional.empty();
        }
        return Optional.of(internationalNumber);
    }

    private static boolean isInItalianNumberingPlan(String internationalNumber) {
        try {
            PhoneNumber parsed = PHONE_NUMBER_UTIL.parse(internationalNumber, ITALIAN_REGION);
            return PHONE_NUMBER_UTIL.isValidNumberForRegion(parsed, ITALIAN_REGION)
                    && ALLOWED_NUMBER_TYPES.contains(PHONE_NUMBER_UTIL.getNumberType(parsed));
        } catch (NumberParseException e) {
            return false;
        }
    }
}
