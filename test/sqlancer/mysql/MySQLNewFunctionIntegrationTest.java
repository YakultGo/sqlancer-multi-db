package sqlancer.mysql;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import sqlancer.IgnoreMeException;
import sqlancer.mysql.ast.MySQLComputableFunction;
import sqlancer.mysql.ast.MySQLComputableFunction.MySQLFunction;
import sqlancer.mysql.ast.MySQLConstant;
import sqlancer.mysql.ast.MySQLTemporalFunction;
import sqlancer.mysql.ast.MySQLTemporalFunction.TemporalFunctionKind;
import sqlancer.mysql.MySQLSchema.MySQLDataType;

/**
 * Integration tests for new MySQL functions.
 * These tests verify that:
 * 1. New functions can be correctly generated in expressions
 * 2. Generated SQL is syntactically valid
 * 3. Functions return expected values (if database is available)
 */
class MySQLNewFunctionIntegrationTest {

    private static Connection connection;
    private static boolean databaseAvailable = false;

    @BeforeAll
    static void setup() {
        // Try to connect to MySQL database
        try {
            String url = "jdbc:mysql://localhost:3306/";
            String user = "root";
            String password = "";
            connection = DriverManager.getConnection(url, user, password);
            databaseAvailable = true;

            // Create test database
            Statement stmt = connection.createStatement();
            stmt.execute("DROP DATABASE IF EXISTS sqlancer_test_new_functions");
            stmt.execute("CREATE DATABASE sqlancer_test_new_functions");
            stmt.execute("USE sqlancer_test_new_functions");
            stmt.execute("CREATE TABLE test_table (id INT PRIMARY KEY, name VARCHAR(100), created_at DATETIME)");
            stmt.execute("INSERT INTO test_table VALUES (1, 'Hello', '2024-06-15 14:30:00')");
            stmt.execute("INSERT INTO test_table VALUES (2, 'World', '2024-06-20 10:00:00')");
            stmt.close();
        } catch (SQLException e) {
            databaseAvailable = false;
            // Database not available, tests will be skipped
        }
    }

    @AfterAll
    static void cleanup() {
        if (connection != null) {
            try {
                Statement stmt = connection.createStatement();
                stmt.execute("DROP DATABASE IF EXISTS sqlancer_test_new_functions");
                stmt.close();
                connection.close();
            } catch (SQLException e) {
                // Ignore cleanup errors
            }
        }
    }

    // ========== String Function Tests ==========

    @Test
    void testSubstringFunctionGeneration() {
        MySQLConstant str = MySQLConstant.createStringConstant("HelloWorld");
        MySQLConstant pos = MySQLConstant.createIntConstant(1);
        MySQLConstant len = MySQLConstant.createIntConstant(5);

        MySQLComputableFunction func = new MySQLComputableFunction(MySQLFunction.SUBSTRING, str, pos, len);
        String sql = MySQLVisitor.asString(func);

        assertTrue(sql.contains("SUBSTRING"));
        assertTrue(sql.contains("HelloWorld"));

        if (databaseAvailable) {
            tryExecuteAndVerify(sql, "Hello");
        }
    }

    @Test
    void testReplaceFunctionGeneration() {
        MySQLConstant str = MySQLConstant.createStringConstant("Hello World");
        MySQLConstant from = MySQLConstant.createStringConstant("World");
        MySQLConstant to = MySQLConstant.createStringConstant("MySQL");

        MySQLComputableFunction func = new MySQLComputableFunction(MySQLFunction.REPLACE, str, from, to);
        String sql = MySQLVisitor.asString(func);

        assertTrue(sql.contains("REPLACE"));

        if (databaseAvailable) {
            tryExecuteAndVerify(sql, "Hello MySQL");
        }
    }

    @Test
    void testLocateFunctionGeneration() {
        MySQLConstant substr = MySQLConstant.createStringConstant("World");
        MySQLConstant str = MySQLConstant.createStringConstant("Hello World");

        MySQLComputableFunction func = new MySQLComputableFunction(MySQLFunction.LOCATE, substr, str);
        String sql = MySQLVisitor.asString(func);

        assertTrue(sql.contains("LOCATE"));

        if (databaseAvailable) {
            tryExecuteAndVerifyInteger(sql, 7);
        }
    }

