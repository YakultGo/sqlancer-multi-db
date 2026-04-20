package sqlancer.gaussdbpg.ast;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import sqlancer.IgnoreMeException;
import sqlancer.Randomly;

public abstract class GaussDBPGConstant implements GaussDBPGExpression {

    public abstract String getTextRepresentation();

    public static GaussDBPGConstant createNullConstant() {
        return new GaussDBPGNullConstant();
    }

    public static GaussDBPGConstant createIntConstant(long val) {
        return new GaussDBPGIntConstant(val);
    }

    public static GaussDBPGConstant createBooleanConstant(boolean val) {
        return new GaussDBPGBooleanConstant(val);
    }

    public static GaussDBPGConstant createTextConstant(String val) {
        return new GaussDBPGTextConstant(val);
    }

    public static GaussDBPGConstant createDecimalConstant(BigDecimal val) {
        return new GaussDBPGDecimalConstant(val);
    }

    public static GaussDBPGConstant createFloatConstant(double val) {
        return new GaussDBPGFloatConstant(val);
    }

    public static GaussDBPGConstant createDateConstant(LocalDate date) {
        return new GaussDBPGDateConstant(date);
    }

    public static GaussDBPGConstant createTimeConstant(LocalTime time) {
        return new GaussDBPGTimeConstant(time);
    }

    public static GaussDBPGConstant createTimestampConstant(LocalDateTime timestamp) {
        return new GaussDBPGTimestampConstant(timestamp);
    }

    public static GaussDBPGConstant createRandomConstant(Randomly r) {
        switch (Randomly.fromOptions(0, 1, 2, 3, 4)) {
        case 0:
            return createIntConstant(r.getInteger());
        case 1:
            return createTextConstant(r.getString());
        case 2:
            return createNullConstant();
        case 3:
            return createBooleanConstant(Randomly.getBoolean());
        case 4:
            return createDecimalConstant(r.getRandomBigDecimal());
        default:
            throw new AssertionError();
        }
    }

    @Override
    public GaussDBPGConstant getExpectedValue() {
        return this;
    }

    // PG semantics: empty string is NOT NULL
    public boolean isNull() {
        return false;
    }

    public boolean isBoolean() {
        return false;
    }

    public boolean isInt() {
        return false;
    }

    public boolean isString() {
        return false;
    }

    public boolean asBoolean() {
        throw new UnsupportedOperationException(this.toString());
    }

    public long asInt() {
        throw new UnsupportedOperationException(this.toString());
    }

    public String asString() {
        throw new UnsupportedOperationException(this.toString());
    }

    public BigDecimal asDecimal() {
        throw new UnsupportedOperationException(this.toString());
    }

    public boolean isDecimal() {
        return false;
    }

    public boolean isFloat() {
        return false;
    }

    public double asFloat() {
        throw new UnsupportedOperationException(this.toString());
    }

    // Comparison operations with PG semantics (NULL comparison returns NULL)
    public abstract GaussDBPGConstant isEquals(GaussDBPGConstant rightVal);

    protected abstract GaussDBPGConstant isLessThan(GaussDBPGConstant rightVal);

    public abstract GaussDBPGConstant cast(GaussDBPGDataType type);

    @Override
    public String toString() {
        return getTextRepresentation();
    }

    // ==================== Null Constant ====================
    public static class GaussDBPGNullConstant extends GaussDBPGConstant {

        @Override
        public String getTextRepresentation() {
            return "NULL";
        }

        @Override
        public GaussDBPGDataType getExpressionType() {
            return null;
        }

        @Override
        public boolean isNull() {
            return true;
        }

        @Override
        public GaussDBPGConstant isEquals(GaussDBPGConstant rightVal) {
            // NULL = anything returns NULL in PG
            return createNullConstant();
        }

        @Override
        protected GaussDBPGConstant isLessThan(GaussDBPGConstant rightVal) {
            // NULL < anything returns NULL in PG
            return createNullConstant();
        }

        @Override
        public GaussDBPGConstant cast(GaussDBPGDataType type) {
            return createNullConstant();
        }
    }

