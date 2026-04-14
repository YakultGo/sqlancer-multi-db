package sqlancer.mysql.gen;

import java.util.Arrays;

import sqlancer.Randomly;
import sqlancer.common.query.ExpectedErrors;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.mysql.MySQLErrors;
import sqlancer.mysql.MySQLGlobalState;
import sqlancer.mysql.MySQLSchema.MySQLDataType;
import sqlancer.mysql.MySQLSchema.MySQLColumn;
import sqlancer.mysql.MySQLSchema.MySQLTable;
import sqlancer.mysql.ast.MySQLConstant;
import sqlancer.mysql.MySQLVisitor;

public class MySQLDeleteGenerator {

    private final StringBuilder sb = new StringBuilder();
    private final MySQLGlobalState globalState;

    public MySQLDeleteGenerator(MySQLGlobalState globalState) {
        this.globalState = globalState;
    }

    public static SQLQueryAdapter delete(MySQLGlobalState globalState) {
        return new MySQLDeleteGenerator(globalState).generate();
    }

    private SQLQueryAdapter generate() {
        MySQLTable randomTable = globalState.getSchema().getRandomTable();
        MySQLExpressionGenerator gen = new MySQLExpressionGenerator(globalState).setColumns(randomTable.getColumns());
        ExpectedErrors errors = new ExpectedErrors();
        boolean testDates = globalState.getDbmsSpecificOptions().testDates;
        boolean hasTemporalColumn = randomTable.getColumns().stream().anyMatch(c -> isTemporalType(c.getType()));

        sb.append("DELETE");
        if (Randomly.getBoolean()) {
            sb.append(" LOW_PRIORITY");
        }
        if (Randomly.getBoolean()) {
            sb.append(" QUICK");
        }
        if (Randomly.getBoolean()) {
            sb.append(" IGNORE");
        }
        // TODO: support partitions
        sb.append(" FROM ");
        sb.append(randomTable.getName());
        if (testDates && hasTemporalColumn) {
            sb.append(" WHERE ");
            MySQLErrors.addExpressionErrors(errors);
            MySQLColumn temporalColumn = randomTable.getColumns().stream().filter(c -> isTemporalType(c.getType()))
                    .findFirst()
                    .orElseThrow();
            sb.append(temporalColumn.getFullQualifiedName());
            sb.append(" = ");
            sb.append(generateTemporalConstant(temporalColumn).getTextRepresentation());
        } else if (Randomly.getBoolean()) {
            sb.append(" WHERE ");
            sb.append(MySQLVisitor.asString(gen.generateExpression()));
            MySQLErrors.addExpressionErrors(errors);
        }
        errors.addAll(Arrays.asList("doesn't have this option",
                "Truncated incorrect DOUBLE value" /*
                                                    * ignore as a workaround for https://bugs.mysql.com/bug.php?id=95997
                                                    */, "Truncated incorrect INTEGER value",
                "Truncated incorrect DECIMAL value", "Data truncated for functional index"));
        // TODO: support ORDER BY
        return new SQLQueryAdapter(sb.toString(), errors);
    }

    private static boolean isTemporalType(MySQLDataType type) {
        switch (type) {
        case DATE:
        case TIME:
        case DATETIME:
        case TIMESTAMP:
        case YEAR:
            return true;
        default:
            return false;
        }
    }

    private static int clampFsp(int precision) {
        if (precision < 0) {
            return 0;
        }
        return Math.min(6, precision);
    }

    private static MySQLConstant generateTemporalConstant(MySQLColumn column) {
        int fsp = clampFsp(column.getPrecision());
        MySQLDataType type = column.getType();
        switch (type) {
        case DATE: {
            int year = (int) Randomly.getNotCachedInteger(1901, 2155);
            int month = (int) Randomly.getNotCachedInteger(1, 12 + 1);
            int day = (int) Randomly.getNotCachedInteger(1, 28 + 1); // keep always-valid across months
            return new MySQLConstant.MySQLDateConstant(year, month, day);
        }
        case YEAR: {
            int year = (int) Randomly.getNotCachedInteger(1901, 2155);
            return new MySQLConstant.MySQLYearConstant(year);
        }
        case TIME: {
            int hour = (int) Randomly.getNotCachedInteger(0, 23 + 1);
            int minute = (int) Randomly.getNotCachedInteger(0, 59 + 1);
            int second = (int) Randomly.getNotCachedInteger(0, 59 + 1);
            if (fsp > 0) {
                int fraction = (int) Randomly.getNotCachedInteger(0, 999999 + 1);
                return new MySQLConstant.MySQLTimeConstant(hour, minute, second, fraction, fsp);
            }
            return new MySQLConstant.MySQLTimeConstant(hour, minute, second);
        }
        case DATETIME: {
            int year = (int) Randomly.getNotCachedInteger(1901, 2155);
            int month = (int) Randomly.getNotCachedInteger(1, 12 + 1);
            int day = (int) Randomly.getNotCachedInteger(1, 28 + 1);
            int hour = (int) Randomly.getNotCachedInteger(0, 23 + 1);
            int minute = (int) Randomly.getNotCachedInteger(0, 59 + 1);
            int second = (int) Randomly.getNotCachedInteger(0, 59 + 1);
            if (fsp > 0) {
                int fraction = (int) Randomly.getNotCachedInteger(0, 999999 + 1);
                return new MySQLConstant.MySQLDateTimeConstant(year, month, day, hour, minute, second, fraction,
                        fsp);
            }
            return new MySQLConstant.MySQLDateTimeConstant(year, month, day, hour, minute, second);
        }
        case TIMESTAMP: {
            int year = (int) Randomly.getNotCachedInteger(1901, 2155);
            int month = (int) Randomly.getNotCachedInteger(1, 12 + 1);
            int day = (int) Randomly.getNotCachedInteger(1, 28 + 1);
            int hour = (int) Randomly.getNotCachedInteger(0, 23 + 1);
            int minute = (int) Randomly.getNotCachedInteger(0, 59 + 1);
            int second = (int) Randomly.getNotCachedInteger(0, 59 + 1);
            if (fsp > 0) {
                int fraction = (int) Randomly.getNotCachedInteger(0, 999999 + 1);
                return new MySQLConstant.MySQLTimestampConstant(year, month, day, hour, minute, second, fraction,
                        fsp);
            }
            return new MySQLConstant.MySQLTimestampConstant(year, month, day, hour, minute, second);
        }
        default:
            throw new AssertionError(type);
        }
    }

}
