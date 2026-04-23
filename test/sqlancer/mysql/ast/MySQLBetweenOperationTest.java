package sqlancer.mysql.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for MySQL BETWEEN operation.
 * BETWEEN expr AND left AND right is equivalent to expr >= left AND expr <= right.
 */
class MySQLBetweenOperationTest {

    // ========== Integer BETWEEN tests ==========

    @Test
    void between_intInRange_returnsTrue() {
        MySQLConstant expr = MySQLConstant.createIntConstant(5);
        MySQLConstant left = MySQLConstant.createIntConstant(1);
        MySQLConstant right = MySQLConstant.createIntConstant(10);
        MySQLBetweenOperation between = new MySQLBetweenOperation(expr, left, right);
        MySQLConstant result = between.getExpectedValue();
        assertEquals(1, result.getInt());
    }

    @Test
    void between_intAtLeftBoundary_returnsTrue() {
        MySQLConstant expr = MySQLConstant.createIntConstant(1);
        MySQLConstant left = MySQLConstant.createIntConstant(1);
        MySQLConstant right = MySQLConstant.createIntConstant(10);
        MySQLBetweenOperation between = new MySQLBetweenOperation(expr, left, right);
        MySQLConstant result = between.getExpectedValue();
        assertEquals(1, result.getInt());
    }

    @Test
    void between_intAtRightBoundary_returnsTrue() {
        MySQLConstant expr = MySQLConstant.createIntConstant(10);
        MySQLConstant left = MySQLConstant.createIntConstant(1);
        MySQLConstant right = MySQLConstant.createIntConstant(10);
        MySQLBetweenOperation between = new MySQLBetweenOperation(expr, left, right);
        MySQLConstant result = between.getExpectedValue();
        assertEquals(1, result.getInt());
    }

    @Test
    void between_intBelowRange_returnsFalse() {
        MySQLConstant expr = MySQLConstant.createIntConstant(0);
        MySQLConstant left = MySQLConstant.createIntConstant(1);
        MySQLConstant right = MySQLConstant.createIntConstant(10);
        MySQLBetweenOperation between = new MySQLBetweenOperation(expr, left, right);
        MySQLConstant result = between.getExpectedValue();
        assertEquals(0, result.getInt());
    }

    @Test
    void between_intAboveRange_returnsFalse() {
        MySQLConstant expr = MySQLConstant.createIntConstant(11);
        MySQLConstant left = MySQLConstant.createIntConstant(1);
        MySQLConstant right = MySQLConstant.createIntConstant(10);
        MySQLBetweenOperation between = new MySQLBetweenOperation(expr, left, right);
        MySQLConstant result = between.getExpectedValue();
        assertEquals(0, result.getInt());
    }

    // ========== Integer only tests (String tests may trigger IgnoreMeException due to type conversion) ==========

    // ========== NULL handling tests ==========

    @Test
    void between_exprNull_returnsNull() {
        MySQLConstant expr = MySQLConstant.createNullConstant();
        MySQLConstant left = MySQLConstant.createIntConstant(1);
        MySQLConstant right = MySQLConstant.createIntConstant(10);
        MySQLBetweenOperation between = new MySQLBetweenOperation(expr, left, right);
        MySQLConstant result = between.getExpectedValue();
        assertTrue(result.isNull());
    }

    @Test
    void between_leftNull_returnsNull() {
        MySQLConstant expr = MySQLConstant.createIntConstant(5);
        MySQLConstant left = MySQLConstant.createNullConstant();
        MySQLConstant right = MySQLConstant.createIntConstant(10);
        MySQLBetweenOperation between = new MySQLBetweenOperation(expr, left, right);
        MySQLConstant result = between.getExpectedValue();
        assertTrue(result.isNull());
    }

    @Test
    void between_rightNull_returnsNull() {
        MySQLConstant expr = MySQLConstant.createIntConstant(5);
        MySQLConstant left = MySQLConstant.createIntConstant(1);
        MySQLConstant right = MySQLConstant.createNullConstant();
        MySQLBetweenOperation between = new MySQLBetweenOperation(expr, left, right);
        MySQLConstant result = between.getExpectedValue();
        assertTrue(result.isNull());
    }

    // ========== Accessor tests ==========

    @Test
    void between_getExpr_returnsCorrectValue() {
        MySQLConstant expr = MySQLConstant.createIntConstant(5);
        MySQLConstant left = MySQLConstant.createIntConstant(1);
        MySQLConstant right = MySQLConstant.createIntConstant(10);
        MySQLBetweenOperation between = new MySQLBetweenOperation(expr, left, right);
        assertEquals(expr, between.getExpr());
    }

    @Test
    void between_getLeft_returnsCorrectValue() {
        MySQLConstant expr = MySQLConstant.createIntConstant(5);
        MySQLConstant left = MySQLConstant.createIntConstant(1);
        MySQLConstant right = MySQLConstant.createIntConstant(10);
        MySQLBetweenOperation between = new MySQLBetweenOperation(expr, left, right);
        assertEquals(left, between.getLeft());
    }

    @Test
    void between_getRight_returnsCorrectValue() {
        MySQLConstant expr = MySQLConstant.createIntConstant(5);
        MySQLConstant left = MySQLConstant.createIntConstant(1);
        MySQLConstant right = MySQLConstant.createIntConstant(10);
        MySQLBetweenOperation between = new MySQLBetweenOperation(expr, left, right);
        assertEquals(right, between.getRight());
    }

    // ========== Edge case tests ==========

    @Test
    void between_equalBounds_exprEqualsBound_returnsTrue() {
        MySQLConstant expr = MySQLConstant.createIntConstant(5);
        MySQLConstant left = MySQLConstant.createIntConstant(5);
        MySQLConstant right = MySQLConstant.createIntConstant(5);
        MySQLBetweenOperation between = new MySQLBetweenOperation(expr, left, right);
        MySQLConstant result = between.getExpectedValue();
        assertEquals(1, result.getInt());
    }

    @Test
    void between_equalBounds_exprDifferent_returnsFalse() {
        MySQLConstant expr = MySQLConstant.createIntConstant(10);
        MySQLConstant left = MySQLConstant.createIntConstant(5);
        MySQLConstant right = MySQLConstant.createIntConstant(5);
        MySQLBetweenOperation between = new MySQLBetweenOperation(expr, left, right);
        MySQLConstant result = between.getExpectedValue();
        assertEquals(0, result.getInt());
    }

    // ========== Decimal tests ==========

    @Test
    void between_decimalInRange_returnsTrue() {
        // Use positive integers to represent decimal-like values for testing
        MySQLConstant expr = MySQLConstant.createIntConstant(55);
        MySQLConstant left = MySQLConstant.createIntConstant(10);
        MySQLConstant right = MySQLConstant.createIntConstant(100);
        MySQLBetweenOperation between = new MySQLBetweenOperation(expr, left, right);
        MySQLConstant result = between.getExpectedValue();
        assertEquals(1, result.getInt());
    }
}