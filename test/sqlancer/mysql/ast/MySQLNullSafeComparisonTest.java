package sqlancer.mysql.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import sqlancer.mysql.ast.MySQLBinaryComparisonOperation.BinaryComparisonOperator;

/**
 * Tests for MySQL NULL-safe comparison operator (<=>).
 * The <=> operator returns TRUE when both operands are NULL, FALSE when one is NULL.
 */
class MySQLNullSafeComparisonTest {

    // ========== NULL-safe equality tests ==========

    @Test
    void nullSafeEquals_bothNull_returnsTrue() {
        MySQLConstant left = MySQLConstant.createNullConstant();
        MySQLConstant right = MySQLConstant.createNullConstant();
        MySQLConstant result = BinaryComparisonOperator.IS_EQUALS_NULL_SAFE.getExpectedValue(left, right);
        assertTrue(result.getInt() == 1);
    }

    @Test
    void nullSafeEquals_leftNullRightNotNull_returnsFalse() {
        MySQLConstant left = MySQLConstant.createNullConstant();
        MySQLConstant right = MySQLConstant.createIntConstant(5);
        MySQLConstant result = BinaryComparisonOperator.IS_EQUALS_NULL_SAFE.getExpectedValue(left, right);
        assertTrue(result.getInt() == 0);
    }

    @Test
    void nullSafeEquals_leftNotNullRightNull_returnsFalse() {
        MySQLConstant left = MySQLConstant.createIntConstant(5);
        MySQLConstant right = MySQLConstant.createNullConstant();
        MySQLConstant result = BinaryComparisonOperator.IS_EQUALS_NULL_SAFE.getExpectedValue(left, right);
        assertTrue(result.getInt() == 0);
    }

    @Test
    void nullSafeEquals_bothEqualInt_returnsTrue() {
        MySQLConstant left = MySQLConstant.createIntConstant(5);
        MySQLConstant right = MySQLConstant.createIntConstant(5);
        MySQLConstant result = BinaryComparisonOperator.IS_EQUALS_NULL_SAFE.getExpectedValue(left, right);
        assertTrue(result.getInt() == 1);
    }

    @Test
    void nullSafeEquals_bothDifferentInt_returnsFalse() {
        MySQLConstant left = MySQLConstant.createIntConstant(5);
        MySQLConstant right = MySQLConstant.createIntConstant(10);
        MySQLConstant result = BinaryComparisonOperator.IS_EQUALS_NULL_SAFE.getExpectedValue(left, right);
        assertTrue(result.getInt() == 0);
    }

    @Test
    void nullSafeEquals_bothEqualString_returnsTrue() {
        MySQLConstant left = MySQLConstant.createStringConstant("hello");
        MySQLConstant right = MySQLConstant.createStringConstant("hello");
        MySQLConstant result = BinaryComparisonOperator.IS_EQUALS_NULL_SAFE.getExpectedValue(left, right);
        assertTrue(result.getInt() == 1);
    }

    @Test
    void nullSafeEquals_bothDifferentString_returnsFalse() {
        MySQLConstant left = MySQLConstant.createStringConstant("hello");
        MySQLConstant right = MySQLConstant.createStringConstant("world");
        MySQLConstant result = BinaryComparisonOperator.IS_EQUALS_NULL_SAFE.getExpectedValue(left, right);
        assertTrue(result.getInt() == 0);
    }

    @Test
    void nullSafeEquals_leftNullRightString_returnsFalse() {
        MySQLConstant left = MySQLConstant.createNullConstant();
        MySQLConstant right = MySQLConstant.createStringConstant("hello");
        MySQLConstant result = BinaryComparisonOperator.IS_EQUALS_NULL_SAFE.getExpectedValue(left, right);
        assertTrue(result.getInt() == 0);
    }

    @Test
    void nullSafeEquals_textRepresentation_isNullSafeOperator() {
        assertEquals("<=>", BinaryComparisonOperator.IS_EQUALS_NULL_SAFE.getTextRepresentation());
    }

    // ========== Regular equality comparison (for reference) ==========

    @Test
    void regularEquals_bothNull_returnsNull() {
        MySQLConstant left = MySQLConstant.createNullConstant();
        MySQLConstant right = MySQLConstant.createNullConstant();
        MySQLConstant result = BinaryComparisonOperator.EQUALS.getExpectedValue(left, right);
        assertTrue(result.isNull());
    }

    @Test
    void regularEquals_leftNullRightNotNull_returnsNull() {
        MySQLConstant left = MySQLConstant.createNullConstant();
        MySQLConstant right = MySQLConstant.createIntConstant(5);
        MySQLConstant result = BinaryComparisonOperator.EQUALS.getExpectedValue(left, right);
        assertTrue(result.isNull());
    }

    // ========== Using MySQLConstant.isEqualsNullSafe directly ==========

    @Test
    void isEqualsNullSafe_methodNullNull_returnsTrue() {
        MySQLConstant left = MySQLConstant.createNullConstant();
        MySQLConstant right = MySQLConstant.createNullConstant();
        MySQLConstant result = left.isEqualsNullSafe(right);
        assertEquals(1, result.getInt());
    }

    @Test
    void isEqualsNullSafe_methodNullNonNull_returnsFalse() {
        MySQLConstant left = MySQLConstant.createNullConstant();
        MySQLConstant right = MySQLConstant.createIntConstant(42);
        MySQLConstant result = left.isEqualsNullSafe(right);
        assertEquals(0, result.getInt());
    }

    @Test
    void isEqualsNullSafe_methodNonNullNull_returnsFalse() {
        MySQLConstant left = MySQLConstant.createIntConstant(42);
        MySQLConstant right = MySQLConstant.createNullConstant();
        MySQLConstant result = left.isEqualsNullSafe(right);
        assertEquals(0, result.getInt());
    }

    @Test
    void isEqualsNullSafe_methodBothEqual_returnsTrue() {
        MySQLConstant left = MySQLConstant.createIntConstant(42);
        MySQLConstant right = MySQLConstant.createIntConstant(42);
        MySQLConstant result = left.isEqualsNullSafe(right);
        assertEquals(1, result.getInt());
    }

    @Test
    void isEqualsNullSafe_methodBothDifferent_returnsFalse() {
        MySQLConstant left = MySQLConstant.createIntConstant(42);
        MySQLConstant right = MySQLConstant.createIntConstant(100);
        MySQLConstant result = left.isEqualsNullSafe(right);
        assertEquals(0, result.getInt());
    }
}