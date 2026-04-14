package sqlancer.mysql.gen;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

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

public class MySQLInsertGenerator {

    private final MySQLTable table;
    private final StringBuilder sb = new StringBuilder();
    private final ExpectedErrors errors = new ExpectedErrors();
    private final MySQLGlobalState globalState;

    public MySQLInsertGenerator(MySQLGlobalState globalState, MySQLTable table) {
        this.globalState = globalState;
        this.table = table;
    }

    public static SQLQueryAdapter insertRow(MySQLGlobalState globalState) throws SQLException {
        MySQLTable table = globalState.getSchema().getRandomTable();
        return insertRow(globalState, table);
    }

    public static SQLQueryAdapter insertRow(MySQLGlobalState globalState, MySQLTable table) throws SQLException {
        if (Randomly.getBoolean()) {
            return new MySQLInsertGenerator(globalState, table).generateInsert();
        } else {
            return new MySQLInsertGenerator(globalState, table).generateReplace();
        }
    }

    private SQLQueryAdapter generateReplace() {
        sb.append("REPLACE");
        if (Randomly.getBoolean()) {
            sb.append(" ");
            sb.append(Randomly.fromOptions("LOW_PRIORITY", "DELAYED"));
        }
        return generateInto();

    }

    private SQLQueryAdapter generateInsert() {
        sb.append("INSERT");
        if (Randomly.getBoolean()) {
            sb.append(" ");
            sb.append(Randomly.fromOptions("LOW_PRIORITY", "DELAYED", "HIGH_PRIORITY"));
        }
        if (Randomly.getBoolean()) {
            sb.append(" IGNORE");
        }
        return generateInto();
    }

    private SQLQueryAdapter generateInto() {
        sb.append(" INTO ");
        sb.append(table.getName());
        List<MySQLColumn> columns = table.getRandomNonEmptyColumnSubset();
        sb.append("(");
        sb.append(columns.stream().map(c -> c.getName()).collect(Collectors.joining(", ")));
        sb.append(") ");
        sb.append("VALUES");
        MySQLExpressionGenerator gen = new MySQLExpressionGenerator(globalState);
        boolean testDates = globalState.getDbmsSpecificOptions().testDates;
        int nrRows;
        if (Randomly.getBoolean()) {
            nrRows = 1;
        } else {
            nrRows = 1 + Randomly.smallNumber();
        }
        for (int row = 0; row < nrRows; row++) {
            if (row != 0) {
                sb.append(", ");
            }
            sb.append("(");
            for (int c = 0; c < columns.size(); c++) {
                if (c != 0) {
                    sb.append(", ");
                }
                MySQLColumn column = columns.get(c);
                if (testDates && isTemporalType(column.getType())) {
                    sb.append(MySQLVisitor.asString(generateTemporalConstant(column)));
                } else {
                    sb.append(MySQLVisitor.asString(gen.generateConstant()));
                }

            }
            sb.append(")");
        }
        MySQLErrors.addInsertUpdateErrors(errors);
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
                // keep within [0, 10^fsp) so it always renders with exactly fsp digits.
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
                return new MySQLConstant.MySQLDateTimeConstant(year, month, day, hour, minute, second, fraction, fsp);
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
                return new MySQLConstant.MySQLTimestampConstant(year, month, day, hour, minute, second, fraction, fsp);
            }
            return new MySQLConstant.MySQLTimestampConstant(year, month, day, hour, minute, second);
        }
        default:
            throw new AssertionError(type);
        }
    }

}