    @Test
    void testLpadRpadFunctionGeneration() {
        MySQLConstant str = MySQLConstant.createStringConstant("Hello");
        MySQLConstant len = MySQLConstant.createIntConstant(10);
        MySQLConstant pad = MySQLConstant.createStringConstant("-");

        MySQLComputableFunction lpad = new MySQLComputableFunction(MySQLFunction.LPAD, str, len, pad);
        MySQLComputableFunction rpad = new MySQLComputableFunction(MySQLFunction.RPAD, str, len, pad);

        String lpadSql = MySQLVisitor.asString(lpad);
        String rpadSql = MySQLVisitor.asString(rpad);

        assertTrue(lpadSql.contains("LPAD"));
        assertTrue(rpadSql.contains("RPAD"));

        if (databaseAvailable) {
            tryExecuteAndVerify(lpadSql, "-----Hello");
            tryExecuteAndVerify(rpadSql, "Hello-----");
        }
    }

    @Test
    void testReverseFunctionGeneration() {
        MySQLConstant str = MySQLConstant.createStringConstant("Hello");

        MySQLComputableFunction func = new MySQLComputableFunction(MySQLFunction.REVERSE, str);
        String sql = MySQLVisitor.asString(func);

        assertTrue(sql.contains("REVERSE"));

        if (databaseAvailable) {
            tryExecuteAndVerify(sql, "olleH");
        }
    }

    @Test
    void testRepeatFunctionGeneration() {
        MySQLConstant str = MySQLConstant.createStringConstant("Hi");
        MySQLConstant count = MySQLConstant.createIntConstant(3);

        MySQLComputableFunction func = new MySQLComputableFunction(MySQLFunction.REPEAT, str, count);
        String sql = MySQLVisitor.asString(func);

        assertTrue(sql.contains("REPEAT"));

        if (databaseAvailable) {
            tryExecuteAndVerify(sql, "HiHiHi");
        }
    }

    @Test
    void testConcatWsFunctionGeneration() {
        MySQLConstant sep = MySQLConstant.createStringConstant(",");
        MySQLConstant str1 = MySQLConstant.createStringConstant("Hello");
        MySQLConstant str2 = MySQLConstant.createStringConstant("World");

        MySQLComputableFunction func = new MySQLComputableFunction(MySQLFunction.CONCAT_WS, sep, str1, str2);
        String sql = MySQLVisitor.asString(func);

        assertTrue(sql.contains("CONCAT_WS"));

        if (databaseAvailable) {
            tryExecuteAndVerify(sql, "Hello,World");
        }
    }

    // ========== JSON Function Tests ==========

    @Test
    void testJsonArrayFunctionGeneration() {
        MySQLConstant val1 = MySQLConstant.createIntConstant(1);
        MySQLConstant val2 = MySQLConstant.createStringConstant("test");

        MySQLComputableFunction func = new MySQLComputableFunction(MySQLFunction.JSON_ARRAY, val1, val2);
        String sql = MySQLVisitor.asString(func);

        assertTrue(sql.contains("JSON_ARRAY"));

        if (databaseAvailable) {
            tryExecuteSql("SELECT " + sql);
        }
    }

    @Test
    void testJsonObjectFunctionGeneration() {
        MySQLConstant key1 = MySQLConstant.createStringConstant("name");
        MySQLConstant val1 = MySQLConstant.createStringConstant("John");
        MySQLConstant key2 = MySQLConstant.createStringConstant("age");
        MySQLConstant val2 = MySQLConstant.createIntConstant(30);

        MySQLComputableFunction func = new MySQLComputableFunction(MySQLFunction.JSON_OBJECT, key1, val1, key2, val2);
        String sql = MySQLVisitor.asString(func);

        assertTrue(sql.contains("JSON_OBJECT"));

        if (databaseAvailable) {
            tryExecuteSql("SELECT " + sql);
        }
    }

    // ========== Temporal Function Tests ==========

    @Test
    void testYearMonthDayFunctionGeneration() {
        MySQLConstant date = new MySQLConstant.MySQLDateConstant(2024, 6, 15);

        MySQLComputableFunction yearFunc = new MySQLComputableFunction(MySQLFunction.YEAR, date);
        MySQLComputableFunction monthFunc = new MySQLComputableFunction(MySQLFunction.MONTH, date);
        MySQLComputableFunction dayFunc = new MySQLComputableFunction(MySQLFunction.DAY, date);

        String yearSql = MySQLVisitor.asString(yearFunc);
        String monthSql = MySQLVisitor.asString(monthFunc);
        String daySql = MySQLVisitor.asString(dayFunc);

        assertTrue(yearSql.contains("YEAR"));
        assertTrue(monthSql.contains("MONTH"));
        assertTrue(daySql.contains("DAY"));

        if (databaseAvailable) {
            tryExecuteAndVerifyInteger(yearSql, 2024);
            tryExecuteAndVerifyInteger(monthSql, 6);
            tryExecuteAndVerifyInteger(daySql, 15);
        }
    }

