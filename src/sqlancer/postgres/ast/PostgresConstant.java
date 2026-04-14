package sqlancer.postgres.ast;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import sqlancer.IgnoreMeException;
import sqlancer.postgres.PostgresSchema.PostgresDataType;

public abstract class PostgresConstant implements PostgresExpression {

    public abstract String getTextRepresentation();

    public abstract String getUnquotedTextRepresentation();

    public static class BooleanConstant extends PostgresConstant {

        private final boolean value;

        public BooleanConstant(boolean value) {
            this.value = value;
        }

        @Override
        public String getTextRepresentation() {
            return value ? "TRUE" : "FALSE";
        }

        @Override
        public PostgresDataType getExpressionType() {
            return PostgresDataType.BOOLEAN;
        }

        @Override
        public boolean asBoolean() {
            return value;
        }

        @Override
        public boolean isBoolean() {
            return true;
        }

        @Override
        public PostgresConstant isEquals(PostgresConstant rightVal) {
            if (rightVal.isNull()) {
                return PostgresConstant.createNullConstant();
            } else if (rightVal.isBoolean()) {
                return PostgresConstant.createBooleanConstant(value == rightVal.asBoolean());
            } else if (rightVal.isString()) {
                return PostgresConstant
                        .createBooleanConstant(value == rightVal.cast(PostgresDataType.BOOLEAN).asBoolean());
            } else {
                throw new AssertionError(rightVal);
            }
        }

        @Override
        protected PostgresConstant isLessThan(PostgresConstant rightVal) {
            if (rightVal.isNull()) {
                return PostgresConstant.createNullConstant();
            } else if (rightVal.isString()) {
                return isLessThan(rightVal.cast(PostgresDataType.BOOLEAN));
            } else {
                assert rightVal.isBoolean();
                return PostgresConstant.createBooleanConstant((value ? 1 : 0) < (rightVal.asBoolean() ? 1 : 0));
            }
        }

        @Override
        public PostgresConstant cast(PostgresDataType type) {
            switch (type) {
            case BOOLEAN:
                return this;
            case INT:
                return PostgresConstant.createIntConstant(value ? 1 : 0);
            case TEXT:
                return PostgresConstant.createTextConstant(value ? "true" : "false");
            default:
                return null;
            }
        }

        @Override
        public String getUnquotedTextRepresentation() {
            return getTextRepresentation();
        }

    }

    public static class PostgresNullConstant extends PostgresConstant {

        @Override
        public String getTextRepresentation() {
            return "NULL";
        }

        @Override
        public PostgresDataType getExpressionType() {
            return null;
        }

        @Override
        public boolean isNull() {
            return true;
        }

        @Override
        public PostgresConstant isEquals(PostgresConstant rightVal) {
            return PostgresConstant.createNullConstant();
        }

        @Override
        protected PostgresConstant isLessThan(PostgresConstant rightVal) {
            return PostgresConstant.createNullConstant();
        }

        @Override
        public PostgresConstant cast(PostgresDataType type) {
            return PostgresConstant.createNullConstant();
        }

        @Override
        public String getUnquotedTextRepresentation() {
            return getTextRepresentation();
        }

    }

    public static class StringConstant extends PostgresConstant {

        private final String value;

        public StringConstant(String value) {
            this.value = value;
        }

        @Override
        public String getTextRepresentation() {
            return String.format("'%s'", value.replace("'", "''"));
        }

        @Override
        public PostgresConstant isEquals(PostgresConstant rightVal) {
            if (rightVal.isNull()) {
                return PostgresConstant.createNullConstant();
            } else if (rightVal.isInt()) {
                return cast(PostgresDataType.INT).isEquals(rightVal.cast(PostgresDataType.INT));
            } else if (rightVal.isBoolean()) {
                return cast(PostgresDataType.BOOLEAN).isEquals(rightVal.cast(PostgresDataType.BOOLEAN));
            } else if (rightVal.isString()) {
                return PostgresConstant.createBooleanConstant(value.contentEquals(rightVal.asString()));
            } else {
                throw new AssertionError(rightVal);
            }
        }

