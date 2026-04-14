package sqlancer.mysql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import sqlancer.SQLConnection;
import sqlancer.mysql.MySQLSchema.MySQLColumn;
import sqlancer.mysql.MySQLSchema.MySQLDataType;
import sqlancer.mysql.MySQLSchema.MySQLRowValue;
import sqlancer.mysql.MySQLSchema.MySQLTable;
import sqlancer.mysql.MySQLSchema.MySQLTables;
import sqlancer.mysql.ast.MySQLConstant;

/**
 * Hermetic tests for {@link MySQLTables#getRandomRowValue(SQLConnection)} temporal column handling.
 * Uses JDK dynamic proxies (no Mockito) to supply a controlled {@link ResultSet}.
 */
class MySQLRowValueTemporalExtractionTest {

    private static final String TABLE = "t";

    /** One column "c" on table {@value #TABLE} → alias {@code tc} as in {@link MySQLTables#getRandomRowValue}. */
    private static MySQLTables tablesWithSingleColumn(MySQLDataType type) {
        MySQLColumn col = new MySQLColumn("c", type, false, 0);
        MySQLTable table = new MySQLTable(TABLE, new ArrayList<>(Collections.singletonList(col)),
                Collections.emptyList(), MySQLTable.MySQLEngine.INNO_DB);
        col.setTable(table);
        return new MySQLTables(Collections.singletonList(table));
    }

    private static String columnAlias() {
        return TABLE + "c";
    }

