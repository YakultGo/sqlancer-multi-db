package sqlancer.gaussdba.ast;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import sqlancer.IgnoreMeException;
import sqlancer.Randomly;

/**
 * GaussDB A兼容模式常量类
 *
 * 关键差异：Oracle语义 - 空字符串被视为NULL
 * '' = NULL, NULL = NULL 返回 NULL（不是TRUE也不是FALSE）
 */
public abstract class GaussDBAConstant implements GaussDBAExpression {

    public abstract String getTextRepresentation();

    public static GaussDBAConstant createNullConstant() {
        return new GaussDBANullConstant();
    }

    public static GaussDBAConstant createNumberConstant(long val) {
        return new GaussDBANumberConstant(val);
    }

    public static GaussDBAConstant createNumberConstant(BigDecimal val) {
        return new GaussDBANumberConstant(val);
    }

    public static GaussDBAConstant createVarchar2Constant(String val) {
        return new GaussDBAVarchar2Constant(val);
    }

    public static GaussDBAConstant createDateConstant(LocalDate date) {
        return new GaussDBADateConstant(date);
    }

    public static GaussDBAConstant createTimestampConstant(LocalDateTime timestamp) {
        return new GaussDBATimestampConstant(timestamp);
    }

    // A模式无BOOLEAN类型，用NUMBER(1)模拟
    public static GaussDBAConstant createBooleanConstant(boolean val) {
        return new GaussDBANumberConstant(val ? 1 : 0);
    }

    public static GaussDBAConstant createRandomConstant(Randomly r) {
        switch (Randomly.fromOptions(0, 1, 2, 3, 4)) {
        case 0:
            return createNumberConstant(r.getInteger());
        case 1:
            return createVarchar2Constant(r.getString());
        case 2:
            return createNullConstant();
        case 3:
            return createBooleanConstant(Randomly.getBoolean());
        case 4:
            return createNumberConstant(r.getRandomBigDecimal());
        default:
            throw new AssertionError();
        }
    }

    @Override
    public GaussDBAConstant getExpectedValue() {
        return this;
    }

    // Oracle语义：空字符串被视为NULL
    public boolean isNull() {
        return false;
    }

    public boolean isNumber() {
        return false;
    }

    public boolean isString() {
        return false;
    }

    public boolean isDate() {
        return false;
    }

    public boolean isTimestamp() {
        return false;
    }

    // A模式无BOOLEAN，用NUMBER(1)模拟，isNumber可以判断布尔
    public boolean isBoolean() {
        return isNumber();
    }

    public boolean asBoolean() {
        if (isNumber()) {
            return asNumber() == 1;
        }
        throw new UnsupportedOperationException(this.toString());
    }

    public long asNumber() {
        throw new UnsupportedOperationException(this.toString());
    }

    public BigDecimal asBigDecimal() {
        throw new UnsupportedOperationException(this.toString());
    }

    public String asString() {
        throw new UnsupportedOperationException(this.toString());
    }

    public LocalDate asDate() {
        throw new UnsupportedOperationException(this.toString());
    }

    public LocalDateTime asTimestamp() {
        throw new UnsupportedOperationException(this.toString());
    }

    // 比较操作 - Oracle语义（空串=NULL）
    public abstract GaussDBAConstant isEquals(GaussDBAConstant rightVal);

    protected abstract GaussDBAConstant isLessThan(GaussDBAConstant rightVal);

    public abstract GaussDBAConstant cast(GaussDBADataType type);

    @Override
    public String toString() {
        return getTextRepresentation();
    }

    // ==================== Null Constant ====================
    public static class GaussDBANullConstant extends GaussDBAConstant {

        @Override
        public String getTextRepresentation() {
            return "NULL";
        }

        @Override
        public GaussDBADataType getExpressionType() {
            return null;
        }

        @Override
        public boolean isNull() {
            return true;
        }

        @Override
        public GaussDBAConstant isEquals(GaussDBAConstant rightVal) {
            // Oracle语义：NULL = anything 返回 NULL（包括NULL=NULL）
            return createNullConstant();
        }

        @Override
        protected GaussDBAConstant isLessThan(GaussDBAConstant rightVal) {
            // Oracle语义：NULL < anything 返回 NULL
            return createNullConstant();
        }

        @Override
        public GaussDBAConstant cast(GaussDBADataType type) {
            return createNullConstant();
        }
    }

    // ==================== Number Constant ====================
    public static class GaussDBANumberConstant extends GaussDBAConstant {

        private final BigDecimal value;

        public GaussDBANumberConstant(long value) {
            this.value = BigDecimal.valueOf(value);
        }

        public GaussDBANumberConstant(BigDecimal value) {
            this.value = value;
        }

        @Override
        public String getTextRepresentation() {
            return String.valueOf(value);
        }

        @Override
        public GaussDBADataType getExpressionType() {
            return GaussDBADataType.NUMBER;
        }

        @Override
        public boolean isNumber() {
            return true;
        }

        @Override
        public long asNumber() {
            return value.longValue();
        }