        @Override
        protected PostgresConstant isLessThan(PostgresConstant rightVal) {
            if (rightVal.isNull()) {
                return PostgresConstant.createNullConstant();
            } else if (rightVal.isInt()) {
                return cast(PostgresDataType.INT).isLessThan(rightVal.cast(PostgresDataType.INT));
            } else if (rightVal.isBoolean()) {
                return cast(PostgresDataType.BOOLEAN).isLessThan(rightVal.cast(PostgresDataType.BOOLEAN));
            } else if (rightVal.isString()) {
                return PostgresConstant.createBooleanConstant(value.compareTo(rightVal.asString()) < 0);
            } else {
                throw new AssertionError(rightVal);
            }
        }

        @Override
        public PostgresConstant cast(PostgresDataType type) {
            if (type == PostgresDataType.TEXT) {
                return this;
            }
            String s = value.trim();
            switch (type) {
            case BOOLEAN:
                try {
                    return PostgresConstant.createBooleanConstant(Long.parseLong(s) != 0);
                } catch (NumberFormatException e) {
                }
                switch (s.toUpperCase()) {
                case "T":
                case "TR":
                case "TRU":
                case "TRUE":
                case "1":
                case "YES":
                case "YE":
                case "Y":
                case "ON":
                    return PostgresConstant.createTrue();
                case "F":
                case "FA":
                case "FAL":
                case "FALS":
                case "FALSE":
                case "N":
                case "NO":
                case "OF":
                case "OFF":
                default:
                    return PostgresConstant.createFalse();
                }
            case INT:
                try {
                    return PostgresConstant.createIntConstant(Long.parseLong(s));
                } catch (NumberFormatException e) {
                    return PostgresConstant.createIntConstant(-1);
                }
            case TEXT:
                return this;
            default:
                return null;
            }
        }

        @Override
        public PostgresDataType getExpressionType() {
            return PostgresDataType.TEXT;
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
        public String getUnquotedTextRepresentation() {
            return value;
        }

    }

    public static class IntConstant extends PostgresConstant {

        private final long val;

        public IntConstant(long val) {
            this.val = val;
        }

        @Override
        public String getTextRepresentation() {
            return String.valueOf(val);
        }

        @Override
        public PostgresDataType getExpressionType() {
            return PostgresDataType.INT;
        }

        @Override
        public long asInt() {
            return val;
        }

        @Override
        public boolean isInt() {
            return true;
        }

        @Override
        public PostgresConstant isEquals(PostgresConstant rightVal) {
            if (rightVal.isNull()) {
                return PostgresConstant.createNullConstant();
            } else if (rightVal.isBoolean()) {
                return cast(PostgresDataType.BOOLEAN).isEquals(rightVal);
            } else if (rightVal.isInt()) {
                return PostgresConstant.createBooleanConstant(val == rightVal.asInt());
            } else if (rightVal.isString()) {
                return PostgresConstant.createBooleanConstant(val == rightVal.cast(PostgresDataType.INT).asInt());
            } else {
                throw new AssertionError(rightVal);
            }
        }

        @Override
        protected PostgresConstant isLessThan(PostgresConstant rightVal) {
            if (rightVal.isNull()) {
                return PostgresConstant.createNullConstant();
            } else if (rightVal.isInt()) {
                return PostgresConstant.createBooleanConstant(val < rightVal.asInt());
            } else if (rightVal.isBoolean()) {
                throw new AssertionError(rightVal);
            } else if (rightVal.isString()) {
                return PostgresConstant.createBooleanConstant(val < rightVal.cast(PostgresDataType.INT).asInt());
            } else {
                throw new IgnoreMeException();
            }

        }

        @Override
        public PostgresConstant cast(PostgresDataType type) {
            switch (type) {
            case BOOLEAN:
                return PostgresConstant.createBooleanConstant(val != 0);
            case INT:
                return this;
            case TEXT:
                return PostgresConstant.createTextConstant(String.valueOf(val));
            default:
                return null;
            }
        }

        @Override
        public String getUnquotedTextRepresentation() {
            return getTextRepresentation();
        }

    }

    public static PostgresConstant createNullConstant() {
        return new PostgresNullConstant();
    }

    public String asString() {
        throw new UnsupportedOperationException(this.toString());
    }

