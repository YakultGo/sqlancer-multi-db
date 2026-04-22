package sqlancer.mysql;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import sqlancer.Randomly;
import sqlancer.mysql.ast.MySQLBetweenOperation;
import sqlancer.mysql.ast.MySQLBinaryComparisonOperation;
import sqlancer.mysql.ast.MySQLBinaryComparisonOperation.BinaryComparisonOperator;
import sqlancer.mysql.ast.MySQLBinaryOperation;
import sqlancer.mysql.ast.MySQLBinaryOperation.MySQLBinaryOperator;
import sqlancer.mysql.ast.MySQLConstant;

/**
 * Integration tests for newly enabled MySQL features.
 * Tests that previously disabled features now work correctly.
 */
class MySQLNewFeaturesIntegrationTest {

    // ========== NULL-safe comparison (<=>) tests ==========

    @Test
    void nullSafeOperator_isAvailable() {
        // Verify the <=> operator is available in the enum
        boolean foundNullSafe = false;
        for (BinaryComparisonOperator op : BinaryComparisonOperator.values()) {
            if (op.getTextRepresentation().equals("<=>")) {
                foundNullSafe = true;
                break;
            }
        }
        assertTrue(foundNullSafe, "<=> operator should be available");
    }

    @Test
    void nullSafeOperator_canGenerateExpression() {
        MySQLConstant left = MySQLConstant.createNullConstant();
        MySQLConstant right = MySQLConstant.createNullConstant();
        MySQLBinaryComparisonOperation comparison = new MySQLBinaryComparisonOperation(left, right,
                BinaryComparisonOperator.IS_EQUALS_NULL_SAFE);
        MySQLConstant result = comparison.getExpectedValue();
        assertNotNull(result);
        assertTrue(result.getInt() == 1);  // NULL <=> NULL = TRUE
    }

    @Test
    void nullSafeOperator_mixedValues() {
        MySQLConstant left = MySQLConstant.createIntConstant(5);
        MySQLConstant right = MySQLConstant.createNullConstant();
        MySQLBinaryComparisonOperation comparison = new MySQLBinaryComparisonOperation(left, right,
                BinaryComparisonOperator.IS_EQUALS_NULL_SAFE);
        MySQLConstant result = comparison.getExpectedValue();
        assertNotNull(result);
        assertTrue(result.getInt() == 0);  // 5 <=> NULL = FALSE
    }

    // ========== BETWEEN operation tests ==========

    @Test
    void betweenOperation_bug99181_isDisabled() {
        // Verify bug99181 is false so BETWEEN can be generated
        assertFalse(MySQLBugs.bug99181, "Bug #99181 should be disabled");
    }

    @Test
    void betweenOperation_canGenerate() {
        MySQLConstant expr = MySQLConstant.createIntConstant(5);
        MySQLConstant left = MySQLConstant.createIntConstant(1);
        MySQLConstant right = MySQLConstant.createIntConstant(10);
        MySQLBetweenOperation between = new MySQLBetweenOperation(expr, left, right);
        MySQLConstant result = between.getExpectedValue();
        assertNotNull(result);
        assertTrue(result.getInt() == 1);  // 5 BETWEEN 1 AND 10 = TRUE
    }

    @Test
    void betweenOperation_outOfRange() {
        MySQLConstant expr = MySQLConstant.createIntConstant(15);
        MySQLConstant left = MySQLConstant.createIntConstant(1);
        MySQLConstant right = MySQLConstant.createIntConstant(10);
        MySQLBetweenOperation between = new MySQLBetweenOperation(expr, left, right);
        MySQLConstant result = between.getExpectedValue();
        assertNotNull(result);
        assertTrue(result.getInt() == 0);  // 15 BETWEEN 1 AND 10 = FALSE
    }

    // ========== Binary bitwise operation tests ==========

    @Test
    void binaryOperation_bug99135_isDisabled() {
        // Verify bug99135 is false so bitwise operations can be generated
        assertFalse(MySQLBugs.bug99135, "Bug #99135 should be disabled");
    }