        @Override
        public BigDecimal asBigDecimal() {
            return value;
        }

        @Override
        public GaussDBAConstant isEquals(GaussDBAConstant rightVal) {
            if (rightVal.isNull()) {
                // Oracle语义：NUMBER = NULL 返回 NULL
                return createNullConstant();
            } else if (rightVal.isNumber()) {
                return createBooleanConstant(value.compareTo(rightVal.asBigDecimal()) == 0);
            } else if (rightVal.isString()) {
                // Oracle：尝试将字符串转为数字比较
                try {
                    return createBooleanConstant(value.compareTo(new BigDecimal(rightVal.asString().trim())) == 0);
                } catch (NumberFormatException e) {
                    return createBooleanConstant(false);
                }
            } else if (rightVal.isDate() || rightVal.isTimestamp()) {
                throw new IgnoreMeException();
            } else {
                // 对于不支持的类型（如BLOB），跳过比较
                throw new IgnoreMeException();
            }
        }

        @Override
        protected GaussDBAConstant isLessThan(GaussDBAConstant rightVal) {
            if (rightVal.isNull()) {
                return createNullConstant();
            } else if (rightVal.isNumber()) {
                return createBooleanConstant(value.compareTo(rightVal.asBigDecimal()) < 0);
            } else if (rightVal.isString()) {
                try {
                    return createBooleanConstant(value.compareTo(new BigDecimal(rightVal.asString().trim())) < 0);
                } catch (NumberFormatException e) {
                    throw new IgnoreMeException();
                }
            } else {
                throw new IgnoreMeException();
            }
        }

        @Override
        public GaussDBAConstant cast(GaussDBADataType type) {
            switch (type) {
            case NUMBER:
                return this;
            case VARCHAR2:
                return createVarchar2Constant(value.toString());
            case DATE:
            case TIMESTAMP:
                throw new IgnoreMeException();
            default:
                return null;
            }
        }
    }

    // ==================== Varchar2 Constant (关键：空串=NULL) ====================
    public static class GaussDBAVarchar2Constant extends GaussDBAConstant {

        private final String value;

        public GaussDBAVarchar2Constant(String value) {
            this.value = value;
        }

        @Override
        public String getTextRepresentation() {
            return "'" + value.replace("'", "''") + "'";
        }

        @Override
        public GaussDBADataType getExpressionType() {
            return GaussDBADataType.VARCHAR2;
        }

        @Override
        public boolean isString() {
            return true;
        }

        @Override
        public String asString() {
            return value;
        }

        /**
         * Oracle语义关键差异：空字符串被视为NULL
         */
        @Override
        public boolean isNull() {
            // Oracle语义：空字符串 = NULL
            return value.isEmpty();
        }

        @Override
        public GaussDBAConstant isEquals(GaussDBAConstant rightVal) {
            // Oracle语义关键：空串被视为NULL
            if (value.isEmpty()) {
                // 空串是NULL，NULL = anything 返回 NULL
                return createNullConstant();
            }

            if (rightVal.isNull()) {
                // 非空串与NULL比较返回NULL
                return createNullConstant();
            }

            if (rightVal.isString()) {
                String rightStr = rightVal.asString();
                // Oracle语义：如果右边也是空串，则是NULL
                if (rightStr.isEmpty()) {
                    return createNullConstant();  // 空串=NULL，NULL=NULL→NULL
                }
                return createBooleanConstant(value.equals(rightStr));
            } else if (rightVal.isNumber()) {
                try {
                    return createBooleanConstant(value.equals(rightVal.asBigDecimal().toString()));
                } catch (Exception e) {
                    return createBooleanConstant(false);
                }
            } else if (rightVal.isDate()) {
                return createBooleanConstant(value.equals(rightVal.asDate().toString()));
            } else if (rightVal.isTimestamp()) {
                return createBooleanConstant(value.equals(rightVal.asTimestamp().toString()));
            } else {
                // 对于不支持的类型（如BLOB），跳过比较
                throw new IgnoreMeException();
            }
        }

        @Override
        protected GaussDBAConstant isLessThan(GaussDBAConstant rightVal) {
            // Oracle语义：空串是NULL
            if (value.isEmpty()) {
                return createNullConstant();
            }

            if (rightVal.isNull()) {
                return createNullConstant();
            }

            if (rightVal.isString()) {
                String rightStr = rightVal.asString();
                if (rightStr.isEmpty()) {
                    return createNullConstant();
                }
                return createBooleanConstant(value.compareTo(rightStr) < 0);
            } else if (rightVal.isNumber()) {
                throw new IgnoreMeException();
            } else {
                throw new IgnoreMeException();
            }
        }

        @Override
        public GaussDBAConstant cast(GaussDBADataType type) {
            switch (type) {
            case VARCHAR2:
                return this;
            case NUMBER:
                try {
                    return createNumberConstant(new BigDecimal(value.trim()));
                } catch (NumberFormatException e) {
                    return createNumberConstant(-1);
                }
            case DATE:
            case TIMESTAMP:
                throw new IgnoreMeException();
            default:
                return null;
            }
        }
    }