    public boolean isString() {
        return false;
    }

    public static PostgresConstant createIntConstant(long val) {
        return new IntConstant(val);
    }

    public static PostgresConstant createBooleanConstant(boolean val) {
        return new BooleanConstant(val);
    }

    @Override
    public PostgresConstant getExpectedValue() {
        return this;
    }

    public boolean isNull() {
        return false;
    }

    public boolean asBoolean() {
        throw new UnsupportedOperationException(this.toString());
    }

    public static PostgresConstant createFalse() {
        return createBooleanConstant(false);
    }

    public static PostgresConstant createTrue() {
        return createBooleanConstant(true);
    }

    public long asInt() {
        throw new UnsupportedOperationException(this.toString());
    }

    public boolean isBoolean() {
        return false;
    }

    public abstract PostgresConstant isEquals(PostgresConstant rightVal);

    public boolean isInt() {
        return false;
    }

    protected abstract PostgresConstant isLessThan(PostgresConstant rightVal);

    @Override
    public String toString() {
        return getTextRepresentation();
    }

    public abstract PostgresConstant cast(PostgresDataType type);

    public static PostgresConstant createTextConstant(String string) {
        return new StringConstant(string);
    }

    public abstract static class PostgresConstantBase extends PostgresConstant {

        @Override
        public String getUnquotedTextRepresentation() {
            return null;
        }

        @Override
        public PostgresConstant isEquals(PostgresConstant rightVal) {
            return null;
        }

        @Override
        protected PostgresConstant isLessThan(PostgresConstant rightVal) {
            return null;
        }

        @Override
        public PostgresConstant cast(PostgresDataType type) {
            return null;
        }
    }

    public static class DecimalConstant extends PostgresConstantBase {

        private final BigDecimal val;

        public DecimalConstant(BigDecimal val) {
            this.val = val;
        }

        @Override
        public String getTextRepresentation() {
            return String.valueOf(val);
        }

        @Override
        public PostgresDataType getExpressionType() {
            return PostgresDataType.DECIMAL;
        }

    }

    public static class InetConstant extends PostgresConstantBase {

        private final String val;

        public InetConstant(String val) {
            this.val = "'" + val + "'";
        }

        @Override
        public String getTextRepresentation() {
            return String.valueOf(val);
        }

        @Override
        public PostgresDataType getExpressionType() {
            return PostgresDataType.INET;
        }

    }

    public static class FloatConstant extends PostgresConstantBase {

        private final float val;

        public FloatConstant(float val) {
            this.val = val;
        }

        @Override
        public String getTextRepresentation() {
            if (Double.isFinite(val)) {
                return String.valueOf(val);
            } else {
                return "'" + val + "'";
            }
        }

        @Override
        public PostgresDataType getExpressionType() {
            return PostgresDataType.FLOAT;
        }

    }

    public static class DoubleConstant extends PostgresConstantBase {

        private final double val;

        public DoubleConstant(double val) {
            this.val = val;
        }

        @Override
        public String getTextRepresentation() {
            if (Double.isFinite(val)) {
                return String.valueOf(val);
            } else {
                return "'" + val + "'";
            }
        }

        @Override
        public PostgresDataType getExpressionType() {
            return PostgresDataType.FLOAT;
        }

    }

    public static class BitConstant extends PostgresConstantBase {

        private final long val;

        public BitConstant(long val) {
            this.val = val;
        }

        @Override
        public String getTextRepresentation() {
            return String.format("B'%s'", Long.toBinaryString(val));
        }

        @Override
        public PostgresDataType getExpressionType() {
            return PostgresDataType.BIT;
        }

    }

    public static class RangeConstant extends PostgresConstantBase {

        private final long left;
        private final boolean leftIsInclusive;
        private final long right;
        private final boolean rightIsInclusive;

        public RangeConstant(long left, boolean leftIsInclusive, long right, boolean rightIsInclusive) {
            this.left = left;
            this.leftIsInclusive = leftIsInclusive;
            this.right = right;
            this.rightIsInclusive = rightIsInclusive;
        }

