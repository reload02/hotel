package validation;

import common.FatalDataException;
import domain.ReservationStatus;
import domain.Role;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SpecParsers {
    private static final Pattern DATE = Pattern.compile("^([0-9]{2}|[0-9]{4})([_\\-./])([0-9]{1,2})\\2([0-9]{1,2})$");
    private static final Pattern YEAR_MONTH = Pattern.compile("^([0-9]{2}|[0-9]{4})([_\\-./])([0-9]{1,2})$");
    private static final Pattern SYSTEM_YEAR_MONTH = Pattern.compile("^([0-9]{4})-([0-9]{2})$");
    private static final Pattern TIME = Pattern.compile("^([0-9]{1,2})([:/.-])([0-9]{1,2})(?:\\s?(AM|am|a|PM|pm|p))?$");
    private static final Pattern RATING = Pattern.compile("^[0-9]+(?:\\.[0-9])?$");
    private static final DateTimeFormatter DATE_OUT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_OUT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME_OUT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter SYSTEM_MONTH_OUT = DateTimeFormatter.ofPattern("yyyy-MM");

    private SpecParsers() {
    }

    public static String trimHorizontalWhitespace(String text) {
        int start = 0;
        int end = text.length();
        while (start < end && isHorizontalWhitespace(text.charAt(start))) {
            start++;
        }
        while (end > start && isHorizontalWhitespace(text.charAt(end - 1))) {
            end--;
        }
        return text.substring(start, end);
    }

    private static boolean isHorizontalWhitespace(char ch) {
        return Character.isWhitespace(ch) && ch != '\n' && ch != '\r';
    }

    public static LocalDate parseDate(String input, String context) {
        Matcher matcher = DATE.matcher(input);
        if (!matcher.matches()) {
            throw new FatalDataException(context + " 문법 오류");
        }
        int year = parseYear(matcher.group(1));
        int month = Integer.parseInt(matcher.group(3));
        int day = Integer.parseInt(matcher.group(4));
        try {
            return LocalDate.of(year, month, day);
        } catch (DateTimeException e) {
            throw new FatalDataException(context + " 의미 오류");
        }
    }

    public static YearMonthValue parseYearMonth(String input, String context) {
        Matcher matcher = YEAR_MONTH.matcher(input);
        if (!matcher.matches()) {
            throw new FatalDataException(context + " 문법 오류");
        }
        int year = parseYear(matcher.group(1));
        int month = Integer.parseInt(matcher.group(3));
        if (month < 1 || month > 12) {
            throw new FatalDataException(context + " 의미 오류");
        }
        return new YearMonthValue(year, month);
    }

    public static YearMonth parseSystemYearMonth(String input, String context) {
        Matcher matcher = SYSTEM_YEAR_MONTH.matcher(input);
        if (!matcher.matches()) {
            throw new FatalDataException(context + " 문법 오류");
        }
        try {
            return YearMonth.of(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
        } catch (DateTimeException e) {
            throw new FatalDataException(context + " 의미 오류");
        }
    }

    public static LocalTime parseTime(String input, String context) {
        Matcher matcher = TIME.matcher(input);
        if (!matcher.matches()) {
            throw new FatalDataException(context + " 문법 오류");
        }
        int hour = Integer.parseInt(matcher.group(1));
        int minute = Integer.parseInt(matcher.group(3));
        String ampm = matcher.group(4);
        if (ampm == null) {
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                throw new FatalDataException(context + " 의미 오류");
            }
            return LocalTime.of(hour, minute);
        }
        if (hour < 1 || hour > 12 || minute < 0 || minute > 59) {
            throw new FatalDataException(context + " 의미 오류");
        }
        String normalized = ampm.toLowerCase(Locale.ROOT);
        if ("am".equals(normalized) || "a".equals(normalized)) {
            return LocalTime.of(hour == 12 ? 0 : hour, minute);
        }
        return LocalTime.of(hour == 12 ? 12 : hour + 12, minute);
    }

    public static LocalDateTime parseDateTime(String input, String context) {
        String[] parts = input.split(" ", -1);
        if (parts.length != 2 || parts[0].isEmpty() || parts[1].isEmpty()) {
            throw new FatalDataException(context + " 문법 오류");
        }
        return LocalDateTime.of(parseDate(parts[0], context), parseTime(parts[1], context));
    }

    public static LocalDateTime parseFlexibleDateTime(String input, String context) {
        String normalized = trimHorizontalWhitespace(input);
        if (normalized.contains(" ")) {
            return parseDateTime(normalized, context);
        }
        return parseDate(normalized, context).atStartOfDay();
    }

    public static int parseRoomNumber(String input, String context) {
        SpecValidators.requireValidRoomNumberText(input, context);
        int value = Integer.parseInt(input);
        if (value < 1 || value >= 10_000) {
            throw new FatalDataException(context + " 의미 오류");
        }
        return value;
    }

    public static int parseCapacity(String input, String context) {
        SpecValidators.requireValidCapacityText(input, context);
        int value = Integer.parseInt(input);
        if (value < 1 || value >= 1_000) {
            throw new FatalDataException(context + " 의미 오류");
        }
        return value;
    }

    public static double parseRating(String input, String context) {
        if (!RATING.matcher(input).matches()) {
            throw new FatalDataException(context + " 문법 오류");
        }
        try {
            double value = Double.parseDouble(input);
            if (Double.isNaN(value) || Double.isInfinite(value) || value < 0.0 || value > 10.0) {
                throw new FatalDataException(context + " 의미 오류");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new FatalDataException(context + " 문법 오류");
        }
    }

    public static Role parseRole(String input, String context) {
        if ("host".equals(input) || "Host".equals(input) || "HOST".equals(input)) {
            return Role.HOST;
        }
        if ("guest".equals(input) || "Guest".equals(input) || "GUEST".equals(input)) {
            return Role.GUEST;
        }
        if ("admin".equals(input) || "Admin".equals(input) || "ADMIN".equals(input)) {
            return Role.ADMIN;
        }
        throw new FatalDataException(context + " 문법 오류");
    }

    public static ReservationStatus parseReservationStatus(String input, String context) {
        for (ReservationStatus status : ReservationStatus.values()) {
            if (status.code().equals(input)) {
                return status;
            }
        }
        throw new FatalDataException(context + " 문법 오류");
    }

    public static String formatDate(LocalDate value) {
        return DATE_OUT.format(value);
    }

    public static String formatTime(LocalTime value) {
        return TIME_OUT.format(value);
    }

    public static String formatDateTime(LocalDateTime value) {
        return DATE_TIME_OUT.format(value);
    }

    public static String formatSystemYearMonth(YearMonth value) {
        return SYSTEM_MONTH_OUT.format(value);
    }

    public static String formatRating(Double value) {
        if (value == null) {
            return "";
        }
        if (value == Math.rint(value)) {
            return Long.toString(Math.round(value));
        }
        return Double.toString(value);
    }

    private static int parseYear(String text) {
        int value = Integer.parseInt(text);
        return text.length() == 2 ? 2000 + value : value;
    }

    public static final class YearMonthValue {
        private final int year;
        private final int month;

        public YearMonthValue(int year, int month) {
            this.year = year;
            this.month = month;
        }

        public int year() {
            return year;
        }

        public int month() {
            return month;
        }
    }
}
