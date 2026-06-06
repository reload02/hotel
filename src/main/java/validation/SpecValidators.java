package validation;

import common.FatalDataException;

import java.util.regex.Pattern;

public final class SpecValidators {
    private static final Pattern USER_NAME = Pattern.compile("^([가-힣]([가-힣 ]{0,18}[가-힣])?|[a-zA-Z]([a-zA-Z ]{0,18}[a-zA-Z])?)$");
    private static final Pattern HOTEL_NAME = Pattern.compile("^[a-zA-Z0-9가-힣!\"#$%&'()*+,\\-./:<=>?@\\[\\\\\\]^_`{|}~]([a-zA-Z0-9가-힣!\"#$%&'()*+,\\-./:<=>?@\\[\\\\\\]^_`{|}~ ]{0,38}[a-zA-Z0-9가-힣!\"#$%&'()*+,\\-./:<=>?@\\[\\\\\\]^_`{|}~])?$"
    );
    private static final Pattern ID = Pattern.compile("^(?!.*;)[a-zA-Z0-9!\"#$%&'()*+,\\-./:<=>?@\\[\\\\\\]^_`{|}~]{3,10}$");
    private static final Pattern PASSWORD = Pattern.compile("^(?!.*;)(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!\"#$%&'()*+,\\-./:<=>?@\\[\\\\\\]^_`{|}~])[a-zA-Z0-9!\"#$%&'()*+,\\-./:<=>?@\\[\\\\\\]^_`{|}~]{8,20}$");
    private static final Pattern POSTAL_CODE = Pattern.compile("^[0-9]{5}$");
    private static final Pattern ROOM_NUMBER = Pattern.compile("^[0-9]{1,5}$");
    private static final Pattern CAPACITY = Pattern.compile("^[0-9]{1,4}$");

    private SpecValidators() {
    }

    public static boolean isValidUserName(String input) {
        return USER_NAME.matcher(input).matches();
    }

    public static boolean isValidHotelName(String input) {
        return HOTEL_NAME.matcher(input).matches();
    }

    public static boolean isValidId(String input) {
        return ID.matcher(input).matches();
    }

    public static boolean isValidPassword(String input) {
        return PASSWORD.matcher(input).matches();
    }

    public static boolean isValidPostalCode(String input) {
        return POSTAL_CODE.matcher(input).matches();
    }

    public static boolean isValidRoomNumberText(String input) {
        return ROOM_NUMBER.matcher(input).matches();
    }

    public static boolean isValidCapacityText(String input) {
        return CAPACITY.matcher(input).matches();
    }

    public static void requireValidUserName(String input, String context) {
        if (!isValidUserName(input)) {
            throw new FatalDataException(context + " 문법 오류");
        }
    }

    public static void requireValidHotelName(String input, String context) {
        if (!isValidHotelName(input)) {
            throw new FatalDataException(context + " 문법 오류");
        }
    }

    public static void requireValidId(String input, String context) {
        if (!isValidId(input)) {
            throw new FatalDataException(context + " 문법 오류");
        }
    }

    public static void requireValidPassword(String input, String context) {
        if (!isValidPassword(input)) {
            throw new FatalDataException(context + " 문법 또는 의미 오류");
        }
    }

    public static void requireValidPostalCode(String input, String context) {
        if (!isValidPostalCode(input)) {
            throw new FatalDataException(context + " 문법 오류");
        }
    }

    public static void requireValidRoomNumberText(String input, String context) {
        if (!isValidRoomNumberText(input)) {
            throw new FatalDataException(context + " 문법 오류");
        }
    }

    public static void requireValidCapacityText(String input, String context) {
        if (!isValidCapacityText(input)) {
            throw new FatalDataException(context + " 문법 오류");
        }
    }
}
