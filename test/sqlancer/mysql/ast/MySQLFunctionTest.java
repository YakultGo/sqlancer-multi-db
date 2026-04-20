package sqlancer.mysql.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import sqlancer.mysql.ast.MySQLComputableFunction.MySQLFunction;

class MySQLFunctionTest {

    // ========== 数学函数测试 ==========

    @Test
    void abs_positiveValue_returnsSame() {
        MySQLConstant arg = MySQLConstant.createIntConstant(5);
        MySQLConstant result = MySQLFunction.ABS.apply(new MySQLConstant[]{arg});
        assertEquals(5, result.getInt());
    }

    @Test
    void abs_negativeValue_returnsPositive() {
        MySQLConstant arg = MySQLConstant.createIntConstant(-5);
        MySQLConstant result = MySQLFunction.ABS.apply(new MySQLConstant[]{arg});
        assertEquals(5, result.getInt());
    }

    @Test
    void abs_null_returnsNull() {
        MySQLConstant arg = MySQLConstant.createNullConstant();
        MySQLConstant result = MySQLFunction.ABS.apply(new MySQLConstant[]{arg});
        assertTrue(result.isNull());
    }

    @Test
    void ceil_intValue_returnsSame() {
        MySQLConstant arg = MySQLConstant.createIntConstant(5);
        MySQLConstant result = MySQLFunction.CEIL.apply(new MySQLConstant[]{arg});
        assertEquals(5, result.getInt());
    }

    @Test
    void floor_intValue_returnsSame() {
        MySQLConstant arg = MySQLConstant.createIntConstant(5);
        MySQLConstant result = MySQLFunction.FLOOR.apply(new MySQLConstant[]{arg});
        assertEquals(5, result.getInt());
    }

    @Test
    void round_intValue_returnsSame() {
        MySQLConstant arg = MySQLConstant.createIntConstant(5);
        MySQLConstant result = MySQLFunction.ROUND.apply(new MySQLConstant[]{arg});
        assertEquals(5, result.getInt());
    }

    @Test
    void mod_normalCase_returnsRemainder() {
        MySQLConstant arg1 = MySQLConstant.createIntConstant(10);
        MySQLConstant arg2 = MySQLConstant.createIntConstant(3);
        MySQLConstant result = MySQLFunction.MOD.apply(new MySQLConstant[]{arg1, arg2});
        assertEquals(1, result.getInt());
    }

    @Test
    void mod_divideByZero_returnsNull() {
        MySQLConstant arg1 = MySQLConstant.createIntConstant(10);
        MySQLConstant arg2 = MySQLConstant.createIntConstant(0);
        MySQLConstant result = MySQLFunction.MOD.apply(new MySQLConstant[]{arg1, arg2});
        assertTrue(result.isNull());
    }

    @Test
    void sign_positive_returns1() {
        MySQLConstant arg = MySQLConstant.createIntConstant(5);
        MySQLConstant result = MySQLFunction.SIGN.apply(new MySQLConstant[]{arg});
        assertEquals(1, result.getInt());
    }

    @Test
    void sign_negative_returnsMinus1() {
        MySQLConstant arg = MySQLConstant.createIntConstant(-5);
        MySQLConstant result = MySQLFunction.SIGN.apply(new MySQLConstant[]{arg});
        assertEquals(-1, result.getInt());
    }

    @Test
    void sign_zero_returns0() {
        MySQLConstant arg = MySQLConstant.createIntConstant(0);
        MySQLConstant result = MySQLFunction.SIGN.apply(new MySQLConstant[]{arg});
        assertEquals(0, result.getInt());
    }

    // ========== 字符串函数测试 ==========

    @Test
    void concat_twoStrings_returnsCombined() {
        MySQLConstant arg1 = MySQLConstant.createStringConstant("Hello");
        MySQLConstant arg2 = MySQLConstant.createStringConstant("World");
        MySQLConstant result = MySQLFunction.CONCAT.apply(new MySQLConstant[]{arg1, arg2});
        assertEquals("HelloWorld", result.getString());
    }

    @Test
    void concat_withNull_returnsNull() {
        MySQLConstant arg1 = MySQLConstant.createStringConstant("Hello");
        MySQLConstant arg2 = MySQLConstant.createNullConstant();
        MySQLConstant result = MySQLFunction.CONCAT.apply(new MySQLConstant[]{arg1, arg2});
        assertTrue(result.isNull());
    }