        @Override
        public String getTextRepresentation() {
            StringBuilder sb = new StringBuilder();
            sb.append("'");
            if (leftIsInclusive) {
                sb.append("[");
            } else {
                sb.append("(");
            }
            sb.append(left);
            sb.append(",");
            sb.append(right);
            if (rightIsInclusive) {
                sb.append("]");
            } else {
                sb.append(")");
            }
            sb.append("'");
            sb.append("::int4range");
            return sb.toString();
        }

        @Override
        public PostgresDataType getExpressionType() {
            return PostgresDataType.RANGE;
        }

    }

    public static PostgresConstant createDecimalConstant(BigDecimal bigDecimal) {
        return new DecimalConstant(bigDecimal);
    }

    public static PostgresConstant createFloatConstant(float val) {
        return new FloatConstant(val);
    }

    public static PostgresConstant createDoubleConstant(double val) {
        return new DoubleConstant(val);
    }

    public static PostgresConstant createRange(long left, boolean leftIsInclusive, long right,
            boolean rightIsInclusive) {
        long realLeft;
        long realRight;
        if (left > right) {
            realRight = left;
            realLeft = right;
        } else {
            realLeft = left;
            realRight = right;
        }
        return new RangeConstant(realLeft, leftIsInclusive, realRight, rightIsInclusive);
    }

    public static PostgresExpression createBitConstant(long integer) {
        return new BitConstant(integer);
    }

    public static PostgresExpression createInetConstant(String val) {
        return new InetConstant(val);
    }

    public static class UUIDConstant extends PostgresConstantBase {
        private final String uuid;

        public UUIDConstant(String uuid) {
            this.uuid = uuid;
        }

        @Override
        public String getTextRepresentation() {
            // Keep it explicit to avoid unknown-type ambiguity.
            return "'" + uuid + "'::uuid";
        }

        @Override
        public PostgresDataType getExpressionType() {
            return PostgresDataType.UUID;
        }
    }

    public static class EnumConstant extends PostgresConstantBase {
        private final String typeName;
        private final String label;

        public EnumConstant(String typeName, String label) {
            this.typeName = typeName;
            this.label = label;
        }

        @Override
        public String getTextRepresentation() {
            return "'" + label.replace("'", "''") + "'::" + typeName;
        }

        @Override
        public PostgresDataType getExpressionType() {
            return PostgresDataType.ENUM;
        }
    }

    public static class ArrayConstant extends PostgresConstantBase {
        private final PostgresDataType arrayType;
        private final List<PostgresConstant> elements;

        public ArrayConstant(PostgresDataType arrayType, List<PostgresConstant> elements) {
            this.arrayType = arrayType;
            this.elements = elements;
        }

        @Override
        public String getTextRepresentation() {
            StringBuilder sb = new StringBuilder();
            sb.append("ARRAY[");
            for (int i = 0; i < elements.size(); i++) {
                if (i != 0) {
                    sb.append(", ");
                }
                sb.append(elements.get(i).getTextRepresentation());
            }
            sb.append("]::");
            sb.append(getArrayBaseTypeName(arrayType));
            sb.append("[]");
            return sb.toString();
        }

        @Override
        public PostgresDataType getExpressionType() {
            return arrayType;
        }

        private static String getArrayBaseTypeName(PostgresDataType arrayType) {
            switch (arrayType) {
            case INT_ARRAY:
                return "integer";
            case TEXT_ARRAY:
                return "text";
            case UUID_ARRAY:
                return "uuid";
            case TIMESTAMPTZ_ARRAY:
                return "timestamptz";
            default:
                throw new AssertionError(arrayType);
            }
        }
    }

    public static PostgresConstant createUUIDConstant(String uuid) {
        return new UUIDConstant(uuid);
    }

    public static class ByteaConstant extends PostgresConstantBase {
        private final String hex;

        public ByteaConstant(String hex) {
            this.hex = hex;
        }

        @Override
        public String getTextRepresentation() {
            // Stable representation across settings/locales.
            return "decode('" + hex + "','hex')";
        }

        @Override
        public PostgresDataType getExpressionType() {
            return PostgresDataType.BYTEA;
        }
    }

    public static PostgresConstant createByteaConstant(String hex) {
        return new ByteaConstant(hex);
    }

