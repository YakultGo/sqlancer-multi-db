package sqlancer.mysql.ast;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import sqlancer.IgnoreMeException;
import sqlancer.mysql.MySQLSchema.MySQLDataType;
import sqlancer.mysql.MySQLToStringVisitor;
import sqlancer.mysql.ast.MySQLTemporalUtil.IntervalUnit;
import sqlancer.mysql.ast.MySQLTemporalUtil.IntervalValue;

/**
 * Represents MySQL temporal functions that use INTERVAL syntax,
 * such as DATE_ADD, DATE_SUB, ADDDATE, SUBDATE, etc.
 */
public final class MySQLTemporalFunction implements MySQLExpression {

    public enum TemporalFunctionKind {
        DATE_ADD("DATE_ADD"),
        DATE_SUB("DATE_SUB"),
        ADDDATE("ADDDATE"),
        SUBDATE("SUBDATE"),
        TIMESTAMPADD("TIMESTAMPADD"),
        TIMESTAMPDIFF("TIMESTAMPDIFF");

        private final String functionName;

        TemporalFunctionKind(String functionName) {
            this.functionName = functionName;
        }

        public String getFunctionName() {
            return functionName;
        }

        public static TemporalFunctionKind getRandom() {
            return sqlancer.Randomly.fromOptions(values());
        }
    }

    private final TemporalFunctionKind kind;
    private final MySQLExpression temporalExpr;
    private final MySQLExpression intervalExpr;
    private final String intervalUnit; // INTERVAL unit as string
    private final MySQLDataType returnType;

    public MySQLTemporalFunction(TemporalFunctionKind kind, MySQLExpression temporalExpr,
            MySQLExpression intervalExpr, String intervalUnit, MySQLDataType returnType) {
        this.kind = kind;
        this.temporalExpr = temporalExpr;
        this.intervalExpr = intervalExpr;
        this.intervalUnit = intervalUnit;
        this.returnType = returnType;
    }

    public TemporalFunctionKind getKind() {
        return kind;
    }

    public MySQLExpression getTemporalExpr() {
        return temporalExpr;
    }

    public MySQLExpression getIntervalExpr() {
        return intervalExpr;
    }

    public String getIntervalUnit() {
        return intervalUnit;
    }

    public MySQLDataType getReturnType() {
        return returnType;
    }

    @Override
    public MySQLConstant getExpectedValue() {
        MySQLConstant temporalValue = temporalExpr.getExpectedValue();
        MySQLConstant intervalValue = intervalExpr.getExpectedValue();

        if (temporalValue == null || temporalValue.isNull() || intervalValue == null || intervalValue.isNull()) {
            return MySQLConstant.createNullConstant();
        }

        String temporalStr = temporalValue.castAsString();
        long intervalNum = intervalValue.castAs(MySQLCastOperation.CastType.SIGNED).getInt();

        try {
            IntervalUnit unit = IntervalUnit.fromString(intervalUnit);
            IntervalValue interval = MySQLTemporalUtil.parseInterval(intervalNum, unit);

            switch (kind) {
            case DATE_ADD:
            case ADDDATE:
                return addInterval(temporalStr, interval, temporalValue);
            case DATE_SUB:
            case SUBDATE:
                return subtractInterval(temporalStr, interval, temporalValue);
            case TIMESTAMPADD:
                return addInterval(temporalStr, interval, temporalValue);
            case TIMESTAMPDIFF:
                // TIMESTAMPDIFF returns a number, not a temporal value
                throw new IgnoreMeException();
            default:
                throw new IgnoreMeException();
            }
        } catch (IgnoreMeException e) {
            throw e;
        } catch (Exception e) {
            throw new IgnoreMeException();
        }
    }

    private MySQLConstant addInterval(String temporalStr, IntervalValue interval, MySQLConstant original) {
        MySQLDataType type = original.getType();

        switch (type) {
        case DATE:
            LocalDate dateResult = MySQLTemporalUtil.addIntervalToDate(
                    MySQLTemporalUtil.parseDate(temporalStr), interval);
            return new MySQLConstant.MySQLDateConstant(
                    dateResult.getYear(),
                    dateResult.getMonthValue(),
                    dateResult.getDayOfMonth());
        case DATETIME:
            LocalDateTime dateTimeResult = MySQLTemporalUtil.addIntervalToDateTime(
                    MySQLTemporalUtil.parseDateTime(temporalStr), interval);
            return new MySQLConstant.MySQLDateTimeConstant(
                    dateTimeResult.getYear(),
                    dateTimeResult.getMonthValue(),
                    dateTimeResult.getDayOfMonth(),
                    dateTimeResult.getHour(),
                    dateTimeResult.getMinute(),
                    dateTimeResult.getSecond());
        case TIMESTAMP:
            LocalDateTime timestampResult = MySQLTemporalUtil.addIntervalToDateTime(
                    MySQLTemporalUtil.parseDateTime(temporalStr), interval);
            return new MySQLConstant.MySQLTimestampConstant(
                    timestampResult.getYear(),
                    timestampResult.getMonthValue(),
                    timestampResult.getDayOfMonth(),
                    timestampResult.getHour(),
                    timestampResult.getMinute(),
                    timestampResult.getSecond());
        case TIME:
            LocalTime timeResult = MySQLTemporalUtil.addIntervalToTime(
                    MySQLTemporalUtil.parseTime(temporalStr), interval);
            return new MySQLConstant.MySQLTimeConstant(
                    timeResult.getHour(),
                    timeResult.getMinute(),
                    timeResult.getSecond());
        case VARCHAR:
            // String might be interpreted as datetime
            try {
                LocalDateTime dtResult = MySQLTemporalUtil.addIntervalToDateTime(
                        MySQLTemporalUtil.parseTemporal(temporalStr), interval);
                return new MySQLConstant.MySQLDateTimeConstant(
                        dtResult.getYear(),
                        dtResult.getMonthValue(),
                        dtResult.getDayOfMonth(),
                        dtResult.getHour(),
                        dtResult.getMinute(),
                        dtResult.getSecond());
            } catch (IgnoreMeException e) {
                // Might be a date
                try {
                    LocalDate dResult = MySQLTemporalUtil.addIntervalToDate(
                            MySQLTemporalUtil.parseDate(temporalStr), interval);
                    return new MySQLConstant.MySQLDateConstant(
                            dResult.getYear(),
                            dResult.getMonthValue(),
                            dResult.getDayOfMonth());
                } catch (IgnoreMeException e2) {
                    throw new IgnoreMeException();
                }
            }
        default:
            throw new IgnoreMeException();
        }
    }

    private MySQLConstant subtractInterval(String temporalStr, IntervalValue interval, MySQLConstant original) {
        return addInterval(temporalStr, interval.negate(), original);
    }

    public MySQLDataType getExpressionType() {
        return returnType;
    }

    /**
     * Generate SQL representation for this temporal function.
     */
    public String asString() {
        MySQLToStringVisitor visitor = new MySQLToStringVisitor();
        visitor.visit(this);
        return visitor.get();
    }
}