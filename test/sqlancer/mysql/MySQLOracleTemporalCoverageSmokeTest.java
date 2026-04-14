package sqlancer.mysql;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import sqlancer.MainOptions;
import sqlancer.Randomly;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.mysql.MySQLSchema.MySQLDataType;
import sqlancer.mysql.ast.MySQLConstant;
import sqlancer.mysql.ast.MySQLExpression;
import sqlancer.mysql.gen.MySQLExpressionGenerator;
import sqlancer.mysql.gen.MySQLTableGenerator;

class MySQLOracleTemporalCoverageSmokeTest {

    private static final long SEED = 123L;

    private static final Pattern TEMPORAL_KEYWORD = Pattern.compile("\\b(DATE|TIME|DATETIME|TIMESTAMP|YEAR)\\b",
            Pattern.CASE_INSENSITIVE);
    // Captures all TIME/DATETIME/TIMESTAMP column definitions that include fractional seconds precision (fsp).
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
        // No pre-existing tables: ensures no CREATE TABLE ... LIKE ... bypass in temporal mode.
        state.setSchemaDirect(new MySQLSchema(java.util.List.of()));
        return state;
    }

    private static void assertContainsTemporalKeyword(String ddl) {
        assertTrue(TEMPORAL_KEYWORD.matcher(ddl).find(), "expected temporal keyword in DDL, but got: " + ddl);
    }

    private static void assertTemporalFspIsInRange(String ddl) {
        Matcher m = FSP_TEMPORAL.matcher(ddl);
        while (m.find()) {
            int fsp = Integer.parseInt(m.group(2));
            assertTrue(fsp >= 0 && fsp <= 6,
                    "expected fsp in [0,6] but got " + fsp + " in DDL: " + ddl);
        }
    }

    @Test
    void oracleSideTemporalCoverageSmokeTest_whenTestDatesEnabled_generatedDdlIsTemporalAndFspBounded() {
        // This test is intentionally "lightweight": generate DDL strings only (no DB calls, no oracle.check()).
        // The oracle coverage matrix depends on the temporal DDL precondition enforced by MySQLTableGenerator
        // when --test-dates is enabled (modeled here via MySQLOptions.testDates).
        for (boolean pqs : new boolean[] { false, true }) {
            TestMySQLGlobalState state = newState(true, pqs, SEED);

            for (int i = 0; i < 200; i++) {
                SQLQueryAdapter q = MySQLTableGenerator.generate(state, "t" + i);
                String ddl = q.getQueryString();
                assertContainsTemporalKeyword(ddl);
                assertTemporalFspIsInRange(ddl);
            }

            // Optional add-on: expression-layer smoke test
            // (ensures temporal constants are reachable under the same --test-dates precondition).
            MySQLExpressionGenerator exprGen = new MySQLExpressionGenerator(state);
            boolean sawTemporalConstant = false;
            for (int i = 0; i < 400; i++) {
                MySQLExpression expr = exprGen.generateConstant();
                MySQLConstant c = expr.getExpectedValue();
                if (c == null) {
                    continue;
                }
                MySQLDataType type = c.getType();
                if (type == MySQLDataType.DATE || type == MySQLDataType.TIME || type == MySQLDataType.DATETIME
                        || type == MySQLDataType.TIMESTAMP || type == MySQLDataType.YEAR) {
                    sawTemporalConstant = true;
                    break;
                }
            }
            assertTrue(sawTemporalConstant, "expected at least one temporal constant when testDates=true (pqs="
                    + pqs + ")");
        }
    }
}