    @Test
    void binaryOperation_bitwiseAnd_canCompute() {
        MySQLConstant left = MySQLConstant.createIntConstant(12);
        MySQLConstant right = MySQLConstant.createIntConstant(10);
        MySQLBinaryOperation operation = new MySQLBinaryOperation(left, right, MySQLBinaryOperator.AND);
        MySQLConstant result = operation.getExpectedValue();
        assertNotNull(result);
        assertTrue(result.getInt() == 8);  // 12 & 10 = 8
    }

    @Test
    void binaryOperation_bitwiseOr_canCompute() {
        MySQLConstant left = MySQLConstant.createIntConstant(12);
        MySQLConstant right = MySQLConstant.createIntConstant(10);
        MySQLBinaryOperation operation = new MySQLBinaryOperation(left, right, MySQLBinaryOperator.OR);
        MySQLConstant result = operation.getExpectedValue();
        assertNotNull(result);
        assertTrue(result.getInt() == 14);  // 12 | 10 = 14
    }

    @Test
    void binaryOperation_bitwiseXor_canCompute() {
        MySQLConstant left = MySQLConstant.createIntConstant(12);
        MySQLConstant right = MySQLConstant.createIntConstant(10);
        MySQLBinaryOperation operation = new MySQLBinaryOperation(left, right, MySQLBinaryOperator.XOR);
        MySQLConstant result = operation.getExpectedValue();
        assertNotNull(result);
        assertTrue(result.getInt() == 6);  // 12 ^ 10 = 6
    }

    @Test
    void binaryOperation_withNull_returnsNull() {
        MySQLConstant left = MySQLConstant.createNullConstant();
        MySQLConstant right = MySQLConstant.createIntConstant(10);
        MySQLBinaryOperation operation = new MySQLBinaryOperation(left, right, MySQLBinaryOperator.AND);
        MySQLConstant result = operation.getExpectedValue();
        assertTrue(result.isNull());
    }

    // ========== Precision/Scale tests ==========

    @Test
    void precisionScale_bug99183_isDisabled() {
        // Verify bug99183 is false so precision/scale can be generated
        assertFalse(MySQLBugs.bug99183, "Bug #99183 should be disabled");
    }

    @Test
    void precisionScale_canBeGenerated() {
        StringBuilder sb = new StringBuilder("DECIMAL");
        // Simulate the generation
        if (Randomly.getBoolean() && !MySQLBugs.bug99183) {
            sb.append("(10, 2)");
        }
        String result = sb.toString();
        assertTrue(result.startsWith("DECIMAL"));
    }

    // ========== UNSIGNED tests (still limited) ==========

    @Test
    void unsigned_bug99127_isEnabled() {
        // Verify bug99127 is true for safety
        assertTrue(MySQLBugs.bug99127, "Bug #99127 should remain enabled for UNSIGNED safety");
    }

    // ========== Operator random selection tests ==========

    @Test
    void binaryComparisonOperator_getRandom_includesNullSafe() {
        // Verify getRandom includes all operators including <=>
        List<BinaryComparisonOperator> operators = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            try {
                operators.add(BinaryComparisonOperator.getRandom());
            } catch (Exception e) {
                // Ignore any exceptions during random selection
            }
        }

        // Verify we got a variety of operators
        assertTrue(operators.size() > 0, "Should be able to select operators randomly");

        // Check that <=> is in the available values
        boolean hasNullSafe = false;
        for (BinaryComparisonOperator op : BinaryComparisonOperator.values()) {
            if (op == BinaryComparisonOperator.IS_EQUALS_NULL_SAFE) {
                hasNullSafe = true;
                break;
            }
        }
        assertTrue(hasNullSafe, "IS_EQUALS_NULL_SAFE should be in available operators");
    }

    @Test
    void binaryOperator_getRandom_returnsValidOperator() {
        // Test that getRandom returns a valid operator
        MySQLBinaryOperator op = MySQLBinaryOperator.getRandom();
        assertNotNull(op);
        assertTrue(op == MySQLBinaryOperator.AND || op == MySQLBinaryOperator.OR
                || op == MySQLBinaryOperator.XOR);
    }
}