    public static PostgresConstant createEnumConstant(String typeName, String label) {
        return new EnumConstant(typeName, label);
    }

    public static PostgresConstant createArrayConstant(PostgresDataType arrayType, List<PostgresConstant> elements) {
        return new ArrayConstant(arrayType, elements);
    }

    private static abstract class PostgresTemporalConstant extends PostgresConstant {
        @Override
        public boolean isNull() {
            return false;
        }

        @Override
        public PostgresConstant cast(PostgresDataType type) {
            if (type == PostgresDataType.TEXT) {
                return PostgresConstant.createTextConstant(getUnquotedTextRepresentation());
            }
            return null;
        }
    }

    public static class DateConstant extends PostgresTemporalConstant {
        private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE;
        private final LocalDate date;
        private final long epochDay;

        public DateConstant(LocalDate date) {
            this.date = date;
            this.epochDay = date.toEpochDay();
        }

        @Override
        public String getTextRepresentation() {
            return "'" + FMT.format(date) + "'::date";
        }

        @Override
        public String getUnquotedTextRepresentation() {
            return FMT.format(date);
        }

        @Override
        public PostgresDataType getExpressionType() {
            return PostgresDataType.DATE;
        }

        @Override
        public PostgresConstant isEquals(PostgresConstant rightVal) {
            if (rightVal.isNull()) {
                return PostgresConstant.createNullConstant();
            }
            if (rightVal instanceof DateConstant) {
                return PostgresConstant.createBooleanConstant(epochDay == ((DateConstant) rightVal).epochDay);
            }
            throw new IgnoreMeException();
        }

        @Override
        protected PostgresConstant isLessThan(PostgresConstant rightVal) {
            if (rightVal.isNull()) {
                return PostgresConstant.createNullConstant();
            }
            if (rightVal instanceof DateConstant) {
                return PostgresConstant.createBooleanConstant(epochDay < ((DateConstant) rightVal).epochDay);
            }
            throw new IgnoreMeException();
        }
    }

    public static class TimeConstant extends PostgresTemporalConstant {
        private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
        private final LocalTime time;
        private final int secondOfDay;

        public TimeConstant(LocalTime time) {
            this.time = time.withNano(0);
            this.secondOfDay = this.time.toSecondOfDay();
        }

        @Override
        public String getTextRepresentation() {
            return "'" + FMT.format(time) + "'::time";
        }

        @Override
        public String getUnquotedTextRepresentation() {
            return FMT.format(time);
        }

        @Override
        public PostgresDataType getExpressionType() {
            return PostgresDataType.TIME;
        }

        @Override
        public PostgresConstant isEquals(PostgresConstant rightVal) {
            if (rightVal.isNull()) {
                return PostgresConstant.createNullConstant();
            }
            if (rightVal instanceof TimeConstant) {
                return PostgresConstant.createBooleanConstant(secondOfDay == ((TimeConstant) rightVal).secondOfDay);
            }
            throw new IgnoreMeException();
        }

        @Override
        protected PostgresConstant isLessThan(PostgresConstant rightVal) {
            if (rightVal.isNull()) {
                return PostgresConstant.createNullConstant();
            }
            if (rightVal instanceof TimeConstant) {
                return PostgresConstant.createBooleanConstant(secondOfDay < ((TimeConstant) rightVal).secondOfDay);
            }
            throw new IgnoreMeException();
        }
    }

    public static class TimestampConstant extends PostgresTemporalConstant {
        private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        private final LocalDateTime timestamp;
        private final long epochSecondUtc;

        public TimestampConstant(LocalDateTime timestamp) {
            this.timestamp = timestamp.withNano(0);
            this.epochSecondUtc = this.timestamp.toEpochSecond(ZoneOffset.UTC);
        }

        @Override
        public String getTextRepresentation() {
            return "'" + FMT.format(timestamp) + "'::timestamp";
        }

        @Override
        public String getUnquotedTextRepresentation() {
            return FMT.format(timestamp);
        }

        @Override
        public PostgresDataType getExpressionType() {
            return PostgresDataType.TIMESTAMP;
        }

