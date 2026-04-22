package sqlancer.mysql.ast;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

import sqlancer.IgnoreMeException;

/**
 * Utility class for MySQL temporal (date/time) operations.
 * Provides parsing, calculation, and extraction methods for date/time values.
 */
public final class MySQLTemporalUtil {

    /**
     * Represents a MySQL INTERVAL value with years, months, days, and time components.
     */
    public static final class IntervalValue {
        private final long years;
        private final long months;
        private final long days;
        private final long hours;
        private final long minutes;
        private final long seconds;
        private final long microseconds;

        IntervalValue(long years, long months, long days, long hours, long minutes, long seconds, long microseconds) {
            this.years = years;
            this.months = months;
            this.days = days;
            this.hours = hours;
            this.minutes = minutes;
            this.seconds = seconds;
            this.microseconds = microseconds;
        }

        long getYears() {
            return years;
        }

        long getMonths() {
            return months;
        }

        long getDays() {
            return days;
        }

        long getHours() {
            return hours;
        }

        long getMinutes() {
            return minutes;
        }

        long getSeconds() {
            return seconds;
        }

        long getMicroseconds() {
            return microseconds;
        }

        long getTotalMonths() {
            return years * 12 + months;
        }

        long getTotalDays() {
            return days;
        }

        long getTotalSeconds() {
            return hours * 3600 + minutes * 60 + seconds;
        }

        long getTotalMicroseconds() {
            return getTotalSeconds() * 1_000_000 + microseconds;
        }

