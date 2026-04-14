package sqlancer.mysql.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import sqlancer.MainOptions;
import sqlancer.Randomly;
import sqlancer.mysql.MySQLGlobalState;
import sqlancer.mysql.MySQLOptions;
import sqlancer.mysql.MySQLOracleFactory;
import sqlancer.mysql.MySQLSchema.MySQLColumn;
import sqlancer.mysql.MySQLSchema.MySQLDataType;
import sqlancer.mysql.ast.MySQLColumnReference;
import sqlancer.mysql.ast.MySQLComputableFunction;
import sqlancer.mysql.ast.MySQLComputableFunction.MySQLFunction;
import sqlancer.mysql.ast.MySQLConstant;
import sqlancer.mysql.ast.MySQLExpression;

class MySQLExpressionGeneratorTemporalTest {

    private static MySQLExpressionGenerator newGenerator(boolean pqs, long seed) {
        MySQLOptions mysqlOpts = new MySQLOptions();
        mysqlOpts.testDates = true;
        mysqlOpts.oracles = pqs ? Arrays.asList(MySQLOracleFactory.PQS)
                : Arrays.asList(MySQLOracleFactory.QUERY_PARTITIONING);

        MySQLGlobalState state = new MySQLGlobalState();
        state.setMainOptions(MainOptions.DEFAULT_OPTIONS);
        state.setDbmsSpecificOptions(mysqlOpts);
        state.setRandomly(new Randomly(seed));

        return new MySQLExpressionGenerator(state);
    }

    @Test
    void generateConstant_whenTestDatesEnabled_generatesTemporalConstants_includingPQS() {
        for (boolean pqs : new boolean[] { false, true }) {
            MySQLExpressionGenerator gen = newGenerator(pqs, 1L);
            boolean sawDate = false;
            boolean sawYear = false;
            boolean sawFractionalTemporal = false;

            for (int i = 0; i < 200; i++) {
                MySQLExpression expr = gen.generateConstant();
                MySQLConstant c = expr.getExpectedValue();
                assertNotNull(c);
                if (c instanceof MySQLConstant.MySQLDateConstant) {
                    sawDate = true;
                }
                if (c instanceof MySQLConstant.MySQLYearConstant) {
                    sawYear = true;
                }
                if (c instanceof MySQLConstant.MySQLTimeConstant || c instanceof MySQLConstant.MySQLDateTimeConstant
                        || c instanceof MySQLConstant.MySQLTimestampConstant) {
                    if (c.castAsString().contains(".")) {
                        sawFractionalTemporal = true;
                    }
                }
            }

            assertTrue(sawDate, "expected at least one DATE constant (pqs=" + pqs + ")");
            assertTrue(sawYear, "expected at least one YEAR constant (pqs=" + pqs + ")");
            assertTrue(sawFractionalTemporal,
                    "expected at least one TIME/DATETIME/TIMESTAMP constant with fractional seconds (pqs=" + pqs + ")");
        }
    }

    @Test
    void generateExpression_canReferenceTemporalColumn() {
        MySQLExpressionGenerator gen = newGenerator(false, 12345L);
        MySQLColumn dateCol = new MySQLColumn("c0", MySQLDataType.DATE, false, 0);
        gen.setColumns(List.of(dateCol));

        boolean sawColumnRef = false;
        for (int i = 0; i < 200; i++) {
            MySQLExpression expr = gen.generateExpression(100); // force leaf node
            if (expr instanceof MySQLColumnReference) {
                sawColumnRef = true;
                break;
            }
        }
        assertTrue(sawColumnRef, "expected at least one MySQLColumnReference");
    }

    @Test
    void coalesce_withTemporalAndNull_keepsTemporalTypeAndQuotedText() {
        MySQLConstant date = new MySQLConstant.MySQLDateConstant(2026, 4, 7);
        MySQLConstant nul = MySQLConstant.createNullConstant();
        MySQLComputableFunction f = new MySQLComputableFunction(MySQLFunction.COALESCE, date, nul);

        MySQLConstant expected = f.getExpectedValue();
        assertNotNull(expected);
        assertEquals(MySQLDataType.DATE, expected.getType());
        assertEquals("'2026-04-07'", expected.getTextRepresentation());
    }
}

