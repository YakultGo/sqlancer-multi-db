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

    // ========== 扩展字符串函数测试 ==========

    @Test
    void substring_normalCase_returnsSubstring() {
        MySQLConstant str = MySQLConstant.createStringConstant("HelloWorld");
        MySQLConstant pos = MySQLConstant.createIntConstant(1);
        MySQLConstant result = MySQLFunction.SUBSTRING.apply(new MySQLConstant[]{str, pos});
        assertEquals("HelloWorld", result.getString());
    }

    @Test
    void substring_withLength_returnsSubstring() {
        MySQLConstant str = MySQLConstant.createStringConstant("HelloWorld");
        MySQLConstant pos = MySQLConstant.createIntConstant(1);
        MySQLConstant len = MySQLConstant.createIntConstant(5);
        MySQLConstant result = MySQLFunction.SUBSTRING.apply(new MySQLConstant[]{str, pos, len});
        assertEquals("Hello", result.getString());
    }

    @Test
    void substring_negativePos_returnsFromEnd() {
        MySQLConstant str = MySQLConstant.createStringConstant("HelloWorld");
        MySQLConstant pos = MySQLConstant.createIntConstant(-5);
        MySQLConstant result = MySQLFunction.SUBSTRING.apply(new MySQLConstant[]{str, pos});
        assertEquals("World", result.getString());
    }

    @Test
    void substring_nullInput_returnsNull() {
        MySQLConstant str = MySQLConstant.createNullConstant();
        MySQLConstant pos = MySQLConstant.createIntConstant(1);
        MySQLConstant result = MySQLFunction.SUBSTRING.apply(new MySQLConstant[]{str, pos});
        assertTrue(result.isNull());
    }

    @Test
    void replace_normalCase_returnsReplaced() {
        MySQLConstant str = MySQLConstant.createStringConstant("HelloWorld");
        MySQLConstant from = MySQLConstant.createStringConstant("World");
        MySQLConstant to = MySQLConstant.createStringConstant("MySQL");
        MySQLConstant result = MySQLFunction.REPLACE.apply(new MySQLConstant[]{str, from, to});
        assertEquals("HelloMySQL", result.getString());
    }

    @Test
    void replace_notFound_returnsOriginal() {
        MySQLConstant str = MySQLConstant.createStringConstant("Hello");
        MySQLConstant from = MySQLConstant.createStringConstant("World");
        MySQLConstant to = MySQLConstant.createStringConstant("MySQL");
        MySQLConstant result = MySQLFunction.REPLACE.apply(new MySQLConstant[]{str, from, to});
        assertEquals("Hello", result.getString());
    }

    @Test
    void locate_found_returnsPosition() {
        MySQLConstant substr = MySQLConstant.createStringConstant("World");
        MySQLConstant str = MySQLConstant.createStringConstant("HelloWorld");
        MySQLConstant result = MySQLFunction.LOCATE.apply(new MySQLConstant[]{substr, str});
        assertEquals(6, result.getInt());
    }

    @Test
    void locate_notFound_returnsZero() {
        MySQLConstant substr = MySQLConstant.createStringConstant("MySQL");
        MySQLConstant str = MySQLConstant.createStringConstant("HelloWorld");
        MySQLConstant result = MySQLFunction.LOCATE.apply(new MySQLConstant[]{substr, str});
        assertEquals(0, result.getInt());
    }

    @Test
    void instr_normalCase_returnsPosition() {
        MySQLConstant str = MySQLConstant.createStringConstant("HelloWorld");
        MySQLConstant substr = MySQLConstant.createStringConstant("World");
        MySQLConstant result = MySQLFunction.INSTR.apply(new MySQLConstant[]{str, substr});
        assertEquals(6, result.getInt());
    }

    @Test
    void lpad_normalCase_returnsPadded() {
        MySQLConstant str = MySQLConstant.createStringConstant("Hello");
        MySQLConstant len = MySQLConstant.createIntConstant(10);
        MySQLConstant pad = MySQLConstant.createStringConstant("-");
        MySQLConstant result = MySQLFunction.LPAD.apply(new MySQLConstant[]{str, len, pad});
        assertEquals("-----Hello", result.getString());
    }

    @Test
    void rpad_normalCase_returnsPadded() {
        MySQLConstant str = MySQLConstant.createStringConstant("Hello");
        MySQLConstant len = MySQLConstant.createIntConstant(10);
        MySQLConstant pad = MySQLConstant.createStringConstant("-");
        MySQLConstant result = MySQLFunction.RPAD.apply(new MySQLConstant[]{str, len, pad});
        assertEquals("Hello-----", result.getString());
    }

    @Test
    void reverse_normalCase_returnsReversed() {
        MySQLConstant str = MySQLConstant.createStringConstant("Hello");
        MySQLConstant result = MySQLFunction.REVERSE.apply(new MySQLConstant[]{str});
        assertEquals("olleH", result.getString());
    }

    @Test
    void repeat_normalCase_returnsRepeated() {
        MySQLConstant str = MySQLConstant.createStringConstant("Hi");
        MySQLConstant count = MySQLConstant.createIntConstant(3);
        MySQLConstant result = MySQLFunction.REPEAT.apply(new MySQLConstant[]{str, count});
        assertEquals("HiHiHi", result.getString());
    }

    @Test
    void space_normalCase_returnsSpaces() {
        MySQLConstant count = MySQLConstant.createIntConstant(5);
        MySQLConstant result = MySQLFunction.SPACE.apply(new MySQLConstant[]{count});
        assertEquals("     ", result.getString());
    }

    @Test
    void ascii_normalCase_returnsAsciiValue() {
        MySQLConstant str = MySQLConstant.createStringConstant("A");
        MySQLConstant result = MySQLFunction.ASCII.apply(new MySQLConstant[]{str});
        assertEquals(65, result.getInt());
    }

    @Test
    void charLength_normalCase_returnsLength() {
        MySQLConstant str = MySQLConstant.createStringConstant("Hello");
        MySQLConstant result = MySQLFunction.CHAR_LENGTH.apply(new MySQLConstant[]{str});
        assertEquals(5, result.getInt());
    }

    @Test
    void concatWs_normalCase_returnsConcatenated() {
        MySQLConstant sep = MySQLConstant.createStringConstant(",");
        MySQLConstant str1 = MySQLConstant.createStringConstant("Hello");
        MySQLConstant str2 = MySQLConstant.createStringConstant("World");
        MySQLConstant result = MySQLFunction.CONCAT_WS.apply(new MySQLConstant[]{sep, str1, str2});
        assertEquals("Hello,World", result.getString());
    }

    @Test
    void concatWs_withNull_skipsNull() {
        MySQLConstant sep = MySQLConstant.createStringConstant(",");
        MySQLConstant str1 = MySQLConstant.createStringConstant("Hello");
        MySQLConstant str2 = MySQLConstant.createNullConstant();
        MySQLConstant str3 = MySQLConstant.createStringConstant("World");
        MySQLConstant result = MySQLFunction.CONCAT_WS.apply(new MySQLConstant[]{sep, str1, str2, str3});
        assertEquals("Hello,World", result.getString());
    }

    @Test
    void ltrim_withLeadingSpaces_returnsTrimmed() {
        MySQLConstant str = MySQLConstant.createStringConstant("   Hello");
        MySQLConstant result = MySQLFunction.LTRIM.apply(new MySQLConstant[]{str});
        assertEquals("Hello", result.getString());
    }

    @Test
    void rtrim_withTrailingSpaces_returnsTrimmed() {
        MySQLConstant str = MySQLConstant.createStringConstant("Hello   ");
        MySQLConstant result = MySQLFunction.RTRIM.apply(new MySQLConstant[]{str});
        assertEquals("Hello", result.getString());
    }

    // ========== 扩展 JSON 函数测试 ==========

    @Test
    void jsonArray_normalCase_returnsArray() {
        MySQLConstant val1 = MySQLConstant.createStringConstant("Hello");
        MySQLConstant val2 = MySQLConstant.createIntConstant(42);
        MySQLConstant result = MySQLFunction.JSON_ARRAY.apply(new MySQLConstant[]{val1, val2});
        assertTrue(result.getString().startsWith("["));
        assertTrue(result.getString().contains("Hello"));
        assertTrue(result.getString().contains("42"));
    }

    @Test
    void jsonObject_normalCase_returnsObject() {
        MySQLConstant key1 = MySQLConstant.createStringConstant("name");
        MySQLConstant val1 = MySQLConstant.createStringConstant("John");
        MySQLConstant result = MySQLFunction.JSON_OBJECT.apply(new MySQLConstant[]{key1, val1});
        assertTrue(result.getString().startsWith("{"));
        assertTrue(result.getString().contains("name"));
        assertTrue(result.getString().contains("John"));
    }

    // ========== 扩展时间日期函数测试 ==========

    @Test
    void year_normalDate_returnsYear() {
        MySQLConstant date = new MySQLConstant.MySQLDateConstant(2024, 6, 15);
        MySQLConstant result = MySQLFunction.YEAR.apply(new MySQLConstant[]{date});
        assertEquals(2024, result.getInt());
    }

    @Test
    void month_normalDate_returnsMonth() {
        MySQLConstant date = new MySQLConstant.MySQLDateConstant(2024, 6, 15);
        MySQLConstant result = MySQLFunction.MONTH.apply(new MySQLConstant[]{date});
        assertEquals(6, result.getInt());
    }

    @Test
    void day_normalDate_returnsDay() {
        MySQLConstant date = new MySQLConstant.MySQLDateConstant(2024, 6, 15);
        MySQLConstant result = MySQLFunction.DAY.apply(new MySQLConstant[]{date});
        assertEquals(15, result.getInt());
    }

    @Test
    void dayOfWeek_sunday_returns1() {
        MySQLConstant date = new MySQLConstant.MySQLDateConstant(2024, 1, 7); // Sunday
        MySQLConstant result = MySQLFunction.DAYOFWEEK.apply(new MySQLConstant[]{date});
        assertEquals(1, result.getInt());
    }

    @Test
    void dayOfYear_firstDay_returns1() {
        MySQLConstant date = new MySQLConstant.MySQLDateConstant(2024, 1, 1);
        MySQLConstant result = MySQLFunction.DAYOFYEAR.apply(new MySQLConstant[]{date});
        assertEquals(1, result.getInt());
    }

    @Test
    void quarter_firstQuarter_returns1() {
        MySQLConstant date = new MySQLConstant.MySQLDateConstant(2024, 2, 15);
        MySQLConstant result = MySQLFunction.QUARTER.apply(new MySQLConstant[]{date});
        assertEquals(1, result.getInt());
    }

    @Test
    void quarter_secondQuarter_returns2() {
        MySQLConstant date = new MySQLConstant.MySQLDateConstant(2024, 5, 15);
        MySQLConstant result = MySQLFunction.QUARTER.apply(new MySQLConstant[]{date});
        assertEquals(2, result.getInt());
    }

    @Test
    void hour_normalTime_returnsHour() {
        MySQLConstant time = new MySQLConstant.MySQLTimeConstant(14, 30, 45);
        MySQLConstant result = MySQLFunction.HOUR.apply(new MySQLConstant[]{time});
        assertEquals(14, result.getInt());
    }

    @Test
    void minute_normalTime_returnsMinute() {
        MySQLConstant time = new MySQLConstant.MySQLTimeConstant(14, 30, 45);
        MySQLConstant result = MySQLFunction.MINUTE.apply(new MySQLConstant[]{time});
        assertEquals(30, result.getInt());
    }

    @Test
    void second_normalTime_returnsSecond() {
        MySQLConstant time = new MySQLConstant.MySQLTimeConstant(14, 30, 45);
        MySQLConstant result = MySQLFunction.SECOND.apply(new MySQLConstant[]{time});
        assertEquals(45, result.getInt());
    }

    @Test
    void dateDiff_sameDates_returnsZero() {
        MySQLConstant date1 = new MySQLConstant.MySQLDateConstant(2024, 6, 15);
        MySQLConstant date2 = new MySQLConstant.MySQLDateConstant(2024, 6, 15);
        MySQLConstant result = MySQLFunction.DATEDIFF.apply(new MySQLConstant[]{date1, date2});
        assertEquals(0, result.getInt());
    }

    @Test
    void dateDiff_oneDayDifference_returns1() {
        MySQLConstant date1 = new MySQLConstant.MySQLDateConstant(2024, 6, 16);
        MySQLConstant date2 = new MySQLConstant.MySQLDateConstant(2024, 6, 15);
        MySQLConstant result = MySQLFunction.DATEDIFF.apply(new MySQLConstant[]{date1, date2});
        assertEquals(1, result.getInt());
    }

    @Test
    void dateDiff_reverseOrder_returnsNegative() {
        MySQLConstant date1 = new MySQLConstant.MySQLDateConstant(2024, 6, 15);
        MySQLConstant date2 = new MySQLConstant.MySQLDateConstant(2024, 6, 16);
        MySQLConstant result = MySQLFunction.DATEDIFF.apply(new MySQLConstant[]{date1, date2});
        assertEquals(-1, result.getInt());
    }

    @Test
    void lastDay_normalDate_returnsLastDay() {
        MySQLConstant date = new MySQLConstant.MySQLDateConstant(2024, 6, 15);
        MySQLConstant result = MySQLFunction.LAST_DAY.apply(new MySQLConstant[]{date});
        // June has 30 days
        assertEquals("2024-06-30", result.castAsString());
    }

    @Test
    void lastDay_february_returnsLastDay() {
        MySQLConstant date = new MySQLConstant.MySQLDateConstant(2024, 2, 15);
        MySQLConstant result = MySQLFunction.LAST_DAY.apply(new MySQLConstant[]{date});
        // 2024 is a leap year, February has 29 days
        assertEquals("2024-02-29", result.castAsString());
    }
}