    // ==================== Date Constant (Oracle DATE含时间) ====================
    public static class GaussDBADateConstant extends GaussDBAConstant {

        private final LocalDate date;
        private final long epochDay;

        public GaussDBADateConstant(LocalDate date) {
            this.date = date;
            this.epochDay = date.toEpochDay();
        }

        @Override
        public String getTextRepresentation() {
            // Oracle风格日期
            return "DATE '" + date.toString() + "'";
        }

        @Override
        public GaussDBADataType getExpressionType() {
            return GaussDBADataType.DATE;
        }

        @Override
        public boolean isDate() {
            return true;
        }

        @Override
        public LocalDate asDate() {
            return date;
        }

        @Override
        public GaussDBAConstant isEquals(GaussDBAConstant rightVal) {
            if (rightVal.isNull()) {
                return createNullConstant();
            } else if (rightVal.isDate()) {
                return createBooleanConstant(epochDay == ((GaussDBADateConstant) rightVal).epochDay);
            } else if (rightVal.isString()) {
                return createBooleanConstant(date.toString().equals(rightVal.asString()));
            } else if (rightVal.isTimestamp()) {
                return createBooleanConstant(date.atStartOfDay().equals(rightVal.asTimestamp()));
            }
            throw new IgnoreMeException();
        }

        @Override
        protected GaussDBAConstant isLessThan(GaussDBAConstant rightVal) {
            if (rightVal.isNull()) {
                return createNullConstant();
            } else if (rightVal.isDate()) {
                return createBooleanConstant(epochDay < ((GaussDBADateConstant) rightVal).epochDay);
            } else if (rightVal.isString()) {
                return createBooleanConstant(date.toString().compareTo(rightVal.asString()) < 0);
            } else if (rightVal.isTimestamp()) {
                return createBooleanConstant(date.atStartOfDay().isBefore(rightVal.asTimestamp()));
            }
            throw new IgnoreMeException();
        }

        @Override
        public GaussDBAConstant cast(GaussDBADataType type) {
            switch (type) {
            case DATE:
                return this;
            case VARCHAR2:
                return createVarchar2Constant(date.toString());
            case TIMESTAMP:
                return createTimestampConstant(date.atStartOfDay());
            case NUMBER:
                return createNumberConstant(epochDay);
            default:
                return null;
            }
        }
    }

    // ==================== Timestamp Constant ====================
    public static class GaussDBATimestampConstant extends GaussDBAConstant {

        private final LocalDateTime timestamp;
        private final long epochSecond;

        public GaussDBATimestampConstant(LocalDateTime timestamp) {
            this.timestamp = timestamp.withNano(0);
            this.epochSecond = this.timestamp.toEpochSecond(java.time.ZoneOffset.UTC);
        }

        @Override
        public String getTextRepresentation() {
            // Oracle风格：使用空格分隔日期和时间（不是ISO的'T'分隔符）
            // LocalDateTime.toString() 返回 "2026-10-31T17:42:04"，需要替换为 "2026-10-31 17:42:04"
            return "TIMESTAMP '" + timestamp.toString().replace('T', ' ') + "'";
        }

        @Override
        public GaussDBADataType getExpressionType() {
            return GaussDBADataType.TIMESTAMP;
        }

        @Override
        public boolean isTimestamp() {
            return true;
        }

        @Override
        public LocalDateTime asTimestamp() {
            return timestamp;
        }

        @Override
        public GaussDBAConstant isEquals(GaussDBAConstant rightVal) {
            if (rightVal.isNull()) {
                return createNullConstant();
            } else if (rightVal.isTimestamp()) {
                return createBooleanConstant(epochSecond == ((GaussDBATimestampConstant) rightVal).epochSecond);
            } else if (rightVal.isDate()) {
                return createBooleanConstant(timestamp.toLocalDate().equals(rightVal.asDate()));
            } else if (rightVal.isString()) {
                return createBooleanConstant(timestamp.toString().equals(rightVal.asString()));
            }
            throw new IgnoreMeException();
        }

        @Override
        protected GaussDBAConstant isLessThan(GaussDBAConstant rightVal) {
            if (rightVal.isNull()) {
                return createNullConstant();
            } else if (rightVal.isTimestamp()) {
                return createBooleanConstant(epochSecond < ((GaussDBATimestampConstant) rightVal).epochSecond);
            } else if (rightVal.isDate()) {
                return createBooleanConstant(timestamp.toLocalDate().isBefore(rightVal.asDate()));
            } else if (rightVal.isString()) {
                return createBooleanConstant(timestamp.toString().compareTo(rightVal.asString()) < 0);
            }
            throw new IgnoreMeException();
        }

        @Override
        public GaussDBAConstant cast(GaussDBADataType type) {
            switch (type) {
            case TIMESTAMP:
                return this;
            case DATE:
                return createDateConstant(timestamp.toLocalDate());
            case VARCHAR2:
                return createVarchar2Constant(timestamp.toString());
            case NUMBER:
                return createNumberConstant(epochSecond);
            default:
                return null;
            }
        }
    }
}