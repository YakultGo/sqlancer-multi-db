package sqlancer.gaussdbpg.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class GaussDBPGConstantTest {

    // Test PG semantics: empty string ≠ NULL
    @Test
    void emptyString_isNotNull() {
        GaussDBPGConstant emptyString = GaussDBPGConstant.createTextConstant("");
        assertFalse(emptyString.isNull());
        assertEquals("''", emptyString.getTextRepresentation());
    }

    @Test
    void nullConstant_isNull() {
        GaussDBPGConstant nullConst = GaussDBPGConstant.createNullConstant();
        assertTrue(nullConst.isNull());
        assertEquals("NULL", nullConst.getTextRepresentation());
    }

    // Test comparison operations - they return GaussDBPGConstant
    @Test
    void intComparison_equals_returnsBooleanConstant() {
        GaussDBPGConstant a = GaussDBPGConstant.createIntConstant(42);
        GaussDBPGConstant b = GaussDBPGConstant.createIntConstant(42);
        GaussDBPGConstant result = a.isEquals(b);
        assertTrue(result.isBoolean());
        assertTrue(result.asBoolean());
    }

    @Test
    void intComparison_notEquals_returnsFalse() {
        GaussDBPGConstant a = GaussDBPGConstant.createIntConstant(42);
        GaussDBPGConstant b = GaussDBPGConstant.createIntConstant(43);
        GaussDBPGConstant result = a.isEquals(b);
        assertTrue(result.isBoolean());
        assertFalse(result.asBoolean());
    }

    @Test
    void intComparison_lessThan_returnsBooleanConstant() {
        GaussDBPGConstant a = GaussDBPGConstant.createIntConstant(10);
        GaussDBPGConstant b = GaussDBPGConstant.createIntConstant(20);
        GaussDBPGConstant result = a.isLessThan(b);
        assertTrue(result.isBoolean());
        assertTrue(result.asBoolean());
        // Reverse comparison
        GaussDBPGConstant reverseResult = b.isLessThan(a);
        assertTrue(reverseResult.isBoolean());
        assertFalse(reverseResult.asBoolean());
    }

    @Test
    void textComparison_equals_returnsBooleanConstant() {
        GaussDBPGConstant a = GaussDBPGConstant.createTextConstant("hello");
        GaussDBPGConstant b = GaussDBPGConstant.createTextConstant("hello");
        GaussDBPGConstant result = a.isEquals(b);
        assertTrue(result.isBoolean());
        assertTrue(result.asBoolean());
    }

    @Test
    void textComparison_lessThan_returnsBooleanConstant() {
        GaussDBPGConstant a = GaussDBPGConstant.createTextConstant("a");
        GaussDBPGConstant b = GaussDBPGConstant.createTextConstant("b");
        GaussDBPGConstant result = a.isLessThan(b);
        assertTrue(result.isBoolean());
        assertTrue(result.asBoolean());
    }

    @Test
    void booleanComparison_trueEqualsTrue() {
        GaussDBPGConstant a = GaussDBPGConstant.createBooleanConstant(true);
        GaussDBPGConstant b = GaussDBPGConstant.createBooleanConstant(true);
        GaussDBPGConstant result = a.isEquals(b);
        assertTrue(result.isBoolean());
        assertTrue(result.asBoolean());
    }

    @Test
    void booleanComparison_trueNotEqualsFalse() {
        GaussDBPGConstant a = GaussDBPGConstant.createBooleanConstant(true);
        GaussDBPGConstant b = GaussDBPGConstant.createBooleanConstant(false);
        GaussDBPGConstant result = a.isEquals(b);
        assertTrue(result.isBoolean());
        assertFalse(result.asBoolean());
    }

    @Test
    void nullComparison_returnsNull() {
        GaussDBPGConstant nullConst = GaussDBPGConstant.createNullConstant();
        GaussDBPGConstant intConst = GaussDBPGConstant.createIntConstant(42);
        // In PG semantics, NULL comparisons should return NULL
        GaussDBPGConstant result1 = nullConst.isEquals(intConst);
        assertTrue(result1.isNull());

        GaussDBPGConstant result2 = intConst.isEquals(nullConst);
        assertTrue(result2.isNull());
    }

    // Test type checking
    @Test
    void intConstant_isInt() {
        GaussDBPGConstant intConst = GaussDBPGConstant.createIntConstant(42);
        assertTrue(intConst.isInt());
        assertFalse(intConst.isBoolean());
        assertFalse(intConst.isString());
    }

    @Test
    void textConstant_isString() {
        GaussDBPGConstant textConst = GaussDBPGConstant.createTextConstant("test");
        assertTrue(textConst.isString());
        assertFalse(textConst.isInt());
    }

    @Test
    void decimalConstant_isDecimal() {
        GaussDBPGConstant decConst = GaussDBPGConstant.createDecimalConstant(new BigDecimal("123.45"));
        assertTrue(decConst.isDecimal());
    }

    @Test
    void floatConstant_isFloat() {
        GaussDBPGConstant floatConst = GaussDBPGConstant.createFloatConstant(3.14);
        assertTrue(floatConst.isFloat());
    }

    // Test text representation
    @Test
    void intConstant_textRepresentation() {
        GaussDBPGConstant intConst = GaussDBPGConstant.createIntConstant(123);
        assertEquals("123", intConst.getTextRepresentation());
    }

    @Test
    void negativeIntConstant_textRepresentation() {
        GaussDBPGConstant intConst = GaussDBPGConstant.createIntConstant(-456);
        assertEquals("-456", intConst.getTextRepresentation());
    }

    @Test
    void textConstant_textRepresentation() {
        GaussDBPGConstant textConst = GaussDBPGConstant.createTextConstant("hello world");
        assertEquals("'hello world'", textConst.getTextRepresentation());
    }

    @Test
    void textConstant_withSingleQuotes_escaped() {
        GaussDBPGConstant textConst = GaussDBPGConstant.createTextConstant("it's");
        assertEquals("'it''s'", textConst.getTextRepresentation());
    }

    @Test
    void booleanConstant_textRepresentation() {
        GaussDBPGConstant trueConst = GaussDBPGConstant.createBooleanConstant(true);
        GaussDBPGConstant falseConst = GaussDBPGConstant.createBooleanConstant(false);
        assertEquals("TRUE", trueConst.getTextRepresentation());
        assertEquals("FALSE", falseConst.getTextRepresentation());
    }

    // Test type casting - using cast() method
    @Test
    void castIntToBoolean() {
        GaussDBPGConstant zero = GaussDBPGConstant.createIntConstant(0);
        GaussDBPGConstant one = GaussDBPGConstant.createIntConstant(1);
        GaussDBPGConstant two = GaussDBPGConstant.createIntConstant(2);

        GaussDBPGConstant zeroCast = zero.cast(GaussDBPGDataType.BOOLEAN);
        GaussDBPGConstant oneCast = one.cast(GaussDBPGDataType.BOOLEAN);
        GaussDBPGConstant twoCast = two.cast(GaussDBPGDataType.BOOLEAN);

        assertTrue(zeroCast.isBoolean());
        assertFalse(zeroCast.asBoolean());
        assertTrue(oneCast.asBoolean());
        assertTrue(twoCast.asBoolean());
    }

    @Test
    void castBooleanToInt() {
        GaussDBPGConstant trueConst = GaussDBPGConstant.createBooleanConstant(true);
        GaussDBPGConstant falseConst = GaussDBPGConstant.createBooleanConstant(false);

        GaussDBPGConstant trueCast = trueConst.cast(GaussDBPGDataType.INT);
        GaussDBPGConstant falseCast = falseConst.cast(GaussDBPGDataType.INT);

        assertTrue(trueCast.isInt());
        assertEquals(1, trueCast.asInt());
        assertEquals(0, falseCast.asInt());
    }

    @Test
    void castIntToText() {
        GaussDBPGConstant intConst = GaussDBPGConstant.createIntConstant(42);
        GaussDBPGConstant textCast = intConst.cast(GaussDBPGDataType.TEXT);
        assertTrue(textCast.isString());
        assertEquals("42", textCast.asString());
    }

    @Test
    void castTextToInt_validNumber() {
        GaussDBPGConstant textConst = GaussDBPGConstant.createTextConstant("123");
        GaussDBPGConstant intCast = textConst.cast(GaussDBPGDataType.INT);
        assertTrue(intCast.isInt());
        assertEquals(123, intCast.asInt());
    }

    @Test
    void castTextToInt_invalidNumber() {
        GaussDBPGConstant textConst = GaussDBPGConstant.createTextConstant("abc");
        GaussDBPGConstant intCast = textConst.cast(GaussDBPGDataType.INT);
        assertTrue(intCast.isInt());
        // Invalid number cast returns -1 based on implementation
        assertEquals(-1, intCast.asInt());
    }

    @Test
    void castTextToBoolean_trueVariants() {
        GaussDBPGConstant textTrue = GaussDBPGConstant.createTextConstant("TRUE");
        GaussDBPGConstant textYes = GaussDBPGConstant.createTextConstant("yes");
        GaussDBPGConstant textOn = GaussDBPGConstant.createTextConstant("on");
        GaussDBPGConstant text1 = GaussDBPGConstant.createTextConstant("1");

        assertTrue(textTrue.cast(GaussDBPGDataType.BOOLEAN).asBoolean());
        assertTrue(textYes.cast(GaussDBPGDataType.BOOLEAN).asBoolean());
        assertTrue(textOn.cast(GaussDBPGDataType.BOOLEAN).asBoolean());
        assertTrue(text1.cast(GaussDBPGDataType.BOOLEAN).asBoolean());
    }

    @Test
    void castTextToBoolean_falseVariants() {
        GaussDBPGConstant textFalse = GaussDBPGConstant.createTextConstant("FALSE");
        GaussDBPGConstant textNo = GaussDBPGConstant.createTextConstant("no");
        GaussDBPGConstant text0 = GaussDBPGConstant.createTextConstant("0");
        GaussDBPGConstant textRandom = GaussDBPGConstant.createTextConstant("xyz");

        assertFalse(textFalse.cast(GaussDBPGDataType.BOOLEAN).asBoolean());
        assertFalse(textNo.cast(GaussDBPGDataType.BOOLEAN).asBoolean());
        assertFalse(text0.cast(GaussDBPGDataType.BOOLEAN).asBoolean());
        assertFalse(textRandom.cast(GaussDBPGDataType.BOOLEAN).asBoolean());
    }
}