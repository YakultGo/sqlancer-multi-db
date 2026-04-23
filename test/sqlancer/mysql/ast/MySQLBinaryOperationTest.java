package sqlancer.mysql.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import sqlancer.mysql.ast.MySQLBinaryOperation.MySQLBinaryOperator;

/**
 * Tests for MySQL bitwise operations (&, |, ^).
 * Note: These operations are now enabled after MySQLBugs.bug99135 = false.
 */
class MySQLBinaryOperationTest {

    // ========== Bitwise AND tests ==========

    @Test
    void bitwiseAnd_basicCase_returnsCorrectResult() {
        MySQLConstant left = MySQLConstant.createIntConstant(12);  // 0b1100
        MySQLConstant right = MySQLConstant.createIntConstant(10); // 0b1010
        MySQLConstant result = MySQLBinaryOperator.AND.apply(left, right);
        assertEquals(8, result.getInt());  // 0b1000 = 8
    }

    @Test
    void bitwiseAnd_withZero_returnsZero() {
        MySQLConstant left = MySQLConstant.createIntConstant(15);
        MySQLConstant right = MySQLConstant.createIntConstant(0);
        MySQLConstant result = MySQLBinaryOperator.AND.apply(left, right);
        assertEquals(0, result.getInt());
    }

    @Test
    void bitwiseAnd_allBits_returnsSame() {
        MySQLConstant left = MySQLConstant.createIntConstant(-1);  // All bits set
        MySQLConstant right = MySQLConstant.createIntConstant(5);
        MySQLConstant result = MySQLBinaryOperator.AND.apply(left, right);
        assertEquals(5, result.getInt());
    }

    @Test
    void bitwiseAnd_withNull_returnsNull() {
        MySQLConstant left = MySQLConstant.createNullConstant();
        MySQLConstant right = MySQLConstant.createIntConstant(5);
        MySQLConstant result = MySQLBinaryOperator.AND.apply(left, right);
        assertTrue(result.isNull());
    }

    @Test
    void bitwiseAnd_bothNull_returnsNull() {
        MySQLConstant left = MySQLConstant.createNullConstant();
        MySQLConstant right = MySQLConstant.createNullConstant();
        MySQLConstant result = MySQLBinaryOperator.AND.apply(left, right);
        assertTrue(result.isNull());
    }

    // ========== Bitwise OR tests ==========

    @Test
    void bitwiseOr_basicCase_returnsCorrectResult() {
        MySQLConstant left = MySQLConstant.createIntConstant(12);  // 0b1100
        MySQLConstant right = MySQLConstant.createIntConstant(10); // 0b1010
        MySQLConstant result = MySQLBinaryOperator.OR.apply(left, right);
        assertEquals(14, result.getInt());  // 0b1110 = 14
    }

    @Test
    void bitwiseOr_withZero_returnsSame() {
        MySQLConstant left = MySQLConstant.createIntConstant(15);
        MySQLConstant right = MySQLConstant.createIntConstant(0);
        MySQLConstant result = MySQLBinaryOperator.OR.apply(left, right);
        assertEquals(15, result.getInt());
    }

    @Test
    void bitwiseOr_withNull_returnsNull() {
        MySQLConstant left = MySQLConstant.createNullConstant();
        MySQLConstant right = MySQLConstant.createIntConstant(5);
        MySQLConstant result = MySQLBinaryOperator.OR.apply(left, right);
        assertTrue(result.isNull());
    }

    // ========== Bitwise XOR tests ==========

    @Test
    void bitwiseXor_basicCase_returnsCorrectResult() {
        MySQLConstant left = MySQLConstant.createIntConstant(12);  // 0b1100
        MySQLConstant right = MySQLConstant.createIntConstant(10); // 0b1010
        MySQLConstant result = MySQLBinaryOperator.XOR.apply(left, right);
        assertEquals(6, result.getInt());  // 0b0110 = 6
    }

    @Test
    void bitwiseXor_withZero_returnsSame() {
        MySQLConstant left = MySQLConstant.createIntConstant(15);
        MySQLConstant right = MySQLConstant.createIntConstant(0);
        MySQLConstant result = MySQLBinaryOperator.XOR.apply(left, right);
        assertEquals(15, result.getInt());
    }

    @Test
    void bitwiseXor_sameValues_returnsZero() {
        MySQLConstant left = MySQLConstant.createIntConstant(5);
        MySQLConstant right = MySQLConstant.createIntConstant(5);
        MySQLConstant result = MySQLBinaryOperator.XOR.apply(left, right);
        assertEquals(0, result.getInt());
    }

    @Test
    void bitwiseXor_withNull_returnsNull() {
        MySQLConstant left = MySQLConstant.createNullConstant();
        MySQLConstant right = MySQLConstant.createIntConstant(5);
        MySQLConstant result = MySQLBinaryOperator.XOR.apply(left, right);
        assertTrue(result.isNull());
    }

    // ========== Text representation tests ==========

    @Test
    void bitwiseAnd_textRepresentation_isCorrect() {
        assertEquals("&", MySQLBinaryOperator.AND.getTextRepresentation());
    }

    @Test
    void bitwiseOr_textRepresentation_isCorrect() {
        assertEquals("|", MySQLBinaryOperator.OR.getTextRepresentation());
    }

    @Test
    void bitwiseXor_textRepresentation_isCorrect() {
        assertEquals("^", MySQLBinaryOperator.XOR.getTextRepresentation());
    }

    // ========== MySQLBinaryOperation object tests ==========

    @Test
    void binaryOperation_getOp_returnsCorrectOperator() {
        MySQLExpression left = MySQLConstant.createIntConstant(12);
        MySQLExpression right = MySQLConstant.createIntConstant(10);
        MySQLBinaryOperation operation = new MySQLBinaryOperation(left, right, MySQLBinaryOperator.AND);
        assertEquals(MySQLBinaryOperator.AND, operation.getOp());
    }

    @Test
    void binaryOperation_getLeft_returnsCorrectExpression() {
        MySQLExpression left = MySQLConstant.createIntConstant(12);
        MySQLExpression right = MySQLConstant.createIntConstant(10);
        MySQLBinaryOperation operation = new MySQLBinaryOperation(left, right, MySQLBinaryOperator.AND);
        assertEquals(left, operation.getLeft());
    }

    @Test
    void binaryOperation_getRight_returnsCorrectExpression() {
        MySQLExpression left = MySQLConstant.createIntConstant(12);
        MySQLExpression right = MySQLConstant.createIntConstant(10);
        MySQLBinaryOperation operation = new MySQLBinaryOperation(left, right, MySQLBinaryOperator.AND);
        assertEquals(right, operation.getRight());
    }

    @Test
    void binaryOperation_getExpectedValue_returnsCorrectResult() {
        MySQLExpression left = MySQLConstant.createIntConstant(12);
        MySQLExpression right = MySQLConstant.createIntConstant(10);
        MySQLBinaryOperation operation = new MySQLBinaryOperation(left, right, MySQLBinaryOperator.AND);
        MySQLConstant result = operation.getExpectedValue();
        assertEquals(8, result.getInt());
    }

    // ========== Mixed type tests ==========

    @Test
    void bitwiseAnd_unsignedResult_unsignedFlag() {
        MySQLConstant left = MySQLConstant.createUnsignedIntConstant(12);
        MySQLConstant right = MySQLConstant.createUnsignedIntConstant(10);
        MySQLConstant result = MySQLBinaryOperator.AND.apply(left, right);
        // The result should be unsigned
        assertTrue(!result.isSigned());
        assertEquals(8, result.getInt());
    }
}