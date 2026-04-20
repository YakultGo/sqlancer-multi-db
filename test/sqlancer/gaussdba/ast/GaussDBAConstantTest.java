package sqlancer.gaussdba.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for GaussDBAConstant to verify Oracle semantics:
 * - Empty string = NULL
 * - No BOOLEAN type, using NUMBER(1)
 * - NULL comparisons return NULL
 */
public class GaussDBAConstantTest {

    @Test
    public void testEmptyStringIsNull() {
        // Oracle语义：空字符串被视为NULL
        GaussDBAConstant emptyString = GaussDBAConstant.createVarchar2Constant("");
        assertTrue(emptyString.isNull(), "Empty string should be NULL in Oracle semantics");
    }

    @Test
    public void testNonEmptyStringIsNotNull() {
        GaussDBAConstant nonEmptyString = GaussDBAConstant.createVarchar2Constant("test");
        assertFalse(nonEmptyString.isNull(), "Non-empty string should NOT be NULL");
    }

    @Test
    public void testNullConstantIsNull() {
        GaussDBAConstant nullConst = GaussDBAConstant.createNullConstant();
        assertTrue(nullConst.isNull(), "NULL constant should be NULL");
    }

    @Test
    public void testNumberConstantIsNotNull() {
        GaussDBAConstant number = GaussDBAConstant.createNumberConstant(1);
        assertFalse(number.isNull(), "Number constant should NOT be NULL");
    }

    @Test
    public void testBooleanRepresentation() {
        // A模式无BOOLEAN类型，用NUMBER(1)表示
        GaussDBAConstant trueVal = GaussDBAConstant.createNumberConstant(1);
        GaussDBAConstant falseVal = GaussDBAConstant.createNumberConstant(0);

        assertTrue(trueVal.isNumber(), "TRUE should be represented as NUMBER");
        assertTrue(falseVal.isNumber(), "FALSE should be represented as NUMBER");
        assertEquals(1, trueVal.asNumber(), "TRUE value should be 1");
        assertEquals(0, falseVal.asNumber(), "FALSE value should be 0");
    }

    @Test
    public void testNullEqualsNullReturnsNull() {
        // Oracle语义：NULL = NULL 返回 NULL (不是TRUE)
        GaussDBAConstant null1 = GaussDBAConstant.createNullConstant();
        GaussDBAConstant null2 = GaussDBAConstant.createNullConstant();

        GaussDBAConstant result = null1.isEquals(null2);
        assertTrue(result.isNull(), "NULL = NULL should return NULL in Oracle semantics");
    }

    @Test
    public void testEmptyStringEqualsEmptyStringReturnsNull() {
        // Oracle语义：空串被视为NULL，所以 '' = '' 返回 NULL
        GaussDBAConstant empty1 = GaussDBAConstant.createVarchar2Constant("");
        GaussDBAConstant empty2 = GaussDBAConstant.createVarchar2Constant("");

        GaussDBAConstant result = empty1.isEquals(empty2);
        assertTrue(result.isNull(), "Empty string = Empty string should return NULL");
    }

    @Test
    public void testNullEqualsValueReturnsNull() {
        // Oracle语义：NULL与任何值比较都返回NULL
        GaussDBAConstant nullConst = GaussDBAConstant.createNullConstant();
        GaussDBAConstant value = GaussDBAConstant.createNumberConstant(1);

        GaussDBAConstant result = nullConst.isEquals(value);
        assertTrue(result.isNull(), "NULL = value should return NULL");

        result = value.isEquals(nullConst);
        assertTrue(result.isNull(), "value = NULL should return NULL");
    }

    @Test
    public void testNumberEquals() {
        GaussDBAConstant num1 = GaussDBAConstant.createNumberConstant(1);
        GaussDBAConstant num2 = GaussDBAConstant.createNumberConstant(1);
        GaussDBAConstant num3 = GaussDBAConstant.createNumberConstant(2);

        GaussDBAConstant result = num1.isEquals(num2);
        assertTrue(result.isNumber(), "1 = 1 should return a number");
        assertEquals(1, result.asNumber(), "1 = 1 should return TRUE (1)");

        result = num1.isEquals(num3);
        assertTrue(result.isNumber(), "1 = 2 should return a number");
        assertEquals(0, result.asNumber(), "1 = 2 should return FALSE (0)");
    }

    @Test
    public void testStringEquals() {
        GaussDBAConstant str1 = GaussDBAConstant.createVarchar2Constant("hello");
        GaussDBAConstant str2 = GaussDBAConstant.createVarchar2Constant("hello");
        GaussDBAConstant str3 = GaussDBAConstant.createVarchar2Constant("world");

        GaussDBAConstant result = str1.isEquals(str2);
        assertTrue(result.isNumber(), "hello = hello should return a number");
        assertEquals(1, result.asNumber(), "hello = hello should return TRUE (1)");

        result = str1.isEquals(str3);
        assertTrue(result.isNumber(), "hello = world should return a number");
        assertEquals(0, result.asNumber(), "hello = world should return FALSE (0)");
    }

    @Test
    public void testNullLessThanReturnsNull() {
        // Oracle语义：NULL与任何值比较都返回NULL
        GaussDBAConstant nullConst = GaussDBAConstant.createNullConstant();
        GaussDBAConstant value = GaussDBAConstant.createNumberConstant(1);

        GaussDBAConstant result = nullConst.isLessThan(value);
        assertTrue(result.isNull(), "NULL < value should return NULL");

        result = value.isLessThan(nullConst);
        assertTrue(result.isNull(), "value < NULL should return NULL");
    }

    @Test
    public void testEmptyStringLessThanReturnsNull() {
        // Oracle语义：空串被视为NULL
        GaussDBAConstant empty = GaussDBAConstant.createVarchar2Constant("");
        GaussDBAConstant value = GaussDBAConstant.createNumberConstant(1);

        GaussDBAConstant result = empty.isLessThan(value);
        assertTrue(result.isNull(), "Empty string < value should return NULL");
    }
}