    // ==================== Boolean Constant ====================
    public static class GaussDBPGBooleanConstant extends GaussDBPGConstant {

        private final boolean value;

        public GaussDBPGBooleanConstant(boolean value) {
            this.value = value;
        }

        @Override
        public String getTextRepresentation() {
            return value ? "TRUE" : "FALSE";
        }

        @Override
        public GaussDBPGDataType getExpressionType() {
            return GaussDBPGDataType.BOOLEAN;
        }

        @Override
        public boolean isBoolean() {
            return true;
        }

        @Override
        public boolean asBoolean() {
            return value;
        }

        @Override
        public GaussDBPGConstant isEquals(GaussDBPGConstant rightVal) {
            if (rightVal.isNull()) {
                return createNullConstant();
            } else if (rightVal.isBoolean()) {
                return createBooleanConstant(value == rightVal.asBoolean());
            } else if (rightVal.isInt()) {
                return createBooleanConstant((value ? 1 : 0) == rightVal.asInt());
            } else if (rightVal.isString()) {
                return createBooleanConstant(value == rightVal.cast(GaussDBPGDataType.BOOLEAN).asBoolean());
            } else if (rightVal.isFloat() || rightVal.isDecimal()) {
                // Compare boolean as 1/0 with float
                return createBooleanConstant((value ? 1.0 : 0.0) == (rightVal.isFloat() ? rightVal.asFloat() : rightVal.asDecimal().doubleValue()));
            } else {
                throw new IgnoreMeException();
            }
        }

        @Override
        protected GaussDBPGConstant isLessThan(GaussDBPGConstant rightVal) {
            if (rightVal.isNull()) {
                return createNullConstant();
            } else if (rightVal.isBoolean()) {
                return createBooleanConstant((value ? 1 : 0) < (rightVal.asBoolean() ? 1 : 0));
            } else if (rightVal.isInt()) {
                return createBooleanConstant((value ? 1 : 0) < rightVal.asInt());
            } else if (rightVal.isString()) {
                return isLessThan(rightVal.cast(GaussDBPGDataType.BOOLEAN));
            } else if (rightVal.isFloat() || rightVal.isDecimal()) {
                return createBooleanConstant((value ? 1.0 : 0.0) < (rightVal.isFloat() ? rightVal.asFloat() : rightVal.asDecimal().doubleValue()));
            } else {
                throw new IgnoreMeException();
            }
        }

        @Override
        public GaussDBPGConstant cast(GaussDBPGDataType type) {
            switch (type) {
            case BOOLEAN:
                return this;
            case INT:
                return createIntConstant(value ? 1 : 0);
            case TEXT:
                return createTextConstant(value ? "true" : "false");
            default:
                return null;
            }
        }
    }

    // ==================== Int Constant ====================
    public static class GaussDBPGIntConstant extends GaussDBPGConstant {

        private final long value;

        public GaussDBPGIntConstant(long value) {
            this.value = value;
        }

        @Override
        public String getTextRepresentation() {
            return String.valueOf(value);
        }

        @Override
        public GaussDBPGDataType getExpressionType() {
            return GaussDBPGDataType.INT;
        }

        @Override
        public boolean isInt() {
            return true;
        }

        @Override
        public long asInt() {
            return value;
        }

        @Override
        public GaussDBPGConstant isEquals(GaussDBPGConstant rightVal) {
            if (rightVal.isNull()) {
                return createNullConstant();
            } else if (rightVal.isInt()) {
                return createBooleanConstant(value == rightVal.asInt());
            } else if (rightVal.isBoolean()) {
                return createBooleanConstant(value == (rightVal.asBoolean() ? 1 : 0));
            } else if (rightVal.isString()) {
                try {
                    return createBooleanConstant(value == Long.parseLong(rightVal.asString().trim()));
                } catch (NumberFormatException e) {
                    return createBooleanConstant(false);
                }
            } else if (rightVal.isDecimal()) {
                return createBooleanConstant(BigDecimal.valueOf(value).compareTo(rightVal.asDecimal()) == 0);
            } else if (rightVal.isFloat()) {
                return createBooleanConstant((double) value == rightVal.asFloat());
            } else {
                throw new IgnoreMeException();
            }
        }

