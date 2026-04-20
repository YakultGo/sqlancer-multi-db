package sqlancer.mysql.gen;

import java.sql.SQLException;
import java.util.List;

import sqlancer.IgnoreMeException;
import sqlancer.Randomly;
import sqlancer.common.gen.AbstractUpdateGenerator;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.mysql.MySQLErrors;
import sqlancer.mysql.MySQLGlobalState;
import sqlancer.mysql.MySQLSchema.MySQLDataType;
import sqlancer.mysql.MySQLSchema.MySQLColumn;
import sqlancer.mysql.MySQLSchema.MySQLTable;
import sqlancer.mysql.ast.MySQLConstant;
import sqlancer.mysql.MySQLVisitor;

public class MySQLUpdateGenerator extends AbstractUpdateGenerator<MySQLColumn> {

    private final MySQLGlobalState globalState;
    private MySQLExpressionGenerator gen;

    public MySQLUpdateGenerator(MySQLGlobalState globalState) {
        this.globalState = globalState;
    }

    public static SQLQueryAdapter create(MySQLGlobalState globalState) throws SQLException {
        return new MySQLUpdateGenerator(globalState).generate();
    }

    private SQLQueryAdapter generate() throws SQLException {
        MySQLTable table = globalState.getSchema().getRandomTable(t -> !t.isView());
        if (table == null) {
            throw new IgnoreMeException();
        }
        List<MySQLColumn> columns = table.getRandomNonEmptyColumnSubset();
        gen = new MySQLExpressionGenerator(globalState).setColumns(table.getColumns());
        sb.append("UPDATE ");
        sb.append(table.getName());
        sb.append(" SET ");
        updateColumns(columns);
        if (Randomly.getBoolean()) {
            sb.append(" WHERE ");
            MySQLErrors.addExpressionErrors(errors);
            sb.append(MySQLVisitor.asString(gen.generateExpression()));
        }
        MySQLErrors.addInsertUpdateErrors(errors);
        errors.add("doesn't have this option");

        return new SQLQueryAdapter(sb.toString(), errors);
    }

    @Override
    protected void updateValue(MySQLColumn column) {
        if (globalState.getDbmsSpecificOptions().testDates && isTemporalType(column.getType())) {
            sb.append(MySQLVisitor.asString(generateTemporalConstant(column)));
            return;
        }
        if (Randomly.getBoolean()) {
            sb.append(gen.generateConstant());
        } else if (Randomly.getBoolean()) {
            sb.append("DEFAULT");
        } else {
            sb.append(MySQLVisitor.asString(gen.generateExpression()));
        }
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
