package sqlancer.mysql.ast;

import java.math.BigInteger;

import sqlancer.IgnoreMeException;
import sqlancer.Randomly;
import sqlancer.mysql.MySQLSchema.MySQLDataType;
import sqlancer.mysql.ast.MySQLCastOperation.CastType;

public abstract class MySQLConstant implements MySQLExpression {

    public boolean isInt() {
        return false;
    }

    public boolean isNull() {
        return false;
    }

    public abstract static class MySQLNoPQSConstant extends MySQLConstant {

        @Override
        public boolean asBooleanNotNull() {
            throw throwException();
        }

        private RuntimeException throwException() {
            throw new UnsupportedOperationException("not applicable for PQS evaluation!");
        }

        @Override
        public MySQLConstant isEquals(MySQLConstant rightVal) {
            return null;
        }

        @Override
        public MySQLConstant castAs(CastType type) {
            throw throwException();
        }

        @Override
        public String castAsString() {
            throw throwException();

        }

        @Override
        public MySQLDataType getType() {
            throw throwException();
        }

        @Override
        protected MySQLConstant isLessThan(MySQLConstant rightVal) {
            throw throwException();
        }

    }

    public static class MySQLDoubleConstant extends MySQLNoPQSConstant {

        private final double val;

        public MySQLDoubleConstant(double val) {
            this.val = val;
            if (Double.isInfinite(val) || Double.isNaN(val)) {
                // seems to not be supported by MySQL
                throw new IgnoreMeException();
            }
        }

        @Override
        public String getTextRepresentation() {
            return String.valueOf(val);
        }

    }

    public static class MySQLTextConstant extends MySQLConstant {

        private final String value;
        private final boolean singleQuotes;

        public MySQLTextConstant(String value) {
            this.value = value;
            singleQuotes = Randomly.getBoolean();

        }

        private void checkIfSmallFloatingPointText() {
            boolean isSmallFloatingPointText = isString() && asBooleanNotNull()
                    && castAs(CastType.SIGNED).getInt() == 0;
            if (isSmallFloatingPointText) {
                throw new IgnoreMeException();
            }
        }