        @Override
        protected GaussDBPGConstant isLessThan(GaussDBPGConstant rightVal) {
            if (rightVal.isNull()) {
                return createNullConstant();
            } else if (rightVal.isInt()) {
                return createBooleanConstant(value < rightVal.asInt());
            } else if (rightVal.isBoolean()) {
                return createBooleanConstant(value < (rightVal.asBoolean() ? 1 : 0));
            } else if (rightVal.isString()) {
                try {
                    return createBooleanConstant(value < Long.parseLong(rightVal.asString().trim()));
                } catch (NumberFormatException e) {
                    throw new IgnoreMeException();
                }
            } else if (rightVal.isDecimal()) {
                return createBooleanConstant(BigDecimal.valueOf(value).compareTo(rightVal.asDecimal()) < 0);
            } else {
                throw new IgnoreMeException();
            }
        }

        @Override
        public GaussDBPGConstant cast(GaussDBPGDataType type) {
            switch (type) {
            case INT:
                return this;
            case BOOLEAN:
                return createBooleanConstant(value != 0);
            case TEXT:
                return createTextConstant(String.valueOf(value));
            case FLOAT:
                return createFloatConstant(value);
            case DECIMAL:
                return createDecimalConstant(BigDecimal.valueOf(value));
            default:
                return null;
            }
        }
    }

    // ==================== Text Constant ====================
    public static class GaussDBPGTextConstant extends GaussDBPGConstant {

        private final String value;

        public GaussDBPGTextConstant(String value) {
            this.value = value;
        }

        @Override
        public String getTextRepresentation() {
            return "'" + value.replace("'", "''") + "'";
        }

        @Override
        public GaussDBPGDataType getExpressionType() {
            return GaussDBPGDataType.TEXT;
        }

        @Override
        public boolean isString() {
            return true;
        }

        @Override
        public String asString() {
            return value;
        }

        @Override
        public GaussDBPGConstant isEquals(GaussDBPGConstant rightVal) {
            if (rightVal.isNull()) {
                return createNullConstant();
            } else if (rightVal.isString()) {
                // PG semantics: empty string != NULL, so this is a normal comparison
                return createBooleanConstant(value.equals(rightVal.asString()));
            } else if (rightVal.isInt()) {
                try {
                    return createBooleanConstant(Long.parseLong(value.trim()) == rightVal.asInt());
                } catch (NumberFormatException e) {
                    return createBooleanConstant(false);
                }
            } else if (rightVal.isBoolean()) {
                return cast(GaussDBPGDataType.BOOLEAN).isEquals(rightVal);
            } else if (rightVal.isFloat()) {
                try {
                    return createBooleanConstant(Double.parseDouble(value.trim()) == rightVal.asFloat());
                } catch (NumberFormatException e) {
                    return createBooleanConstant(false);
                }
            } else if (rightVal.isDecimal()) {
                try {
                    return createBooleanConstant(new BigDecimal(value.trim()).compareTo(rightVal.asDecimal()) == 0);
                } catch (NumberFormatException e) {
                    return createBooleanConstant(false);
                }
            } else {
                throw new IgnoreMeException();
            }
        }

        @Override
        protected GaussDBPGConstant isLessThan(GaussDBPGConstant rightVal) {
            if (rightVal.isNull()) {
                return createNullConstant();
            } else if (rightVal.isString()) {
                return createBooleanConstant(value.compareTo(rightVal.asString()) < 0);
            } else if (rightVal.isInt()) {
                try {
                    return createBooleanConstant(Long.parseLong(value.trim()) < rightVal.asInt());
                } catch (NumberFormatException e) {
                    throw new IgnoreMeException();
                }
            } else if (rightVal.isBoolean()) {
                return cast(GaussDBPGDataType.BOOLEAN).isLessThan(rightVal);
            } else {
                throw new IgnoreMeException();
            }
        }