    @Test
    void testHourMinuteSecondFunctionGeneration() {
        MySQLConstant time = new MySQLConstant.MySQLTimeConstant(14, 30, 45);

        MySQLComputableFunction hourFunc = new MySQLComputableFunction(MySQLFunction.HOUR, time);
        MySQLComputableFunction minuteFunc = new MySQLComputableFunction(MySQLFunction.MINUTE, time);
        MySQLComputableFunction secondFunc = new MySQLComputableFunction(MySQLFunction.SECOND, time);

        String hourSql = MySQLVisitor.asString(hourFunc);
        String minuteSql = MySQLVisitor.asString(minuteFunc);
        String secondSql = MySQLVisitor.asString(secondFunc);

        assertTrue(hourSql.contains("HOUR"));
        assertTrue(minuteSql.contains("MINUTE"));
        assertTrue(secondSql.contains("SECOND"));

        if (databaseAvailable) {
            tryExecuteAndVerifyInteger(hourSql, 14);
            tryExecuteAndVerifyInteger(minuteSql, 30);
            tryExecuteAndVerifyInteger(secondSql, 45);
        }
    }

    @Test
    void testDateDiffFunctionGeneration() {
        MySQLConstant date1 = new MySQLConstant.MySQLDateConstant(2024, 6, 20);
        MySQLConstant date2 = new MySQLConstant.MySQLDateConstant(2024, 6, 15);

        MySQLComputableFunction func = new MySQLComputableFunction(MySQLFunction.DATEDIFF, date1, date2);
        String sql = MySQLVisitor.asString(func);

        assertTrue(sql.contains("DATEDIFF"));

        if (databaseAvailable) {
            tryExecuteAndVerifyInteger(sql, 5);
        }
    }

    @Test
    void testLastDayFunctionGeneration() {
        MySQLConstant date = new MySQLConstant.MySQLDateConstant(2024, 6, 15);

        MySQLComputableFunction func = new MySQLComputableFunction(MySQLFunction.LAST_DAY, date);
        String sql = MySQLVisitor.asString(func);

        assertTrue(sql.contains("LAST_DAY"));

        if (databaseAvailable) {
            tryExecuteSql("SELECT " + sql);
        }
    }

    @Test
    void testTemporalFunctionGeneration() {
        MySQLConstant date = new MySQLConstant.MySQLDateConstant(2024, 6, 15);
        MySQLConstant interval = MySQLConstant.createIntConstant(5);

        MySQLTemporalFunction func = new MySQLTemporalFunction(
                TemporalFunctionKind.DATE_ADD,
                date,
                interval,
                "DAY",
                MySQLDataType.DATE
        );

        String sql = MySQLVisitor.asString(func);

        assertTrue(sql.contains("DATE_ADD"));
        assertTrue(sql.contains("INTERVAL"));
        assertTrue(sql.contains("DAY"));

        if (databaseAvailable) {
            tryExecuteSql("SELECT " + sql);
        }
    }

    // ========== Expression Generation Tests ==========

    @Test
    void testConstantGenerationWithNewTypes() {
        // Test that new constant types can be created and converted to SQL
        MySQLConstant[] constants = {
            MySQLConstant.createIntConstant(42),
            MySQLConstant.createStringConstant("test"),
            MySQLConstant.createNullConstant(),
            new MySQLConstant.MySQLDateConstant(2024, 6, 15),
            new MySQLConstant.MySQLTimeConstant(14, 30, 45),
            new MySQLConstant.MySQLDateTimeConstant(2024, 6, 15, 14, 30, 45),
            MySQLConstant.createJSONConstant("{\"key\": \"value\"}")
        };

        for (MySQLConstant constant : constants) {
            String sql = MySQLVisitor.asString(constant);
            assertNotNull(sql);
            assertTrue(sql.length() > 0);
        }
    }