    @Test
    void length_normalString_returnsLength() {
        MySQLConstant arg = MySQLConstant.createStringConstant("Hello");
        MySQLConstant result = MySQLFunction.LENGTH.apply(new MySQLConstant[]{arg});
        assertEquals(5, result.getInt());
    }

    @Test
    void length_emptyString_returnsZero() {
        MySQLConstant arg = MySQLConstant.createStringConstant("");
        MySQLConstant result = MySQLFunction.LENGTH.apply(new MySQLConstant[]{arg});
        assertEquals(0, result.getInt());
    }

    @Test
    void upper_lowercase_returnsUppercase() {
        MySQLConstant arg = MySQLConstant.createStringConstant("hello");
        MySQLConstant result = MySQLFunction.UPPER.apply(new MySQLConstant[]{arg});
        assertEquals("HELLO", result.getString());
    }

    @Test
    void lower_uppercase_returnsLowercase() {
        MySQLConstant arg = MySQLConstant.createStringConstant("HELLO");
        MySQLConstant result = MySQLFunction.LOWER.apply(new MySQLConstant[]{arg});
        assertEquals("hello", result.getString());
    }

    @Test
    void trim_withSpaces_returnsTrimmed() {
        MySQLConstant arg = MySQLConstant.createStringConstant("  hello  ");
        MySQLConstant result = MySQLFunction.TRIM.apply(new MySQLConstant[]{arg});
        assertEquals("hello", result.getString());
    }

    @Test
    void left_normalCase_returnsSubstring() {
        MySQLConstant strArg = MySQLConstant.createStringConstant("HelloWorld");
        MySQLConstant lenArg = MySQLConstant.createIntConstant(5);
        MySQLConstant result = MySQLFunction.LEFT.apply(new MySQLConstant[]{strArg, lenArg});
        assertEquals("Hello", result.getString());
    }

    @Test
    void right_normalCase_returnsSubstring() {
        MySQLConstant strArg = MySQLConstant.createStringConstant("HelloWorld");
        MySQLConstant lenArg = MySQLConstant.createIntConstant(5);
        MySQLConstant result = MySQLFunction.RIGHT.apply(new MySQLConstant[]{strArg, lenArg});
        assertEquals("World", result.getString());
    }

    // ========== JSON 函数测试 ==========

    @Test
    void jsonType_object_returnsOBJECT() {
        MySQLConstant arg = MySQLConstant.createJSONConstant("{\"a\": 1}");
        MySQLConstant result = MySQLFunction.JSON_TYPE.apply(new MySQLConstant[]{arg});
        assertEquals("OBJECT", result.getString());
    }

    @Test
    void jsonType_array_returnsARRAY() {
        MySQLConstant arg = MySQLConstant.createJSONConstant("[1, 2, 3]");
        MySQLConstant result = MySQLFunction.JSON_TYPE.apply(new MySQLConstant[]{arg});
        assertEquals("ARRAY", result.getString());
    }

    @Test
    void jsonValid_validJson_returns1() {
        MySQLConstant arg = MySQLConstant.createJSONConstant("{\"a\": 1}");
        MySQLConstant result = MySQLFunction.JSON_VALID.apply(new MySQLConstant[]{arg});
        assertEquals(1, result.getInt());
    }

    @Test
    void jsonValid_null_returnsNull() {
        MySQLConstant arg = MySQLConstant.createNullConstant();
        MySQLConstant result = MySQLFunction.JSON_VALID.apply(new MySQLConstant[]{arg});
        assertTrue(result.isNull());
    }

    // ========== 时间函数测试 ==========

    @Test
    void now_returnsNullForExpectedValue() {
        MySQLConstant result = MySQLFunction.NOW.apply(new MySQLConstant[]{});
        assertNull(result);  // 无法计算期望值
    }

    @Test
    void curdate_returnsNullForExpectedValue() {
        MySQLConstant result = MySQLFunction.CURDATE.apply(new MySQLConstant[]{});
        assertNull(result);
    }

    @Test
    void curtime_returnsNullForExpectedValue() {
        MySQLConstant result = MySQLFunction.CURTIME.apply(new MySQLConstant[]{});
        assertNull(result);
    }

    // ========== 已有函数测试 ==========

    @Test
    void bitCount_normalValue_returnsCount() {
        MySQLConstant arg = MySQLConstant.createIntConstant(7);  // 0b111
        MySQLConstant result = MySQLFunction.BIT_COUNT.apply(new MySQLConstant[]{arg});
        assertEquals(3, result.getInt());
    }

    // 注意：COALESCE 和 IF 函数需要 origArgs 参数，这里简化测试
}