        @Override
        public GaussDBPGConstant cast(GaussDBPGDataType type) {
            switch (type) {
            case TEXT:
                return this;
            case BOOLEAN:
                String upper = value.trim().toUpperCase();
                if (upper.equals("TRUE") || upper.equals("T") || upper.equals("YES") || upper.equals("Y")
                        || upper.equals("ON") || upper.equals("1")) {
                    return createBooleanConstant(true);
                } else {
                    return createBooleanConstant(false);
                }
            case INT:
                try {
                    return createIntConstant(Long.parseLong(value.trim()));
                } catch (NumberFormatException e) {
                    return createIntConstant(-1);
                }
            case FLOAT:
                try {
                    return createFloatConstant(Double.parseDouble(value.trim()));
                } catch (NumberFormatException e) {
                    return createFloatConstant(0.0);
                }
            default:
                return null;
            }
        }
    }

    // ==================== Decimal Constant ====================
    public static class GaussDBPGDecimalConstant extends GaussDBPGConstant {

        private final BigDecimal value;

        public GaussDBPGDecimalConstant(BigDecimal value) {
            this.value = value;
        }

        @Override
        public String getTextRepresentation() {
            return String.valueOf(value);
        }

        @Override
        public GaussDBPGDataType getExpressionType() {
            return GaussDBPGDataType.DECIMAL;
        }

        @Override
        public boolean isDecimal() {
            return true;
        }

        public BigDecimal asDecimal() {
            return value;
        }

        @Override
        public GaussDBPGConstant isEquals(GaussDBPGConstant rightVal) {
            if (rightVal.isNull()) {
                return createNullConstant();
            } else if (rightVal.isDecimal()) {
                return createBooleanConstant(value.compareTo(rightVal.asDecimal()) == 0);
            } else if (rightVal.isInt()) {
                return createBooleanConstant(value.compareTo(BigDecimal.valueOf(rightVal.asInt())) == 0);
            } else if (rightVal.isFloat()) {
                return createBooleanConstant(value.compareTo(BigDecimal.valueOf(rightVal.asFloat())) == 0);
            } else if (rightVal.isBoolean()) {
                return createBooleanConstant(value.compareTo(BigDecimal.valueOf(rightVal.asBoolean() ? 1 : 0)) == 0);
            } else if (rightVal.isString()) {
                try {
                    return createBooleanConstant(value.compareTo(new BigDecimal(rightVal.asString().trim())) == 0);
                } catch (NumberFormatException e) {
                    return createBooleanConstant(false);
                }
            } else {
                throw new IgnoreMeException();
            }
        }

        @Override
        protected GaussDBPGConstant isLessThan(GaussDBPGConstant rightVal) {
            if (rightVal.isNull()) {
                return createNullConstant();
            } else if (rightVal.isDecimal()) {
                return createBooleanConstant(value.compareTo(rightVal.asDecimal()) < 0);
            } else if (rightVal.isInt()) {
                return createBooleanConstant(value.compareTo(BigDecimal.valueOf(rightVal.asInt())) < 0);
            } else {
                throw new IgnoreMeException();
            }
        }

        @Override
        public GaussDBPGConstant cast(GaussDBPGDataType type) {
            switch (type) {
            case DECIMAL:
                return this;
            case INT:
                return createIntConstant(value.longValue());
            case TEXT:
                return createTextConstant(String.valueOf(value));
            case FLOAT:
                return createFloatConstant(value.doubleValue());
            case BOOLEAN:
                return createBooleanConstant(value.compareTo(BigDecimal.ZERO) != 0);
            default:
                return null;
            }
        }
    }

    // ==================== Float Constant ====================
    public static class GaussDBPGFloatConstant extends GaussDBPGConstant {

        private final double value;

        public GaussDBPGFloatConstant(double value) {
            this.value = value;
        }

        @Override
        public String getTextRepresentation() {
            if (Double.isFinite(value)) {
                return String.valueOf(value);
            } else {
                return "'" + value + "'";
            }
        }