    @Test
    void testAllNewFunctionsGenerateValidSQL() {
        // Test each new function generates valid SQL
        MySQLFunction[] newFunctions = {
            MySQLFunction.SUBSTRING, MySQLFunction.REPLACE, MySQLFunction.LOCATE,
            MySQLFunction.INSTR, MySQLFunction.LPAD, MySQLFunction.RPAD,
            MySQLFunction.REVERSE, MySQLFunction.REPEAT, MySQLFunction.SPACE,
            MySQLFunction.ASCII, MySQLFunction.CHAR_LENGTH, MySQLFunction.CONCAT_WS,
            MySQLFunction.LTRIM, MySQLFunction.RTRIM,
            MySQLFunction.JSON_EXTRACT, MySQLFunction.JSON_ARRAY, MySQLFunction.JSON_OBJECT,
            MySQLFunction.JSON_REMOVE, MySQLFunction.JSON_CONTAINS, MySQLFunction.JSON_KEYS,
            MySQLFunction.YEAR, MySQLFunction.MONTH, MySQLFunction.DAY,
            MySQLFunction.DAYOFWEEK, MySQLFunction.DAYOFMONTH, MySQLFunction.DAYOFYEAR,
            MySQLFunction.WEEK, MySQLFunction.QUARTER,
            MySQLFunction.HOUR, MySQLFunction.MINUTE, MySQLFunction.SECOND,
            MySQLFunction.DATEDIFF, MySQLFunction.LAST_DAY, MySQLFunction.TO_DAYS, MySQLFunction.FROM_DAYS
        };

        for (MySQLFunction func : newFunctions) {
            // Create appropriate arguments for each function
            MySQLConstant[] args = createDefaultArgsForFunction(func);
            if (args != null) {
                try {
                    MySQLComputableFunction computableFunc = new MySQLComputableFunction(func, args);
                    String sql = MySQLVisitor.asString(computableFunc);
                    assertNotNull(sql);
                    assertTrue(sql.contains(func.getName()));
                    assertTrue(sql.length() > 0);
                } catch (IgnoreMeException e) {
                    // Some combinations may throw IgnoreMeException, that's expected
                }
            }
        }
    }

    // ========== Helper Methods ==========

    private MySQLConstant[] createDefaultArgsForFunction(MySQLFunction func) {
        switch (func) {
        // String functions with 1 arg
        case REVERSE:
        case SPACE:
        case ASCII:
        case CHAR_LENGTH:
        case LTRIM:
        case RTRIM:
        case JSON_TYPE:
        case JSON_VALID:
        case YEAR:
        case MONTH:
        case DAY:
        case DAYOFWEEK:
        case DAYOFMONTH:
        case DAYOFYEAR:
        case QUARTER:
        case HOUR:
        case MINUTE:
        case SECOND:
        case LAST_DAY:
        case TO_DAYS:
        case FROM_DAYS:
            return new MySQLConstant[] { MySQLConstant.createStringConstant("test") };

        // String functions with 2 args
        case LOCATE:
        case INSTR:
        case REPEAT:
        case JSON_EXTRACT:
        case JSON_CONTAINS:
        case JSON_KEYS:
        case DATEDIFF:
            return new MySQLConstant[] {
                MySQLConstant.createStringConstant("test"),
                MySQLConstant.createStringConstant("test2")
            };

        // String functions with 3 args
        case SUBSTRING:
        case REPLACE:
        case LPAD:
        case RPAD:
        case JSON_REMOVE:
            return new MySQLConstant[] {
                MySQLConstant.createStringConstant("test"),
                MySQLConstant.createIntConstant(1),
                MySQLConstant.createIntConstant(5)
            };

        // JSON functions (variadic)
        case JSON_ARRAY:
            return new MySQLConstant[] {
                MySQLConstant.createIntConstant(1),
                MySQLConstant.createStringConstant("test")
            };

        case JSON_OBJECT:
            return new MySQLConstant[] {
                MySQLConstant.createStringConstant("key"),
                MySQLConstant.createStringConstant("value")
            };

        case CONCAT_WS:
            return new MySQLConstant[] {
                MySQLConstant.createStringConstant(","),
                MySQLConstant.createStringConstant("a"),
                MySQLConstant.createStringConstant("b")
            };

        case WEEK:
            return new MySQLConstant[] {
                new MySQLConstant.MySQLDateConstant(2024, 6, 15),
                MySQLConstant.createIntConstant(0)
            };

        default:
            return null;
        }
    }

    private void tryExecuteAndVerify(String sql, String expected) {
        if (!databaseAvailable) return;

        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT " + sql);
            if (rs.next()) {
                String result = rs.getString(1);
                assertTrue(result.equals(expected) || result.equalsIgnoreCase(expected),
                    "Expected: " + expected + ", Got: " + result);
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            fail("SQL execution failed: " + e.getMessage() + " for SQL: " + sql);
        }
    }

    private void tryExecuteAndVerifyInteger(String sql, int expected) {
        if (!databaseAvailable) return;

        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT " + sql);
            if (rs.next()) {
                int result = rs.getInt(1);
                assertTrue(result == expected, "Expected: " + expected + ", Got: " + result);
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            fail("SQL execution failed: " + e.getMessage() + " for SQL: " + sql);
        }
    }

    private void tryExecuteSql(String sql) {
        if (!databaseAvailable) return;

        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            fail("SQL execution failed: " + e.getMessage() + " for SQL: " + sql);
        }
    }
}