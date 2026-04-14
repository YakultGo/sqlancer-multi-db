package sqlancer.mysql.gen;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import sqlancer.MainOptions;
import sqlancer.Randomly;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.mysql.MySQLGlobalState;
import sqlancer.mysql.MySQLOptions;
import sqlancer.mysql.MySQLOracleFactory;
import sqlancer.mysql.MySQLSchema;

class MySQLTableGeneratorTemporalDDLTest {

    private static final Pattern TEMPORAL_KEYWORD = Pattern.compile("\\b(DATE|TIME|DATETIME|TIMESTAMP|YEAR)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FSP_TEMPORAL = Pattern.compile("\\b(TIME|DATETIME|TIMESTAMP)\\s*\\(\\s*(\\d)\\s*\\)",
            Pattern.CASE_INSENSITIVE);

    private static final class TestMySQLGlobalState extends MySQLGlobalState {
        public void setSchemaDirect(MySQLSchema schema) {
            setSchema(schema);
        }
    }

    private static TestMySQLGlobalState newState(boolean testDates, boolean pqs, long seed) {
        MySQLOptions mysqlOpts = new MySQLOptions();
        mysqlOpts.testDates = testDates;
        mysqlOpts.oracles = pqs ? Arrays.asList(MySQLOracleFactory.PQS)
                : Arrays.asList(MySQLOracleFactory.QUERY_PARTITIONING);

        TestMySQLGlobalState state = new TestMySQLGlobalState();
        state.setMainOptions(MainOptions.DEFAULT_OPTIONS);
        state.setDbmsSpecificOptions(mysqlOpts);
        state.setRandomly(new Randomly(seed));
        state.setSchemaDirect(new MySQLSchema(List.of()));
        return state;
    }

    private static void assertContainsTemporalKeyword(String ddl) {
        assertTrue(TEMPORAL_KEYWORD.matcher(ddl).find(), "expected temporal keyword in DDL, but got: " + ddl);
    }

    private static void assertTemporalFspValidIfPresent(String ddl) {
        Matcher m = FSP_TEMPORAL.matcher(ddl);
        while (m.find()) {
            int fsp = Integer.parseInt(m.group(2));
            assertTrue(fsp >= 0 && fsp <= 6, "expected fsp in [0,6] but got " + fsp + " in DDL: " + ddl);
        }
    }

    @Test
    void generate_whenTestDatesEnabled_mustContainTemporalColumn_andFspForFractionalTemporal() {
        TestMySQLGlobalState state = newState(true, false, 1L);
        SQLQueryAdapter q = MySQLTableGenerator.generate(state, "t0");
        String ddl = q.getQueryString();

        assertContainsTemporalKeyword(ddl);
        assertTemporalFspValidIfPresent(ddl);
    }

    @Test
    void generate_whenTestDatesDisabled_doesNotThrow() {
        TestMySQLGlobalState state = newState(false, false, 2L);
        assertDoesNotThrow(() -> MySQLTableGenerator.generate(state, "t0"));
    }

    @Test
    void generate_whenTestDatesEnabled_inPQSMode_stillContainsTemporalColumn() {
        TestMySQLGlobalState state = newState(true, true, 3L);
        SQLQueryAdapter q = MySQLTableGenerator.generate(state, "t0");
        String ddl = q.getQueryString();

        assertContainsTemporalKeyword(ddl);
        assertTemporalFspValidIfPresent(ddl);
    }
}

