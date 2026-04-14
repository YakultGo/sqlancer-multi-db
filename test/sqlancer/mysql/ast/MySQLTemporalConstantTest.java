package sqlancer.mysql.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import sqlancer.mysql.MySQLSchema.MySQLDataType;

class MySQLTemporalConstantTest {

    @Test
    void date_rendersAsSingleQuotedYYYYMMDD() {
        MySQLConstant c = new MySQLConstant.MySQLDateConstant(2026, 4, 7);
        assertEquals("'2026-04-07'", c.getTextRepresentation());
        assertEquals(MySQLDataType.DATE, c.getType());
        assertEquals("2026-04-07", c.castAsString());
    }

    @Test
    void time_defaultFsp_rendersAsSingleQuotedHHMMSS() {
        MySQLConstant c = new MySQLConstant.MySQLTimeConstant(12, 34, 56);
        assertEquals("'12:34:56'", c.getTextRepresentation());
        assertEquals(MySQLDataType.TIME, c.getType());
        assertEquals("12:34:56", c.castAsString());
    }

    @Test
    void time_fsp6_rendersWithSixFractionalDigits() {
        MySQLConstant c = new MySQLConstant.MySQLTimeConstant(12, 34, 56, 0, 6);
        assertEquals("'12:34:56.000000'", c.getTextRepresentation());
        assertEquals(MySQLDataType.TIME, c.getType());
        assertEquals("12:34:56.000000", c.castAsString());
    }

    @Test
    void datetime_rendersAsSingleQuotedWithOptionalFraction() {
        MySQLConstant c0 = new MySQLConstant.MySQLDateTimeConstant(2026, 4, 7, 12, 34, 56);
        assertEquals("'2026-04-07 12:34:56'", c0.getTextRepresentation());
        assertEquals(MySQLDataType.DATETIME, c0.getType());
        assertEquals("2026-04-07 12:34:56", c0.castAsString());

        MySQLConstant c6 = new MySQLConstant.MySQLDateTimeConstant(2026, 4, 7, 12, 34, 56, 0, 6);
        assertEquals("'2026-04-07 12:34:56.000000'", c6.getTextRepresentation());
        assertEquals(MySQLDataType.DATETIME, c6.getType());
        assertEquals("2026-04-07 12:34:56.000000", c6.castAsString());
    }

    @Test
    void timestamp_rendersAsSingleQuotedWithOptionalFraction() {
        MySQLConstant c0 = new MySQLConstant.MySQLTimestampConstant(2026, 4, 7, 12, 34, 56);
        assertEquals("'2026-04-07 12:34:56'", c0.getTextRepresentation());
        assertEquals(MySQLDataType.TIMESTAMP, c0.getType());
        assertEquals("2026-04-07 12:34:56", c0.castAsString());

        MySQLConstant c6 = new MySQLConstant.MySQLTimestampConstant(2026, 4, 7, 12, 34, 56, 0, 6);
        assertEquals("'2026-04-07 12:34:56.000000'", c6.getTextRepresentation());
        assertEquals(MySQLDataType.TIMESTAMP, c6.getType());
        assertEquals("2026-04-07 12:34:56.000000", c6.castAsString());
    }

    @Test
    void year_rendersAsSingleQuotedFourDigits() {
        MySQLConstant c = new MySQLConstant.MySQLYearConstant(2026);
        assertEquals("'2026'", c.getTextRepresentation());
        assertEquals(MySQLDataType.YEAR, c.getType());
        assertEquals("2026", c.castAsString());
    }
}

