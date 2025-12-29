package co.hotwax.ns.util;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.google.i18n.phonenumbers.NumberParseException;
import org.moqui.context.ExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PhoneValidationUtil {

    private static final Logger logger = LoggerFactory.getLogger(PhoneValidationUtil.class);

    public static boolean isValidPhoneNumber(String countryCode, String areaCode, String contactNumber, ExecutionContext ec) {
        try {
            if (contactNumber == null || contactNumber.trim().isEmpty()) return false;

            String fullNumber = buildFullNumber(countryCode, areaCode, contactNumber);

            PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
            PhoneNumber parsedNumber = phoneUtil.parse(fullNumber, countryCode);

            return phoneUtil.isValidNumber(parsedNumber) || phoneUtil.isPossibleNumber(parsedNumber);

        } catch (NumberParseException e) {
            logger.error("Phone parse error: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Phone validation failed: {}", e.getMessage());
        }
        return false;
    }

    private static String buildFullNumber(String countryCode, String areaCode, String contactNumber) {
        StringBuilder sb = new StringBuilder();
        if (countryCode != null && !countryCode.isEmpty()) {
            String cleaned = countryCode.trim().replaceFirst("^\\+", "");
            sb.append("+").append(cleaned);
        }
        if (areaCode != null && !areaCode.isEmpty()) {
            sb.append(areaCode.trim());
        }
        sb.append(contactNumber.trim());
        return sb.toString();
    }
    public static String getFormattedPhoneNumber(String countryCode, String areaCode, String contactNumber) {
        try {
            if (contactNumber == null || contactNumber.trim().isEmpty()) return null;

            String fullNumber = buildFullNumber(countryCode, areaCode, contactNumber);
            PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
            PhoneNumber parsedNumber = phoneUtil.parse(fullNumber, countryCode);

            if (phoneUtil.isValidNumber(parsedNumber)) {
                return phoneUtil.format(parsedNumber, PhoneNumberUtil.PhoneNumberFormat.E164); // or INTERNATIONAL
            }

        } catch (NumberParseException e) {
            logger.error("Format parse error: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Formatting failed: {}", e.getMessage());
        }
        return null;
    }
}