        @Override
        public GaussDBPGDataType getExpressionType() {
            return GaussDBPGDataType.FLOAT;
        }

        @Override
        public boolean isFloat() {
            return true;
        }

        @Override
        public double asFloat() {
            return value;
        }

        @Override
        public GaussDBPGConstant isEquals(GaussDBPGConstant rightVal) {
            if (rightVal.isNull()) {
                return createNullConstant();
            } else if (rightVal.isFloat()) {
                return createBooleanConstant(value == rightVal.asFloat());
            } else if (rightVal.isInt()) {
                return createBooleanConstant(value == rightVal.asInt());
            } else if (rightVal.isDecimal()) {
                return createBooleanConstant(BigDecimal.valueOf(value).compareTo(rightVal.asDecimal()) == 0);
            } else if (rightVal.isBoolean()) {
                return createBooleanConstant((value != 0) == rightVal.asBoolean());
            } else if (rightVal.isString()) {
                try {
                    return createBooleanConstant(value == Double.parseDouble(rightVal.asString().trim()));
                } catch (NumberFormatException e) {
                    return createBooleanConstant(false);
                }
            } else {
                throw new IgnoreMeException();
            }
        }

        @Override
        protected GaussDBPGConstant isLessThan(GaussDBPGConstant rightVal) {
            if (rightVal.isNull()) {
                return createNullConstant();
            } else if (rightVal.isFloat()) {
                return createBooleanConstant(value < rightVal.asFloat());
            } else if (rightVal.isInt()) {
                return createBooleanConstant(value < rightVal.asInt());
            } else {
                throw new IgnoreMeException();
            }
        }

        @Override
        public GaussDBPGConstant cast(GaussDBPGDataType type) {
            switch (type) {
            case FLOAT:
                return this;
            case INT:
                return createIntConstant((long) value);
            case TEXT:
                return createTextConstant(String.valueOf(value));
            case DECIMAL:
                return createDecimalConstant(BigDecimal.valueOf(value));
            case BOOLEAN:
                return createBooleanConstant(value != 0);
            default:
                return null;
            }
        }
    }

    // ==================== Date Constant ====================
    public static class GaussDBPGDateConstant extends GaussDBPGConstant {

        private final LocalDate date;
        private final long epochDay;

        public GaussDBPGDateConstant(LocalDate date) {
            this.date = date;
            this.epochDay = date.toEpochDay();
        }

        @Override
        public String getTextRepresentation() {
            return "'" + date.toString() + "'::date";
        }

        @Override
        public GaussDBPGDataType getExpressionType() {
            return GaussDBPGDataType.DATE;
        }

        public LocalDate asDate() {
            return date;
        }

        @Override
        public GaussDBPGConstant isEquals(GaussDBPGConstant rightVal) {
            if (rightVal.isNull()) {
                return createNullConstant();
            } else if (rightVal instanceof GaussDBPGDateConstant) {
                return createBooleanConstant(epochDay == ((GaussDBPGDateConstant) rightVal).epochDay);
            } else if (rightVal.isString()) {
                return createBooleanConstant(date.toString().equals(rightVal.asString()));
            }
            throw new IgnoreMeException();
        }

        @Override
        protected GaussDBPGConstant isLessThan(GaussDBPGConstant rightVal) {
            if (rightVal.isNull()) {
                return createNullConstant();
            } else if (rightVal instanceof GaussDBPGDateConstant) {
                return createBooleanConstant(epochDay < ((GaussDBPGDateConstant) rightVal).epochDay);
            } else if (rightVal.isString()) {
                return createBooleanConstant(date.toString().compareTo(rightVal.asString()) < 0);
            }
            throw new IgnoreMeException();
        }

        @Override
        public GaussDBPGConstant cast(GaussDBPGDataType type) {
            switch (type) {
            case DATE:
                return this;
            case TEXT:
                return createTextConstant(date.toString());
            case TIMESTAMP:
                return createTimestampConstant(date.atStartOfDay());
            default:
                return null;
            }
        }
    }