    private static SQLConnection connectionWithResultSet(ResultSet rs) {
        Statement stmt = (Statement) Proxy.newProxyInstance(Statement.class.getClassLoader(),
                new Class<?>[] { Statement.class }, new StatementHandler(rs));
        Connection conn = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class<?>[] { Connection.class }, new ConnectionHandler(stmt));
        return new SQLConnection(conn);
    }

    private static final class ConnectionHandler implements InvocationHandler {
        private final Statement statement;

        ConnectionHandler(Statement statement) {
            this.statement = statement;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String n = method.getName();
            if ("createStatement".equals(n)) {
                return statement;
            }
            if ("close".equals(n)) {
                return null;
            }
            if ("isClosed".equals(n)) {
                return false;
            }
            throw new UnsupportedOperationException(n);
        }
    }

    private static final class StatementHandler implements InvocationHandler {
        private final ResultSet rs;

        StatementHandler(ResultSet rs) {
            this.rs = rs;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String n = method.getName();
            if ("executeQuery".equals(n)) {
                return rs;
            }
            if ("close".equals(n)) {
                return null;
            }
            if ("isClosed".equals(n)) {
                return false;
            }
            throw new UnsupportedOperationException(n);
        }
    }

    private static ResultSet fakeResultSet(FakeRow row) {
        return (ResultSet) Proxy.newProxyInstance(ResultSet.class.getClassLoader(), new Class<?>[] { ResultSet.class },
                new ResultSetHandler(row));
    }

    private static final class ResultSetHandler implements InvocationHandler {
        private final FakeRow row;
        private final AtomicInteger nextCalls = new AtomicInteger();

        ResultSetHandler(FakeRow row) {
            this.row = row;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String n = method.getName();
            if ("next".equals(n)) {
                return nextCalls.getAndIncrement() == 0;
            }
            if ("findColumn".equals(n)) {
                String label = (String) args[0];
                assertEquals(columnAlias(), label);
                return 1;
            }
            if ("getString".equals(n)) {
                int col = (Integer) args[0];
                assertEquals(1, col);
                return row.stringValue;
            }
            if ("getDate".equals(n)) {
                int col = (Integer) args[0];
                assertEquals(1, col);
                return row.dateValue;
            }
            if ("getTime".equals(n)) {
                int col = (Integer) args[0];
                assertEquals(1, col);
                return row.timeValue;
            }
            if ("getTimestamp".equals(n)) {
                int col = (Integer) args[0];
                assertEquals(1, col);
                return row.timestampValue;
            }
            if ("getInt".equals(n)) {
                int col = (Integer) args[0];
                assertEquals(1, col);
                return row.intValue;
            }
            if ("close".equals(n)) {
                return null;
            }
            throw new UnsupportedOperationException(n);
        }
    }

    private static final class FakeRow {
        final String stringValue;
        final Date dateValue;
        final Time timeValue;
        final Timestamp timestampValue;
        final int intValue;

        FakeRow(String stringValue, Date dateValue, Time timeValue, Timestamp timestampValue, int intValue) {
            this.stringValue = stringValue;
            this.dateValue = dateValue;
            this.timeValue = timeValue;
            this.timestampValue = timestampValue;
            this.intValue = intValue;
        }

        static FakeRow forDate() {
            return new FakeRow("2024-06-01", Date.valueOf("2024-06-01"), null, null, 0);
        }

        static FakeRow forTime() {
            return new FakeRow("14:05:06", null, Time.valueOf("14:05:06"), null, 0);
        }

        static FakeRow forDatetime() {
            Timestamp ts = Timestamp.valueOf("2024-06-01 14:05:06");
            return new FakeRow("2024-06-01 14:05:06", null, null, ts, 0);
        }

        static FakeRow forTimestamp() {
            Timestamp ts = Timestamp.valueOf("2024-06-01 14:05:06");
            return new FakeRow("2024-06-01 14:05:06", null, null, ts, 0);
        }

        static FakeRow forYear() {
            return new FakeRow("2024", null, null, null, 2024);
        }

        static FakeRow forNull() {
            return new FakeRow(null, null, null, null, 0);
        }
    }

    /** Unwraps {@link MySQLConstant#createStringConstant} quoting (random ' or "). */
    private static String innerLiteralText(String textRepresentation) {
        assertNotNull(textRepresentation);
        assertTrue(textRepresentation.length() >= 2, () -> textRepresentation);
        char q = textRepresentation.charAt(0);
        assertTrue(q == '\'' || q == '"', () -> textRepresentation);
        assertEquals(q, textRepresentation.charAt(textRepresentation.length() - 1));
        return textRepresentation.substring(1, textRepresentation.length() - 1).replace("" + q + q, "" + q);
    }

    @Test
    void getRandomRowValue_date_yyyyMmDd() throws SQLException {
        MySQLTables tables = tablesWithSingleColumn(MySQLDataType.DATE);
        ResultSet rs = fakeResultSet(FakeRow.forDate());
        MySQLRowValue row = tables.getRandomRowValue(connectionWithResultSet(rs));
        MySQLColumn col = tables.getColumns().get(0);
        String repr = row.getValues().get(col).getTextRepresentation();
        String inner = innerLiteralText(repr);
        assertTrue(inner.matches("\\d{4}-\\d{2}-\\d{2}"), inner);
        assertEquals("2024-06-01", inner);
    }

    @Test
    void getRandomRowValue_time_containsColons() throws SQLException {
        MySQLTables tables = tablesWithSingleColumn(MySQLDataType.TIME);
        ResultSet rs = fakeResultSet(FakeRow.forTime());
        MySQLRowValue row = tables.getRandomRowValue(connectionWithResultSet(rs));
        MySQLColumn col = tables.getColumns().get(0);
        String inner = innerLiteralText(row.getValues().get(col).getTextRepresentation());
        assertTrue(inner.contains(":"), inner);
    }

    @Test
    void getRandomRowValue_datetime_containsSpaceBetweenDateAndTime() throws SQLException {
        MySQLTables tables = tablesWithSingleColumn(MySQLDataType.DATETIME);
        ResultSet rs = fakeResultSet(FakeRow.forDatetime());
        MySQLRowValue row = tables.getRandomRowValue(connectionWithResultSet(rs));
        MySQLColumn col = tables.getColumns().get(0);
        String inner = innerLiteralText(row.getValues().get(col).getTextRepresentation());
        assertTrue(inner.contains(" "), inner);
        assertTrue(inner.contains(":"), inner);
    }

    @Test
    void getRandomRowValue_timestamp_containsSpaceBetweenDateAndTime() throws SQLException {
        MySQLTables tables = tablesWithSingleColumn(MySQLDataType.TIMESTAMP);
        ResultSet rs = fakeResultSet(FakeRow.forTimestamp());
        MySQLRowValue row = tables.getRandomRowValue(connectionWithResultSet(rs));
        MySQLColumn col = tables.getColumns().get(0);
        String inner = innerLiteralText(row.getValues().get(col).getTextRepresentation());
        assertTrue(inner.contains(" "), inner);
        assertTrue(inner.contains(":"), inner);
    }

    @Test
    void getRandomRowValue_year_fourDigits() throws SQLException {
        MySQLTables tables = tablesWithSingleColumn(MySQLDataType.YEAR);
        ResultSet rs = fakeResultSet(FakeRow.forYear());
        MySQLRowValue row = tables.getRandomRowValue(connectionWithResultSet(rs));
        MySQLColumn col = tables.getColumns().get(0);
        String inner = innerLiteralText(row.getValues().get(col).getTextRepresentation());
        assertEquals(4, inner.length());
        assertTrue(inner.chars().allMatch(Character::isDigit), inner);
        assertEquals("2024", inner);
    }

    @Test
    void getRandomRowValue_temporalNull_mapsToNullConstant() throws SQLException {
        MySQLTables tables = tablesWithSingleColumn(MySQLDataType.DATE);
        ResultSet rs = fakeResultSet(FakeRow.forNull());
        MySQLRowValue row = tables.getRandomRowValue(connectionWithResultSet(rs));
        MySQLColumn col = tables.getColumns().get(0);
        MySQLConstant c = row.getValues().get(col);
        assertTrue(c.isNull());
        assertEquals("NULL", c.getTextRepresentation());
    }

    @Test
    void getRandomRowValue_temporalNull_stringLiteralNotUsed() throws SQLException {
        MySQLTables tables = tablesWithSingleColumn(MySQLDataType.DATE);
        ResultSet rs = fakeResultSet(FakeRow.forNull());
        MySQLRowValue row = tables.getRandomRowValue(connectionWithResultSet(rs));
        String repr = row.getValues().get(tables.getColumns().get(0)).getTextRepresentation();
        assertFalse(repr.startsWith("'"));
        assertFalse(repr.startsWith("\""));
    }
}
