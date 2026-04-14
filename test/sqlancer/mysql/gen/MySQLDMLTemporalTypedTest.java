package sqlancer.mysql.gen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import sqlancer.MainOptions;
import sqlancer.Randomly;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.mysql.MySQLGlobalState;
import sqlancer.mysql.MySQLOptions;
import sqlancer.mysql.MySQLOracleFactory;
import sqlancer.mysql.MySQLSchema;
import sqlancer.mysql.MySQLSchema.MySQLColumn;
import sqlancer.mysql.MySQLSchema.MySQLDataType;
import sqlancer.mysql.MySQLSchema.MySQLTable;

class MySQLDMLTemporalTypedTest {

    private static final Pattern DATE_LITERAL = Pattern.compile("'\\d{4}-\\d{2}-\\d{2}'");
    private static final Pattern YEAR_LITERAL = Pattern.compile("'\\d{4}'");
    private static final Pattern TIME_FSP6_LITERAL = Pattern.compile("'\\d{2}:\\d{2}:\\d{2}\\.\\d{6}'");
    private static final Pattern DATETIME_FSP6_LITERAL = Pattern.compile("'\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{6}'");

    private static final class TestMySQLGlobalState extends MySQLGlobalState {
        public void setSchemaDirect(MySQLSchema schema) {
            setSchema(schema);
        }
    }

    private static TestMySQLGlobalState newStateWithSingleColumnTable(MySQLDataType type, int precision, long seed) {
        MySQLOptions mysqlOpts = new MySQLOptions();
        mysqlOpts.testDates = true;
        mysqlOpts.oracles = Arrays.asList(MySQLOracleFactory.QUERY_PARTITIONING);

        TestMySQLGlobalState state = new TestMySQLGlobalState();
        state.setMainOptions(MainOptions.DEFAULT_OPTIONS);
        state.setDbmsSpecificOptions(mysqlOpts);
        state.setRandomly(new Randomly(seed));

        MySQLColumn c0 = new MySQLColumn("c0", type, false, precision);
        MySQLTable t0 = new MySQLTable("t0", List.of(c0), List.of(), MySQLTable.MySQLEngine.INNO_DB);
        c0.setTable(t0);
        state.setSchemaDirect(new MySQLSchema(List.of(t0)));
        return state;
    }

    private static void assertFinds(Pattern p, String sql) {
        assertTrue(p.matcher(sql).find(), "expected pattern " + p + " in SQL, but got: " + sql);
    }

    @Test
    void insert_date_usesDateLiteral() throws Exception {
        TestMySQLGlobalState state = newStateWithSingleColumnTable(MySQLDataType.DATE, 0, 1L);
        SQLQueryAdapter q = MySQLInsertGenerator.insertRow(state, state.getSchema().getDatabaseTables().get(0));
        assertFinds(DATE_LITERAL, q.getQueryString());
    }

    @Test
    void insert_year_usesYearLiteral() throws Exception {
        TestMySQLGlobalState state = newStateWithSingleColumnTable(MySQLDataType.YEAR, 0, 2L);
        SQLQueryAdapter q = MySQLInsertGenerator.insertRow(state, state.getSchema().getDatabaseTables().get(0));
        assertFinds(YEAR_LITERAL, q.getQueryString());
    }

    @Test
    void insert_time_fsp6_usesFractionalTimeLiteral() throws Exception {
        TestMySQLGlobalState state = newStateWithSingleColumnTable(MySQLDataType.TIME, 6, 3L);
        SQLQueryAdapter q = MySQLInsertGenerator.insertRow(state, state.getSchema().getDatabaseTables().get(0));
        assertFinds(TIME_FSP6_LITERAL, q.getQueryString());
    }

    @Test
    void insert_datetime_fsp6_usesFractionalDateTimeLiteral() throws Exception {
        TestMySQLGlobalState state = newStateWithSingleColumnTable(MySQLDataType.DATETIME, 6, 4L);
        SQLQueryAdapter q = MySQLInsertGenerator.insertRow(state, state.getSchema().getDatabaseTables().get(0));
        assertFinds(DATETIME_FSP6_LITERAL, q.getQueryString());
    }

    @Test
    void insert_timestamp_fsp6_usesFractionalTimestampLiteral() throws Exception {
        TestMySQLGlobalState state = newStateWithSingleColumnTable(MySQLDataType.TIMESTAMP, 6, 5L);
        SQLQueryAdapter q = MySQLInsertGenerator.insertRow(state, state.getSchema().getDatabaseTables().get(0));
        assertFinds(DATETIME_FSP6_LITERAL, q.getQueryString());
    }

    @Test
    void update_date_setContainsDateLiteral() throws Exception {
        TestMySQLGlobalState state = newStateWithSingleColumnTable(MySQLDataType.DATE, 0, 11L);
        SQLQueryAdapter q = MySQLUpdateGenerator.create(state);
        assertFinds(DATE_LITERAL, q.getQueryString());
    }

    @Test
    void update_year_setContainsYearLiteral() throws Exception {
        TestMySQLGlobalState state = newStateWithSingleColumnTable(MySQLDataType.YEAR, 0, 12L);
        SQLQueryAdapter q = MySQLUpdateGenerator.create(state);
        assertFinds(YEAR_LITERAL, q.getQueryString());
    }

    @Test
    void update_time_fsp6_setContainsFractionalTimeLiteral() throws Exception {
        TestMySQLGlobalState state = newStateWithSingleColumnTable(MySQLDataType.TIME, 6, 13L);
        SQLQueryAdapter q = MySQLUpdateGenerator.create(state);
        assertFinds(TIME_FSP6_LITERAL, q.getQueryString());
    }

    @Test
    void update_datetime_fsp6_setContainsFractionalDateTimeLiteral() throws Exception {
        TestMySQLGlobalState state = newStateWithSingleColumnTable(MySQLDataType.DATETIME, 6, 14L);
        SQLQueryAdapter q = MySQLUpdateGenerator.create(state);
        assertFinds(DATETIME_FSP6_LITERAL, q.getQueryString());
    }

    @Test
    void update_timestamp_fsp6_setContainsFractionalTimestampLiteral() throws Exception {
        TestMySQLGlobalState state = newStateWithSingleColumnTable(MySQLDataType.TIMESTAMP, 6, 15L);
        SQLQueryAdapter q = MySQLUpdateGenerator.create(state);
        assertFinds(DATETIME_FSP6_LITERAL, q.getQueryString());
    }

    @Test
    void delete_whenTemporalColumnPresent_forcesWhereWithTemporalLiteral() {
        TestMySQLGlobalState state = newStateWithSingleColumnTable(MySQLDataType.TIME, 6, 21L);
        SQLQueryAdapter q = MySQLDeleteGenerator.delete(state);
        String sql = q.getQueryString();
        assertTrue(sql.contains(" WHERE "), "expected WHERE clause in DELETE, but got: " + sql);
        assertFinds(TIME_FSP6_LITERAL, sql);
    }
}