        @Override
        public boolean asBooleanNotNull() {
            // TODO implement as cast
            for (int i = value.length(); i >= 0; i--) {
                try {
                    String substring = value.substring(0, i);
                    Double val = Double.valueOf(substring);
                    return val != 0 && !Double.isNaN(val);
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
            return false;
            // return castAs(CastType.SIGNED).getInt() != 0;
        }

        @Override
        public String getTextRepresentation() {
            StringBuilder sb = new StringBuilder();
            String quotes = singleQuotes ? "'" : "\"";
            sb.append(quotes);
            String text = value.replace(quotes, quotes + quotes).replace("\\", "\\\\");
            sb.append(text);
            sb.append(quotes);
            return sb.toString();
        }

        @Override
        public MySQLConstant isEquals(MySQLConstant rightVal) {
            if (rightVal.isNull()) {
                return MySQLConstant.createNullConstant();
            } else if (rightVal.isInt()) {
                checkIfSmallFloatingPointText();
                if (asBooleanNotNull()) {
                    // TODO support SELECT .123 = '.123'; by converting to floating point
                    throw new IgnoreMeException();
                }
                return castAs(CastType.SIGNED).isEquals(rightVal);
            } else if (rightVal.isString()) {
                return MySQLConstant.createBoolean(value.equalsIgnoreCase(rightVal.getString()));
            } else {
                throw new AssertionError(rightVal);
            }
        }

        @Override
        public String getString() {
            return value;
        }

        @Override
        public boolean isString() {
            return true;
        }

        @Override
        public MySQLConstant castAs(CastType type) {
            if (type == CastType.SIGNED || type == CastType.UNSIGNED) {
                String value = this.value;
                while (value.startsWith(" ") || value.startsWith("\t") || value.startsWith("\n")) {
                    if (value.startsWith("\n")) {
                        /* workaround for https://bugs.mysql.com/bug.php?id=96294 */
                        throw new IgnoreMeException();
                    }
                    value = value.substring(1);
                }
                for (int i = value.length(); i >= 0; i--) {
                    try {
                        String substring = value.substring(0, i);
                        long val = Long.parseLong(substring);
                        return MySQLConstant.createIntConstant(val, type == CastType.SIGNED);
                    } catch (NumberFormatException e) {
                        // ignore
                    }
                }
                return MySQLConstant.createIntConstant(0, type == CastType.SIGNED);
            } else {
                throw new AssertionError();
            }
        }

        @Override
        public String castAsString() {
            return value;
        }

        @Override
        public MySQLDataType getType() {
            return MySQLDataType.VARCHAR;
        }

        @Override
        protected MySQLConstant isLessThan(MySQLConstant rightVal) {
            if (rightVal.isNull()) {
                return MySQLConstant.createNullConstant();
            } else if (rightVal.isInt()) {
                if (asBooleanNotNull()) {
                    // TODO uspport floating point
                    throw new IgnoreMeException();
                }
                checkIfSmallFloatingPointText();
                return castAs(rightVal.isSigned() ? CastType.SIGNED : CastType.UNSIGNED).isLessThan(rightVal);
            } else if (rightVal.isString()) {
                // unexpected result for '-' < "!";
                // return
                // MySQLConstant.createBoolean(value.compareToIgnoreCase(rightVal.getString()) <
                // 0);
                throw new IgnoreMeException();
            } else {
                throw new AssertionError(rightVal);
            }
        }

    }

    public static class MySQLIntConstant extends MySQLConstant {

        private final long value;
        private final String stringRepresentation;
        private final boolean isSigned;

        public MySQLIntConstant(long value, boolean isSigned) {
            this.value = value;
            this.isSigned = isSigned;
            if (isSigned) {
                stringRepresentation = String.valueOf(value);
            } else {
                stringRepresentation = Long.toUnsignedString(value);
            }
        }

        public MySQLIntConstant(long value, String stringRepresentation) {
            this.value = value;
            this.stringRepresentation = stringRepresentation;
            isSigned = true;
        }

        @Override
        public boolean isInt() {
            return true;
        }

        @Override
        public long getInt() {
            return value;
        }

        @Override
        public boolean asBooleanNotNull() {
            return value != 0;
        }

        @Override
        public String getTextRepresentation() {
            return stringRepresentation;
        }

        @Override
        public MySQLConstant isEquals(MySQLConstant rightVal) {
            if (rightVal.isInt()) {
                return MySQLConstant.createBoolean(new BigInteger(getStringRepr())
                        .compareTo(new BigInteger(((MySQLIntConstant) rightVal).getStringRepr())) == 0);
            } else if (rightVal.isNull()) {
                return MySQLConstant.createNullConstant();
            } else if (rightVal.isString()) {
                if (rightVal.asBooleanNotNull()) {
                    // TODO support SELECT .123 = '.123'; by converting to floating point
                    throw new IgnoreMeException();
                }
                return isEquals(rightVal.castAs(CastType.SIGNED));
            } else {
                throw new AssertionError(rightVal);
            }
        }

        @Override
        public MySQLConstant castAs(CastType type) {
            if (type == CastType.SIGNED) {
                return new MySQLIntConstant(value, true);
            } else if (type == CastType.UNSIGNED) {
                return new MySQLIntConstant(value, false);
            } else {
                throw new AssertionError();
            }
        }

        @Override
        public String castAsString() {
            if (isSigned) {
                return String.valueOf(value);
            } else {
                return Long.toUnsignedString(value);
            }
        }

        @Override
        public MySQLDataType getType() {
            return MySQLDataType.INT;
        }

        @Override
        public boolean isSigned() {
            return isSigned;
        }

        private String getStringRepr() {
            if (isSigned) {
                return String.valueOf(value);
            } else {
                return Long.toUnsignedString(value);
            }
        }

        @Override
        protected MySQLConstant isLessThan(MySQLConstant rightVal) {
            if (rightVal.isInt()) {
                long intVal = rightVal.getInt();
                if (isSigned && rightVal.isSigned()) {
                    return MySQLConstant.createBoolean(value < intVal);
                } else {
                    return MySQLConstant.createBoolean(new BigInteger(getStringRepr())
                            .compareTo(new BigInteger(((MySQLIntConstant) rightVal).getStringRepr())) < 0);
                    // return MySQLConstant.createBoolean(Long.compareUnsigned(value, intVal) < 0);
                }
            } else if (rightVal.isNull()) {
                return MySQLConstant.createNullConstant();
            } else if (rightVal.isString()) {
                if (rightVal.asBooleanNotNull()) {
                    // TODO support float
                    throw new IgnoreMeException();
                }
                return isLessThan(rightVal.castAs(isSigned ? CastType.SIGNED : CastType.UNSIGNED));
            } else {
                throw new AssertionError(rightVal);
            }
        }

    }

    public static class MySQLNullConstant extends MySQLConstant {

        @Override
        public boolean isNull() {
            return true;
        }

        @Override
        public boolean asBooleanNotNull() {
            throw new UnsupportedOperationException(this.toString());
        }

        @Override
        public String getTextRepresentation() {
            return "NULL";
        }

        @Override
        public MySQLConstant isEquals(MySQLConstant rightVal) {
            return MySQLConstant.createNullConstant();
        }

        @Override
        public MySQLConstant castAs(CastType type) {
            return this;
        }

        @Override
        public String castAsString() {
            return "NULL";
        }

        @Override
        public MySQLDataType getType() {
            return null;
        }

        @Override
        protected MySQLConstant isLessThan(MySQLConstant rightVal) {
            return this;
        }

    }

    private static String pad2(int value) {
        return String.format("%02d", value);
    }

    private static String pad4(int value) {
        return String.format("%04d", value);
    }

    private static String padFraction(int fraction, int fsp) {
        if (fsp <= 0) {
            return "";
        }
        return String.format("%0" + fsp + "d", fraction);
    }

    public static class MySQLDateConstant extends MySQLConstant {

        private final int year;
        private final int month;
        private final int day;

        public MySQLDateConstant(int year, int month, int day) {
            this.year = year;
            this.month = month;
            this.day = day;
        }

        @Override
        public String getTextRepresentation() {
            return "'" + castAsString() + "'";
        }

        @Override
        public String castAsString() {
            return pad4(year) + "-" + pad2(month) + "-" + pad2(day);
        }

        @Override
        public MySQLDataType getType() {
            return MySQLDataType.DATE;
        }

        @Override
        public boolean asBooleanNotNull() {
            // Temporal literals are non-NULL and (for our purposes) always truthy.
            return true;
        }

        @Override
        public MySQLConstant isEquals(MySQLConstant rightVal) {
            if (rightVal.isNull()) {
                return MySQLConstant.createNullConstant();
            }
            if (rightVal.getType() != getType()) {
                throw new IgnoreMeException();
            }
            return MySQLConstant.createBoolean(castAsString().equals(rightVal.castAsString()));
        }

        @Override
        public MySQLConstant castAs(CastType type) {
            // Simplification: casting DATE/TIME/TIMESTAMP/DATETIME literals to numeric is not implemented.
            throw new IgnoreMeException();
        }

        @Override
        protected MySQLConstant isLessThan(MySQLConstant rightVal) {
            if (rightVal.isNull()) {
                return MySQLConstant.createNullConstant();
            }
            if (rightVal.getType() != getType()) {
                throw new IgnoreMeException();
            }
            // Fixed-width representation makes lexicographic order match chronological order.
            return MySQLConstant.createBoolean(castAsString().compareTo(rightVal.castAsString()) < 0);
        }
    }

    public static class MySQLTimeConstant extends MySQLConstant {

        private final int hour;
        private final int minute;
        private final int second;
        private final int fraction;
        private final int fsp;

        public MySQLTimeConstant(int hour, int minute, int second) {
            this(hour, minute, second, 0, 0);
        }

        public MySQLTimeConstant(int hour, int minute, int second, int fraction, int fsp) {
            this.hour = hour;
            this.minute = minute;
            this.second = second;
            this.fraction = fraction;
            this.fsp = fsp;
        }

        @Override
        public String getTextRepresentation() {
            return "'" + castAsString() + "'";
        }

        @Override
        public String castAsString() {
            String base = pad2(hour) + ":" + pad2(minute) + ":" + pad2(second);
            if (fsp > 0) {
                // TIME fsp=6 with fraction=0 must render as ".000000"
                return base + "." + padFraction(fraction, fsp);
            }
            return base;
        }

        @Override
        public MySQLDataType getType() {
            return MySQLDataType.TIME;
        }

        @Override
        public boolean asBooleanNotNull() {
            return true;
        }

        @Override
        public MySQLConstant isEquals(MySQLConstant rightVal) {
            if (rightVal.isNull()) {
                return MySQLConstant.createNullConstant();
            }
            if (rightVal.getType() != getType()) {
                throw new IgnoreMeException();
            }
            return MySQLConstant.createBoolean(castAsString().equals(rightVal.castAsString()));
        }

        @Override
        public MySQLConstant castAs(CastType type) {
            // Simplification: casting TIME literals to numeric is not implemented.
            throw new IgnoreMeException();
        }

        @Override
        protected MySQLConstant isLessThan(MySQLConstant rightVal) {
            if (rightVal.isNull()) {
                return MySQLConstant.createNullConstant();
            }
            if (rightVal.getType() != getType()) {
                throw new IgnoreMeException();
            }
            return MySQLConstant.createBoolean(castAsString().compareTo(rightVal.castAsString()) < 0);
        }
    }

    public static class MySQLDateTimeConstant extends MySQLConstant {

        private final int year;
        private final int month;
        private final int day;
        private final int hour;
        private final int minute;
        private final int second;
        private final int fraction;
        private final int fsp;

        public MySQLDateTimeConstant(int year, int month, int day, int hour, int minute, int second) {
            this(year, month, day, hour, minute, second, 0, 0);
        }

        public MySQLDateTimeConstant(int year, int month, int day, int hour, int minute, int second, int fraction,
                int fsp) {
            this.year = year;
            this.month = month;
            this.day = day;
            this.hour = hour;
            this.minute = minute;
            this.second = second;
            this.fraction = fraction;
            this.fsp = fsp;
        }

        @Override
        public String getTextRepresentation() {
            return "'" + castAsString() + "'";
        }

        @Override
        public String castAsString() {
            String base = pad4(year) + "-" + pad2(month) + "-" + pad2(day) + " " + pad2(hour) + ":"
                    + pad2(minute) + ":" + pad2(second);
            if (fsp > 0) {
                // DATETIME/TIMESTAMP fsp=6 with fraction=0 must render as ".000000"
                return base + "." + padFraction(fraction, fsp);
            }
            return base;
        }

        @Override
        public MySQLDataType getType() {
            return MySQLDataType.DATETIME;
        }

        @Override
        public boolean asBooleanNotNull() {
            return true;
        }

        @Override
        public MySQLConstant isEquals(MySQLConstant rightVal) {
            if (rightVal.isNull()) {
                return MySQLConstant.createNullConstant();
            }
            if (rightVal.getType() != getType()) {
                throw new IgnoreMeException();
            }
            return MySQLConstant.createBoolean(castAsString().equals(rightVal.castAsString()));
        }

        @Override
        public MySQLConstant castAs(CastType type) {
            // Simplification: casting DATETIME literals to numeric is not implemented.
            throw new IgnoreMeException();
        }

        @Override
        protected MySQLConstant isLessThan(MySQLConstant rightVal) {
            if (rightVal.isNull()) {
                return MySQLConstant.createNullConstant();
            }
            if (rightVal.getType() != getType()) {
                throw new IgnoreMeException();
            }
            return MySQLConstant.createBoolean(castAsString().compareTo(rightVal.castAsString()) < 0);
        }
    }

    public static class MySQLTimestampConstant extends MySQLConstant {

        private final int year;
        private final int month;
        private final int day;
        private final int hour;
        private final int minute;
        private final int second;
        private final int fraction;
        private final int fsp;

        public MySQLTimestampConstant(int year, int month, int day, int hour, int minute, int second) {
            this(year, month, day, hour, minute, second, 0, 0);
        }

        public MySQLTimestampConstant(int year, int month, int day, int hour, int minute, int second, int fraction,
                int fsp) {
            this.year = year;
            this.month = month;
            this.day = day;
            this.hour = hour;
            this.minute = minute;
            this.second = second;
            this.fraction = fraction;
            this.fsp = fsp;
        }

        @Override
        public String getTextRepresentation() {
            return "'" + castAsString() + "'";
        }

        @Override
        public String castAsString() {
            String base = pad4(year) + "-" + pad2(month) + "-" + pad2(day) + " " + pad2(hour) + ":"
                    + pad2(minute) + ":" + pad2(second);
            if (fsp > 0) {
                // DATETIME/TIMESTAMP fsp=6 with fraction=0 must render as ".000000"
                return base + "." + padFraction(fraction, fsp);
            }
            return base;
        }

        @Override
        public MySQLDataType getType() {
            return MySQLDataType.TIMESTAMP;
        }

        @Override
        public boolean asBooleanNotNull() {
            return true;
        }

        @Override
        public MySQLConstant isEquals(MySQLConstant rightVal) {
            if (rightVal.isNull()) {
                return MySQLConstant.createNullConstant();
            }
            if (rightVal.getType() != getType()) {
                throw new IgnoreMeException();
            }
            return MySQLConstant.createBoolean(castAsString().equals(rightVal.castAsString()));
        }

        @Override
        public MySQLConstant castAs(CastType type) {
            // Simplification: casting TIMESTAMP literals to numeric is not implemented.
            throw new IgnoreMeException();
        }

        @Override
        protected MySQLConstant isLessThan(MySQLConstant rightVal) {
            if (rightVal.isNull()) {
                return MySQLConstant.createNullConstant();
            }
            if (rightVal.getType() != getType()) {
                throw new IgnoreMeException();
            }
            return MySQLConstant.createBoolean(castAsString().compareTo(rightVal.castAsString()) < 0);
        }
    }

    public static class MySQLYearConstant extends MySQLConstant {

        private final int year;

        public MySQLYearConstant(int year) {
            this.year = year;
        }

        @Override
        public String getTextRepresentation() {
            return "'" + castAsString() + "'";
        }

        @Override
        public String castAsString() {
            return String.valueOf(year);
        }

        @Override
        public MySQLDataType getType() {
            return MySQLDataType.YEAR;
        }

        @Override
        public boolean asBooleanNotNull() {
            return true;
        }

        @Override
        public MySQLConstant isEquals(MySQLConstant rightVal) {
            if (rightVal.isNull()) {
                return MySQLConstant.createNullConstant();
            }
            if (rightVal.getType() != getType()) {
                throw new IgnoreMeException();
            }
            return MySQLConstant.createBoolean(castAsString().equals(rightVal.castAsString()));
        }

        @Override
        public MySQLConstant castAs(CastType type) {
            // Simplification: casting YEAR literals to numeric is not implemented.
            throw new IgnoreMeException();
        }

        @Override
        protected MySQLConstant isLessThan(MySQLConstant rightVal) {
            if (rightVal.isNull()) {
                return MySQLConstant.createNullConstant();
            }
            if (rightVal.getType() != getType()) {
                throw new IgnoreMeException();
            }
            return MySQLConstant.createBoolean(Integer.compare(year, ((MySQLYearConstant) rightVal).year) < 0);
        }
    }

    public long getInt() {
        throw new UnsupportedOperationException();
    }

    public boolean isSigned() {
        return false;
    }

    public String getString() {
        throw new UnsupportedOperationException();
    }

    public boolean isString() {
        return false;
    }

    public static MySQLConstant createNullConstant() {
        return new MySQLNullConstant();
    }

    public static MySQLConstant createIntConstant(long value) {
        return new MySQLIntConstant(value, true);
    }

    public static MySQLConstant createIntConstant(long value, boolean signed) {
        return new MySQLIntConstant(value, signed);
    }

    public static MySQLConstant createUnsignedIntConstant(long value) {
        return new MySQLIntConstant(value, false);
    }

    public static MySQLConstant createIntConstantNotAsBoolean(long value) {
        return new MySQLIntConstant(value, String.valueOf(value));
    }

    @Override
    public MySQLConstant getExpectedValue() {
        return this;
    }

    public abstract boolean asBooleanNotNull();

    public abstract String getTextRepresentation();

    public static MySQLConstant createFalse() {
        return MySQLConstant.createIntConstant(0);
    }

    public static MySQLConstant createBoolean(boolean isTrue) {
        return MySQLConstant.createIntConstant(isTrue ? 1 : 0);
    }

    public static MySQLConstant createTrue() {
        return MySQLConstant.createIntConstant(1);
    }

    @Override
    public String toString() {
        return getTextRepresentation();
    }

    public abstract MySQLConstant isEquals(MySQLConstant rightVal);

    public abstract MySQLConstant castAs(CastType type);

    public abstract String castAsString();

    public static MySQLConstant createStringConstant(String string) {
        return new MySQLTextConstant(string);
    }

    public abstract MySQLDataType getType();

    protected abstract MySQLConstant isLessThan(MySQLConstant rightVal);

}