    // ==================== Time Constant ====================
    public static class GaussDBPGTimeConstant extends GaussDBPGConstant {

        private final LocalTime time;
        private final int secondOfDay;

        public GaussDBPGTimeConstant(LocalTime time) {
            this.time = time.withNano(0);
            this.secondOfDay = this.time.toSecondOfDay();
        }

        @Override
        public String getTextRepresentation() {
            return "'" + time.toString() + "'::time";
        }

        @Override
        public GaussDBPGDataType getExpressionType() {
            return GaussDBPGDataType.TIME;
        }

        public LocalTime asTime() {
            return time;
        }

        @Override
        public GaussDBPGConstant isEquals(GaussDBPGConstant rightVal) {
            if (rightVal.isNull()) {
                return createNullConstant();
            } else if (rightVal instanceof GaussDBPGTimeConstant) {
                return createBooleanConstant(secondOfDay == ((GaussDBPGTimeConstant) rightVal).secondOfDay);
            } else if (rightVal.isString()) {
                return createBooleanConstant(time.toString().equals(rightVal.asString()));
            }
            throw new IgnoreMeException();
        }

        @Override
        protected GaussDBPGConstant isLessThan(GaussDBPGConstant rightVal) {
            if (rightVal.isNull()) {
                return createNullConstant();
            } else if (rightVal instanceof GaussDBPGTimeConstant) {
                return createBooleanConstant(secondOfDay < ((GaussDBPGTimeConstant) rightVal).secondOfDay);
            } else if (rightVal.isString()) {
                return createBooleanConstant(time.toString().compareTo(rightVal.asString()) < 0);
            }
            throw new IgnoreMeException();
        }

        @Override
        public GaussDBPGConstant cast(GaussDBPGDataType type) {
            switch (type) {
            case TIME:
                return this;
            case TEXT:
                return createTextConstant(time.toString());
            default:
                return null;
            }
        }
    }

    // ==================== Timestamp Constant ====================
    public static class GaussDBPGTimestampConstant extends GaussDBPGConstant {

        private final LocalDateTime timestamp;
        private final long epochSecond;

        public GaussDBPGTimestampConstant(LocalDateTime timestamp) {
            this.timestamp = timestamp.withNano(0);
            this.epochSecond = this.timestamp.toEpochSecond(java.time.ZoneOffset.UTC);
        }

        @Override
        public String getTextRepresentation() {
            return "'" + timestamp.toString() + "'::timestamp";
        }

        @Override
        public GaussDBPGDataType getExpressionType() {
            return GaussDBPGDataType.TIMESTAMP;
        }

        public LocalDateTime asTimestamp() {
            return timestamp;
        }

        @Override
        public GaussDBPGConstant isEquals(GaussDBPGConstant rightVal) {
            if (rightVal.isNull()) {
                return createNullConstant();
            } else if (rightVal instanceof GaussDBPGTimestampConstant) {
                return createBooleanConstant(epochSecond == ((GaussDBPGTimestampConstant) rightVal).epochSecond);
            } else if (rightVal.isString()) {
                return createBooleanConstant(timestamp.toString().equals(rightVal.asString()));
            }
            throw new IgnoreMeException();
        }

        @Override
        protected GaussDBPGConstant isLessThan(GaussDBPGConstant rightVal) {
            if (rightVal.isNull()) {
                return createNullConstant();
            } else if (rightVal instanceof GaussDBPGTimestampConstant) {
                return createBooleanConstant(epochSecond < ((GaussDBPGTimestampConstant) rightVal).epochSecond);
            } else if (rightVal.isString()) {
                return createBooleanConstant(timestamp.toString().compareTo(rightVal.asString()) < 0);
            }
            throw new IgnoreMeException();
        }

        @Override
        public GaussDBPGConstant cast(GaussDBPGDataType type) {
            switch (type) {
            case TIMESTAMP:
                return this;
            case TEXT:
                return createTextConstant(timestamp.toString());
            case DATE:
                return createDateConstant(timestamp.toLocalDate());
            default:
                return null;
            }
        }
    }
}