        @Override
        public PostgresConstant isEquals(PostgresConstant rightVal) {
            if (rightVal.isNull()) {
                return PostgresConstant.createNullConstant();
            }
            if (rightVal instanceof TimestampConstant) {
                return PostgresConstant
                        .createBooleanConstant(epochSecondUtc == ((TimestampConstant) rightVal).epochSecondUtc);
            }
            throw new IgnoreMeException();
        }

        @Override
        protected PostgresConstant isLessThan(PostgresConstant rightVal) {
            if (rightVal.isNull()) {
                return PostgresConstant.createNullConstant();
            }
            if (rightVal instanceof TimestampConstant) {
                return PostgresConstant.createBooleanConstant(epochSecondUtc < ((TimestampConstant) rightVal).epochSecondUtc);
            }
            throw new IgnoreMeException();
        }
    }

    public static class TimestampTZConstant extends PostgresTemporalConstant {
        private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssXXX");
        private final OffsetDateTime timestamptz;
        private final long epochSecond;

        public TimestampTZConstant(OffsetDateTime timestamptz) {
            this.timestamptz = timestamptz.withNano(0);
            this.epochSecond = this.timestamptz.toInstant().getEpochSecond();
        }

        @Override
        public String getTextRepresentation() {
            return "'" + FMT.format(timestamptz) + "'::timestamptz";
        }

        @Override
        public String getUnquotedTextRepresentation() {
            return FMT.format(timestamptz);
        }

        @Override
        public PostgresDataType getExpressionType() {
            return PostgresDataType.TIMESTAMPTZ;
        }

        @Override
        public PostgresConstant isEquals(PostgresConstant rightVal) {
            if (rightVal.isNull()) {
                return PostgresConstant.createNullConstant();
            }
            if (rightVal instanceof TimestampTZConstant) {
                return PostgresConstant.createBooleanConstant(epochSecond == ((TimestampTZConstant) rightVal).epochSecond);
            }
            throw new IgnoreMeException();
        }

        @Override
        protected PostgresConstant isLessThan(PostgresConstant rightVal) {
            if (rightVal.isNull()) {
                return PostgresConstant.createNullConstant();
            }
            if (rightVal instanceof TimestampTZConstant) {
                return PostgresConstant.createBooleanConstant(epochSecond < ((TimestampTZConstant) rightVal).epochSecond);
            }
            throw new IgnoreMeException();
        }
    }

    public static class IntervalConstant extends PostgresTemporalConstant {
        private final long seconds;

        public IntervalConstant(long seconds) {
            this.seconds = seconds;
        }

        @Override
        public String getTextRepresentation() {
            return "'" + seconds + " seconds'::interval";
        }

        @Override
        public String getUnquotedTextRepresentation() {
            return seconds + " seconds";
        }

        @Override
        public PostgresDataType getExpressionType() {
            return PostgresDataType.INTERVAL;
        }

        @Override
        public PostgresConstant isEquals(PostgresConstant rightVal) {
            if (rightVal.isNull()) {
                return PostgresConstant.createNullConstant();
            }
            if (rightVal instanceof IntervalConstant) {
                return PostgresConstant.createBooleanConstant(seconds == ((IntervalConstant) rightVal).seconds);
            }
            throw new IgnoreMeException();
        }

        @Override
        protected PostgresConstant isLessThan(PostgresConstant rightVal) {
            if (rightVal.isNull()) {
                return PostgresConstant.createNullConstant();
            }
            if (rightVal instanceof IntervalConstant) {
                return PostgresConstant.createBooleanConstant(seconds < ((IntervalConstant) rightVal).seconds);
            }
            throw new IgnoreMeException();
        }
    }

    public static PostgresConstant createDateConstant(LocalDate date) {
        return new DateConstant(date);
    }

    public static PostgresConstant createTimeConstant(LocalTime time) {
        return new TimeConstant(time);
    }

    public static PostgresConstant createTimestampConstant(LocalDateTime timestamp) {
        return new TimestampConstant(timestamp);
    }

    public static PostgresConstant createTimestamptzConstant(Instant instant) {
        return new TimestampTZConstant(instant.atOffset(ZoneOffset.UTC));
    }

    public static PostgresConstant createIntervalSecondsConstant(long seconds) {
        return new IntervalConstant(seconds);
    }

}