        IntervalValue negate() {
            return new IntervalValue(-years, -months, -days, -hours, -minutes, -seconds, -microseconds);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (years != 0) {
                sb.append(years).append(" YEAR");
            }
            if (months != 0) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(months).append(" MONTH");
            }
            if (days != 0) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(days).append(" DAY");
            }
            if (hours != 0) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(hours).append(" HOUR");
            }
            if (minutes != 0) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(minutes).append(" MINUTE");
            }
            if (seconds != 0 || microseconds != 0) {
                if (sb.length() > 0) sb.append(" ");
                if (microseconds != 0) {
                    sb.append(seconds + microseconds / 1_000_000.0).append(" SECOND");
                } else {
                    sb.append(seconds).append(" SECOND");
                }
            }
            if (sb.length() == 0) {
                sb.append("0 SECOND");
            }
            return sb.toString();
        }
    }

    /**
     * Interval unit types supported by MySQL.
     */
    public enum IntervalUnit {
        YEAR, QUARTER, MONTH, DAY, HOUR, MINUTE, SECOND,
        YEAR_MONTH, DAY_HOUR, DAY_MINUTE, DAY_SECOND,
        HOUR_MINUTE, HOUR_SECOND, MINUTE_SECOND,
        DAY_MICROSECOND, HOUR_MICROSECOND, MINUTE_MICROSECOND,
        SECOND_MICROSECOND, MICROSECOND;

        static IntervalUnit fromString(String text) {
            switch (text.trim().toUpperCase()) {
            case "YEAR":
                return YEAR;
            case "QUARTER":
                return QUARTER;
            case "MONTH":
                return MONTH;
            case "DAY":
                return DAY;
            case "HOUR":
                return HOUR;
            case "MINUTE":
                return MINUTE;
            case "SECOND":
                return SECOND;
            case "YEAR_MONTH":
                return YEAR_MONTH;
            case "DAY_HOUR":
                return DAY_HOUR;
            case "DAY_MINUTE":
                return DAY_MINUTE;
            case "DAY_SECOND":
                return DAY_SECOND;
            case "HOUR_MINUTE":
                return HOUR_MINUTE;
            case "HOUR_SECOND":
                return HOUR_SECOND;
            case "MINUTE_SECOND":
                return MINUTE_SECOND;
            case "DAY_MICROSECOND":
                return DAY_MICROSECOND;
            case "HOUR_MICROSECOND":
                return HOUR_MICROSECOND;
            case "MINUTE_MICROSECOND":
                return MINUTE_MICROSECOND;
            case "SECOND_MICROSECOND":
                return SECOND_MICROSECOND;
            case "MICROSECOND":
                return MICROSECOND;
            default:
                throw new IgnoreMeException();
            }
        }
    }

    // Date formatter: YYYY-MM-DD
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Time formatter: HH:mm:ss[.SSSSSS]
    private static final DateTimeFormatter TIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("HH:mm:ss")
            .optionalStart()
            .appendFraction(ChronoField.MICRO_OF_SECOND, 0, 6, true)
            .optionalEnd()
            .toFormatter();

    // Datetime formatter: YYYY-MM-DD HH:mm:ss[.SSSSSS]
    private static final DateTimeFormatter DATETIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            .optionalStart()
            .appendFraction(ChronoField.MICRO_OF_SECOND, 0, 6, true)
            .optionalEnd()
            .toFormatter();

    private MySQLTemporalUtil() {
        // Utility class, prevent instantiation
    }

    // ==================== Parsing Methods ====================

    /**
     * Parse a date string in MySQL format (YYYY-MM-DD).
     */
    static LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IgnoreMeException();
        }
    }

    /**
     * Parse a time string in MySQL format (HH:mm:ss[.SSSSSS]).
     */
    static LocalTime parseTime(String value) {
        try {
            return LocalTime.parse(value.trim(), TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IgnoreMeException();
        }
    }

    /**
     * Parse a datetime string in MySQL format (YYYY-MM-DD HH:mm:ss[.SSSSSS]).
     */
    static LocalDateTime parseDateTime(String value) {
        try {
            return LocalDateTime.parse(value.trim(), DATETIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IgnoreMeException();
        }
    }

    /**
     * Parse a temporal value (date, datetime, or timestamp).
     */
    static LocalDateTime parseTemporal(String value) {
        String trimmed = value.trim();
        if (trimmed.contains(" ")) {
            return parseDateTime(trimmed);
        } else {
            return parseDate(trimmed).atStartOfDay();
        }
    }

    // ==================== Formatting Methods ====================

    /**
     * Format a LocalDate to MySQL date string.
     */
    static String formatDate(LocalDate date) {
        return date.format(DATE_FORMATTER);
    }

    /**
     * Format a LocalTime to MySQL time string (with microseconds if present).
     */
    static String formatTime(LocalTime time) {
        int micros = time.getNano() / 1000;
        if (micros != 0) {
            return String.format("%02d:%02d:%02d.%06d", time.getHour(), time.getMinute(), time.getSecond(), micros);
        }
        return time.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    /**
     * Format a LocalDateTime to MySQL datetime string.
     */
    static String formatDateTime(LocalDateTime dateTime) {
        int micros = dateTime.getNano() / 1000;
        if (micros != 0) {
            return String.format("%04d-%02d-%02d %02d:%02d:%02d.%06d",
                    dateTime.getYear(), dateTime.getMonthValue(), dateTime.getDayOfMonth(),
                    dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond(), micros);
        }
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    // ==================== Date Arithmetic Methods ====================

    /**
     * Add an interval to a date value.
     */
    static LocalDate addIntervalToDate(LocalDate date, IntervalValue interval) {
        LocalDate result = date;
        // Add years and months
        long totalMonths = interval.getTotalMonths();
        if (totalMonths != 0) {
            result = result.plusMonths(totalMonths);
        }
        // Add days
        if (interval.getDays() != 0) {
            result = result.plusDays(interval.getDays());
        }
        // Time components don't affect DATE type
        return result;
    }

    /**
     * Add an interval to a datetime value.
     */
    static LocalDateTime addIntervalToDateTime(LocalDateTime dateTime, IntervalValue interval) {
        LocalDateTime result = dateTime;
        // Add years and months
        long totalMonths = interval.getTotalMonths();
        if (totalMonths != 0) {
            result = result.plusMonths(totalMonths);
        }
        // Add days
        if (interval.getDays() != 0) {
            result = result.plusDays(interval.getDays());
        }
        // Add time components
        long totalMicros = interval.getTotalMicroseconds();
        if (totalMicros != 0) {
            result = result.plus(totalMicros, ChronoUnit.MICROS);
        }
        return result;
    }

    /**
     * Add an interval to a time value.
     */
    static LocalTime addIntervalToTime(LocalTime time, IntervalValue interval) {
        // Time can only add time components; date components cause overflow/underflow
        long totalMicros = interval.getTotalMicroseconds();
        if (totalMicros != 0) {
            return time.plus(totalMicros, ChronoUnit.MICROS);
        }
        return time;
    }

    /**
     * Subtract an interval from a date value.
     */
    static LocalDate subtractIntervalFromDate(LocalDate date, IntervalValue interval) {
        return addIntervalToDate(date, interval.negate());
    }

    /**
     * Subtract an interval from a datetime value.
     */
    static LocalDateTime subtractIntervalFromDateTime(LocalDateTime dateTime, IntervalValue interval) {
        return addIntervalToDateTime(dateTime, interval.negate());
    }

    /**
     * Subtract an interval from a time value.
     */
    static LocalTime subtractIntervalFromTime(LocalTime time, IntervalValue interval) {
        return addIntervalToTime(time, interval.negate());
    }

    // ==================== Date Difference Methods ====================

    /**
     * Calculate the number of days between two dates (date1 - date2).
     * Only the date parts are compared, regardless of time.
     */
    static long dateDiff(String date1, String date2) {
        LocalDate d1 = parseTemporal(date1).toLocalDate();
        LocalDate d2 = parseTemporal(date2).toLocalDate();
        return ChronoUnit.DAYS.between(d2, d1);  // d1 - d2
    }

    /**
     * Calculate the time difference between two time/datetime values (time1 - time2).
     * Returns a LocalTime representing the difference.
     */
    static LocalTime timeDiff(String time1, String time2) {
        LocalDateTime t1 = parseTemporal(time1);
        LocalDateTime t2 = parseTemporal(time2);
        long totalSeconds = ChronoUnit.SECONDS.between(t2, t1);
        // Handle negative differences
        boolean negative = totalSeconds < 0;
        totalSeconds = Math.abs(totalSeconds);
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        LocalTime result = LocalTime.of((int) hours, (int) minutes, (int) seconds);
        if (negative) {
            // MySQL represents negative time as "-HH:mm:ss"
            // We'll throw IgnoreMeException for negative time to avoid complexity
            throw new IgnoreMeException();
        }
        return result;
    }

    // ==================== Date Part Extraction Methods ====================

    /**
     * Extract year from a date/datetime value.
     */
    static int extractYear(String value) {
        return parseTemporal(value).getYear();
    }

    /**
     * Extract quarter (1-4) from a date/datetime value.
     */
    static int extractQuarter(String value) {
        int month = parseTemporal(value).getMonthValue();
        return (month - 1) / 3 + 1;
    }

    /**
     * Extract month (1-12) from a date/datetime value.
     */
    static int extractMonth(String value) {
        return parseTemporal(value).getMonthValue();
    }

    /**
     * Extract day of month (1-31) from a date/datetime value.
     */
    static int extractDay(String value) {
        return parseTemporal(value).getDayOfMonth();
    }

    /**
     * Extract day of week (1=Sunday, 7=Saturday) from a date/datetime value.
     * Note: MySQL's DAYOFWEEK returns 1 for Sunday, while Java's DayOfWeek returns 1 for Monday.
     */
    static int dayOfWeek(String value) {
        LocalDate date = parseTemporal(value).toLocalDate();
        // Java: Monday=1, Sunday=7; MySQL: Sunday=1, Saturday=7
        int javaDayOfWeek = date.getDayOfWeek().getValue();
        return (javaDayOfWeek % 7) + 1;
    }

    /**
     * Extract day of year (1-366) from a date/datetime value.
     */
    static int dayOfYear(String value) {
        return parseTemporal(value).getDayOfYear();
    }

    /**
     * Extract week of year from a date/datetime value.
     * MySQL WEEK() has multiple modes; mode 0 (default): Sunday is first day of week.
     */
    static int weekOfYear(String value, int mode) {
        LocalDate date = parseTemporal(value).toLocalDate();
        // Simplified implementation: use mode 0 and 1
        switch (mode) {
        case 0:
            // Week starts on Sunday, range 0-53
            return (date.getDayOfYear() - 1 + adjustToSunday(date)) / 7;
        case 1:
            // Week starts on Monday, range 0-53
            return (date.getDayOfYear() - 1 + adjustToMonday(date)) / 7;
        case 2:
            // Week starts on Sunday, range 1-53
            return ((date.getDayOfYear() - 1 + adjustToSunday(date)) / 7) + 1;
        case 3:
            // Week starts on Monday, range 1-53
            return ((date.getDayOfYear() - 1 + adjustToMonday(date)) / 7) + 1;
        default:
            // For other modes, throw IgnoreMeException for simplicity
            throw new IgnoreMeException();
        }
    }

    private static int adjustToSunday(LocalDate date) {
        // Days to subtract to get to the nearest previous Sunday
        int dayOfWeek = date.getDayOfWeek().getValue(); // Monday=1, Sunday=7
        return (dayOfWeek == 7) ? 0 : dayOfWeek;
    }

    private static int adjustToMonday(LocalDate date) {
        // Days to subtract to get to the nearest previous Monday
        int dayOfWeek = date.getDayOfWeek().getValue(); // Monday=1, Sunday=7
        return dayOfWeek - 1;
    }

    /**
     * Extract hour (0-23) from a time/datetime value.
     */
    static int extractHour(String value) {
        String trimmed = value.trim();
        if (trimmed.contains("-") && trimmed.contains(" ")) {
            // Looks like a datetime
            LocalDateTime dt = parseTemporal(trimmed);
            return dt.getHour();
        } else if (trimmed.contains(":")) {
            // Looks like a time
            LocalTime time = parseTime(trimmed);
            return time.getHour();
        } else {
            // Try as temporal anyway
            LocalDateTime dt = parseTemporal(trimmed);
            return dt.getHour();
        }
    }

    /**
     * Extract minute (0-59) from a time/datetime value.
     */
    static int extractMinute(String value) {
        String trimmed = value.trim();
        if (trimmed.contains("-") && trimmed.contains(" ")) {
            LocalDateTime dt = parseTemporal(trimmed);
            return dt.getMinute();
        } else if (trimmed.contains(":")) {
            LocalTime time = parseTime(trimmed);
            return time.getMinute();
        } else {
            LocalDateTime dt = parseTemporal(trimmed);
            return dt.getMinute();
        }
    }

    /**
     * Extract second (0-59) from a time/datetime value.
     */
    static int extractSecond(String value) {
        String trimmed = value.trim();
        if (trimmed.contains("-") && trimmed.contains(" ")) {
            LocalDateTime dt = parseTemporal(trimmed);
            return dt.getSecond();
        } else if (trimmed.contains(":")) {
            LocalTime time = parseTime(trimmed);
            return time.getSecond();
        } else {
            LocalDateTime dt = parseTemporal(trimmed);
            return dt.getSecond();
        }
    }

    /**
     * Extract microsecond (0-999999) from a time/datetime value.
     */
    static int extractMicrosecond(String value) {
        LocalDateTime dt = parseTemporal(value);
        return dt.getNano() / 1000;
    }

    // ==================== Interval Parsing ====================

    /**
     * Parse an interval expression with a unit.
     * Example: parseInterval(5, "DAY") returns IntervalValue with 5 days.
     */
    static IntervalValue parseInterval(long value, IntervalUnit unit) {
        switch (unit) {
        case YEAR:
            return new IntervalValue(value, 0, 0, 0, 0, 0, 0);
        case QUARTER:
            return new IntervalValue(0, value * 3, 0, 0, 0, 0, 0);
        case MONTH:
            return new IntervalValue(0, value, 0, 0, 0, 0, 0);
        case DAY:
            return new IntervalValue(0, 0, value, 0, 0, 0, 0);
        case HOUR:
            return new IntervalValue(0, 0, 0, value, 0, 0, 0);
        case MINUTE:
            return new IntervalValue(0, 0, 0, 0, value, 0, 0);
        case SECOND:
            return new IntervalValue(0, 0, 0, 0, 0, value, 0);
        case MICROSECOND:
            return new IntervalValue(0, 0, 0, 0, 0, 0, value);
        case YEAR_MONTH:
            // value should be interpreted as "YEARS-MONTHS" format (e.g., "1-2")
            // For simplicity, we'll use value as months only
            throw new IgnoreMeException();
        case DAY_HOUR:
            throw new IgnoreMeException();
        case DAY_MINUTE:
            throw new IgnoreMeException();
        case DAY_SECOND:
            throw new IgnoreMeException();
        case HOUR_MINUTE:
            throw new IgnoreMeException();
        case HOUR_SECOND:
            throw new IgnoreMeException();
        case MINUTE_SECOND:
            throw new IgnoreMeException();
        case DAY_MICROSECOND:
            throw new IgnoreMeException();
        case HOUR_MICROSECOND:
            throw new IgnoreMeException();
        case MINUTE_MICROSECOND:
            throw new IgnoreMeException();
        case SECOND_MICROSECOND:
            throw new IgnoreMeException();
        default:
            throw new IgnoreMeException();
        }
    }

    /**
     * Parse an interval from string representation.
     * Supports formats like "5 DAY", "3 MONTH", "1-2 YEAR_MONTH".
     */
    static IntervalValue parseIntervalFromString(String value, String unit) {
        IntervalUnit intervalUnit = IntervalUnit.fromString(unit);
        String trimmedValue = value.trim();

        // Handle compound units that have "-" separator in value
        if (intervalUnit == IntervalUnit.YEAR_MONTH ||
            intervalUnit == IntervalUnit.DAY_HOUR ||
            intervalUnit == IntervalUnit.DAY_MINUTE ||
            intervalUnit == IntervalUnit.DAY_SECOND ||
            intervalUnit == IntervalUnit.HOUR_MINUTE ||
            intervalUnit == IntervalUnit.HOUR_SECOND ||
            intervalUnit == IntervalUnit.MINUTE_SECOND) {
            // Compound units have format like "1-2" for YEAR_MONTH (1 year, 2 months)
            String[] parts = trimmedValue.split("-");
            if (parts.length == 2) {
                try {
                    long first = Long.parseLong(parts[0].trim());
                    long second = Long.parseLong(parts[1].trim());
                    switch (intervalUnit) {
                    case YEAR_MONTH:
                        return new IntervalValue(first, second, 0, 0, 0, 0, 0);
                    case DAY_HOUR:
                        return new IntervalValue(0, 0, first, second, 0, 0, 0);
                    case DAY_MINUTE:
                        // Format: DAYS HOURS_MINUTES -> needs further parsing
                        // Simplified: treat as days and minutes directly
                        throw new IgnoreMeException();
                    default:
                        throw new IgnoreMeException();
                    }
                } catch (NumberFormatException e) {
                    throw new IgnoreMeException();
                }
            }
        }

        // Simple units
        try {
            long numericValue = Long.parseLong(trimmedValue);
            return parseInterval(numericValue, intervalUnit);
        } catch (NumberFormatException e) {
            // Handle decimal seconds
            try {
                double decimalValue = Double.parseDouble(trimmedValue);
                if (intervalUnit == IntervalUnit.SECOND) {
                    long seconds = (long) decimalValue;
                    long micros = (long) ((decimalValue - seconds) * 1_000_000);
                    return new IntervalValue(0, 0, 0, 0, 0, seconds, micros);
                }
                throw new IgnoreMeException();
            } catch (NumberFormatException e2) {
                throw new IgnoreMeException();
            }
        }
    }

    // ==================== Date Format Methods ====================

    /**
     * Format a date/datetime according to MySQL DATE_FORMAT pattern.
     * Only supports a subset of format specifiers for testing purposes.
     */
    static String dateFormat(String value, String format) {
        LocalDateTime dt = parseTemporal(value);
        // Convert MySQL format to Java format (simplified subset)
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < format.length(); i++) {
            char c = format.charAt(i);
            if (c == '%') {
                if (i + 1 < format.length()) {
                    char specifier = format.charAt(i + 1);
                    i++;
                    switch (specifier) {
                    case 'Y': // Year, 4 digits
                        result.append(String.format("%04d", dt.getYear()));
                        break;
                    case 'y': // Year, 2 digits
                        result.append(String.format("%02d", dt.getYear() % 100));
                        break;
                    case 'm': // Month, 2 digits (01-12)
                        result.append(String.format("%02d", dt.getMonthValue()));
                        break;
                    case 'c': // Month, numeric (1-12)
                        result.append(dt.getMonthValue());
                        break;
                    case 'd': // Day of month, 2 digits (01-31)
                        result.append(String.format("%02d", dt.getDayOfMonth()));
                        break;
                    case 'e': // Day of month, numeric (1-31)
                        result.append(dt.getDayOfMonth());
                        break;
                    case 'H': // Hour, 2 digits (00-23)
                        result.append(String.format("%02d", dt.getHour()));
                        break;
                    case 'k': // Hour, numeric (0-23)
                        result.append(dt.getHour());
                        break;
                    case 'h': // Hour, 2 digits (01-12)
                        int h12 = dt.getHour() % 12;
                        if (h12 == 0) h12 = 12;
                        result.append(String.format("%02d", h12));
                        break;
                    case 'I': // Hour, 2 digits (01-12)
                        int h12I = dt.getHour() % 12;
                        if (h12I == 0) h12I = 12;
                        result.append(String.format("%02d", h12I));
                        break;
                    case 'i': // Minutes, 2 digits (00-59)
                        result.append(String.format("%02d", dt.getMinute()));
                        break;
                    case 's': // Seconds, 2 digits (00-59)
                        result.append(String.format("%02d", dt.getSecond()));
                        break;
                    case 'S': // Seconds, 2 digits (00-59)
                        result.append(String.format("%02d", dt.getSecond()));
                        break;
                    case 'p': // AM or PM
                        if (dt.getHour() < 12) {
                            result.append("AM");
                        } else {
                            result.append("PM");
                        }
                        break;
                    case 'W': // Weekday name (Sunday..Saturday)
                        result.append(dt.getDayOfWeek().toString());
                        break;
                    case 'a': // Abbreviated weekday name (Sun..Sat)
                        result.append(dt.getDayOfWeek().toString().substring(0, 3));
                        break;
                    case 'w': // Day of week (0=Sunday..6=Saturday)
                        int dowJava = dt.getDayOfWeek().getValue();
                        int dowMySQL = (dowJava % 7); // Convert: Monday=1 -> 1, Sunday=7 -> 0
                        result.append(dowMySQL);
                        break;
                    case 'j': // Day of year (001-366)
                        result.append(String.format("%03d", dt.getDayOfYear()));
                        break;
                    case 'U': // Week (00-53), Sunday is first day
                        result.append(String.format("%02d", weekOfYear(value, 0)));
                        break;
                    case 'u': // Week (00-53), Monday is first day
                        result.append(String.format("%02d", weekOfYear(value, 1)));
                        break;
                    case 'M': // Month name (January..December)
                        result.append(dt.getMonth().toString());
                        break;
                    case 'b': // Abbreviated month name (Jan..Dec)
                        result.append(dt.getMonth().toString().substring(0, 3));
                        break;
                    default:
                        // Unsupported specifier, skip
                        result.append('%').append(specifier);
                    }
                } else {
                    result.append('%');
                }
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * Get last day of month for a date.
     */
    static LocalDate lastDayOfMonth(String value) {
        LocalDate date = parseTemporal(value).toLocalDate();
        return date.with(TemporalAdjusters.lastDayOfMonth());
    }

    /**
     * Get first day of month for a date.
     */
    static LocalDate firstDayOfMonth(String value) {
        LocalDate date = parseTemporal(value).toLocalDate();
        return date.with(TemporalAdjusters.firstDayOfMonth());
    }

    /**
     * Get first day of year for a date.
     */
    static LocalDate firstDayOfYear(String value) {
        LocalDate date = parseTemporal(value).toLocalDate();
        return date.with(TemporalAdjusters.firstDayOfYear());
    }

    /**
     * Get last day of year for a date.
     */
    static LocalDate lastDayOfYear(String value) {
        LocalDate date = parseTemporal(value).toLocalDate();
        return date.with(TemporalAdjusters.lastDayOfYear());
    }

    /**
     * Make a date from year and day-of-year values.
     */
    static LocalDate makeDate(int year, int dayOfYear) {
        try {
            return LocalDate.ofYearDay(year, dayOfYear);
        } catch (DateTimeException e) {
            throw new IgnoreMeException();
        }
    }

    /**
     * Make a time from hour, minute, and second values.
     */
    static LocalTime makeTime(int hour, int minute, int second) {
        try {
            return LocalTime.of(hour, minute, second);
        } catch (DateTimeException e) {
            throw new IgnoreMeException();
        }
    }
}