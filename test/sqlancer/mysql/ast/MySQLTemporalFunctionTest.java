package sqlancer.mysql.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import sqlancer.mysql.MySQLVisitor;
import sqlancer.mysql.ast.MySQLTemporalFunction.TemporalFunctionKind;
import sqlancer.mysql.ast.MySQLTemporalUtil.IntervalUnit;
import sqlancer.mysql.MySQLSchema.MySQLDataType;

class MySQLTemporalFunctionTest {

    // ========== MySQLTemporalUtil Tests ==========

    @Test
    void parseDate_validFormat_returnsLocalDate() {
        LocalDate result = MySQLTemporalUtil.parseDate("2024-06-15");
        assertEquals(2024, result.getYear());
        assertEquals(6, result.getMonthValue());
        assertEquals(15, result.getDayOfMonth());
    }

    @Test
    void parseDateTime_validFormat_returnsLocalDateTime() {
        LocalDateTime result = MySQLTemporalUtil.parseDateTime("2024-06-15 14:30:45");
        assertEquals(2024, result.getYear());
        assertEquals(6, result.getMonthValue());
        assertEquals(15, result.getDayOfMonth());
        assertEquals(14, result.getHour());
        assertEquals(30, result.getMinute());
        assertEquals(45, result.getSecond());
    }

    @Test
    void addIntervalToDate_addDays_returnsCorrectDate() {
        LocalDate date = LocalDate.of(2024, 6, 15);
        MySQLTemporalUtil.IntervalValue interval = MySQLTemporalUtil.parseInterval(5, IntervalUnit.DAY);
        LocalDate result = MySQLTemporalUtil.addIntervalToDate(date, interval);
        assertEquals(2024, result.getYear());
        assertEquals(6, result.getMonthValue());
        assertEquals(20, result.getDayOfMonth());
    }

    @Test
    void addIntervalToDate_addMonths_returnsCorrectDate() {
        LocalDate date = LocalDate.of(2024, 6, 15);
        MySQLTemporalUtil.IntervalValue interval = MySQLTemporalUtil.parseInterval(2, IntervalUnit.MONTH);
        LocalDate result = MySQLTemporalUtil.addIntervalToDate(date, interval);
        assertEquals(2024, result.getYear());
        assertEquals(8, result.getMonthValue());
        assertEquals(15, result.getDayOfMonth());
    }

    @Test
    void subtractIntervalFromDate_subtractDays_returnsCorrectDate() {
        LocalDate date = LocalDate.of(2024, 6, 15);
        MySQLTemporalUtil.IntervalValue interval = MySQLTemporalUtil.parseInterval(5, IntervalUnit.DAY);
        LocalDate result = MySQLTemporalUtil.subtractIntervalFromDate(date, interval);
        assertEquals(2024, result.getYear());
        assertEquals(6, result.getMonthValue());
        assertEquals(10, result.getDayOfMonth());
    }

    @Test
    void dateDiff_oneDay_returns1() {
        long result = MySQLTemporalUtil.dateDiff("2024-06-16", "2024-06-15");
        assertEquals(1, result);
    }

    @Test
    void dateDiff_sameDate_returns0() {
        long result = MySQLTemporalUtil.dateDiff("2024-06-15", "2024-06-15");
        assertEquals(0, result);
    }

    @Test
    void extractYear_validDate_returnsYear() {
        int result = MySQLTemporalUtil.extractYear("2024-06-15");
        assertEquals(2024, result);
    }

    @Test
    void extractMonth_validDate_returnsMonth() {
        int result = MySQLTemporalUtil.extractMonth("2024-06-15");
        assertEquals(6, result);
    }

    @Test
    void extractDay_validDate_returnsDay() {
        int result = MySQLTemporalUtil.extractDay("2024-06-15");
        assertEquals(15, result);
    }

    @Test
    void dayOfWeek_sunday_returns1() {
        // 2024-01-07 is a Sunday
        int result = MySQLTemporalUtil.dayOfWeek("2024-01-07");
        assertEquals(1, result);
    }

    @Test
    void dayOfWeek_monday_returns2() {
        // 2024-01-08 is a Monday
        int result = MySQLTemporalUtil.dayOfWeek("2024-01-08");
        assertEquals(2, result);
    }

    @Test
    void dayOfYear_firstDay_returns1() {
        int result = MySQLTemporalUtil.dayOfYear("2024-01-01");
        assertEquals(1, result);
    }

    @Test
    void quarter_january_returns1() {
        int result = MySQLTemporalUtil.extractQuarter("2024-01-15");
        assertEquals(1, result);
    }

    @Test
    void quarter_april_returns2() {
        int result = MySQLTemporalUtil.extractQuarter("2024-04-15");
        assertEquals(2, result);
    }

    @Test
    void extractHour_validTime_returnsHour() {
        int result = MySQLTemporalUtil.extractHour("2024-06-15 14:30:45");
        assertEquals(14, result);
    }

    @Test
    void extractMinute_validTime_returnsMinute() {
        int result = MySQLTemporalUtil.extractMinute("2024-06-15 14:30:45");
        assertEquals(30, result);
    }

    @Test
    void extractSecond_validTime_returnsSecond() {
        int result = MySQLTemporalUtil.extractSecond("2024-06-15 14:30:45");
        assertEquals(45, result);
    }

    @Test
    void lastDayOfMonth_june_returns30() {
        LocalDate result = MySQLTemporalUtil.lastDayOfMonth("2024-06-15");
        assertEquals(30, result.getDayOfMonth());
    }

    @Test
    void lastDayOfMonth_februaryLeapYear_returns29() {
        LocalDate result = MySQLTemporalUtil.lastDayOfMonth("2024-02-15");
        assertEquals(29, result.getDayOfMonth());
    }

    @Test
    void lastDayOfMonth_februaryNonLeapYear_returns28() {
        LocalDate result = MySQLTemporalUtil.lastDayOfMonth("2023-02-15");
        assertEquals(28, result.getDayOfMonth());
    }

    @Test
    void dateFormat_yearSpecifier_returnsYear() {
        String result = MySQLTemporalUtil.dateFormat("2024-06-15", "%Y");
        assertEquals("2024", result);
    }

    @Test
    void dateFormat_monthSpecifier_returnsMonth() {
        String result = MySQLTemporalUtil.dateFormat("2024-06-15", "%m");
        assertEquals("06", result);
    }

    @Test
    void dateFormat_daySpecifier_returnsDay() {
        String result = MySQLTemporalUtil.dateFormat("2024-06-15", "%d");
        assertEquals("15", result);
    }

    @Test
    void dateFormat_combinedSpecifier_returnsCombined() {
        String result = MySQLTemporalUtil.dateFormat("2024-06-15 14:30:45", "%Y-%m-%d %H:%i:%s");
        assertEquals("2024-06-15 14:30:45", result);
    }

    // ========== MySQLTemporalFunction Tests ==========

    @Test
    void dateAdd_addDays_returnsCorrectDate() {
        MySQLConstant date = new MySQLConstant.MySQLDateConstant(2024, 6, 15);
        MySQLConstant interval = MySQLConstant.createIntConstant(5);

        MySQLTemporalFunction func = new MySQLTemporalFunction(
                TemporalFunctionKind.DATE_ADD,
                date,
                interval,
                "DAY",
                MySQLDataType.DATE
        );

        MySQLConstant result = func.getExpectedValue();
        assertEquals("2024-06-20", result.castAsString());
    }

    @Test
    void dateAdd_addMonths_returnsCorrectDate() {
        MySQLConstant date = new MySQLConstant.MySQLDateConstant(2024, 6, 15);
        MySQLConstant interval = MySQLConstant.createIntConstant(2);

        MySQLTemporalFunction func = new MySQLTemporalFunction(
                TemporalFunctionKind.DATE_ADD,
                date,
                interval,
                "MONTH",
                MySQLDataType.DATE
        );

        MySQLConstant result = func.getExpectedValue();
        assertEquals("2024-08-15", result.castAsString());
    }

    @Test
    void dateSub_subtractDays_returnsCorrectDate() {
        MySQLConstant date = new MySQLConstant.MySQLDateConstant(2024, 6, 15);
        MySQLConstant interval = MySQLConstant.createIntConstant(5);

        MySQLTemporalFunction func = new MySQLTemporalFunction(
                TemporalFunctionKind.DATE_SUB,
                date,
                interval,
                "DAY",
                MySQLDataType.DATE
        );

        MySQLConstant result = func.getExpectedValue();
        assertEquals("2024-06-10", result.castAsString());
    }

    @Test
    void dateAdd_withNullDate_returnsNull() {
        MySQLConstant date = MySQLConstant.createNullConstant();
        MySQLConstant interval = MySQLConstant.createIntConstant(5);

        MySQLTemporalFunction func = new MySQLTemporalFunction(
                TemporalFunctionKind.DATE_ADD,
                date,
                interval,
                "DAY",
                MySQLDataType.DATE
        );

        MySQLConstant result = func.getExpectedValue();
        assertTrue(result.isNull());
    }

    @Test
    void dateAdd_withNullInterval_returnsNull() {
        MySQLConstant date = new MySQLConstant.MySQLDateConstant(2024, 6, 15);
        MySQLConstant interval = MySQLConstant.createNullConstant();

        MySQLTemporalFunction func = new MySQLTemporalFunction(
                TemporalFunctionKind.DATE_ADD,
                date,
                interval,
                "DAY",
                MySQLDataType.DATE
        );

        MySQLConstant result = func.getExpectedValue();
        assertTrue(result.isNull());
    }

    // ========== MySQLIntervalConstant Tests ==========

    @Test
    void intervalConstant_textRepresentation_returnsCorrectFormat() {
        MySQLConstant interval = MySQLConstant.createIntervalConstant(5, "DAY");
        assertEquals("INTERVAL 5 DAY", interval.getTextRepresentation());
    }

    @Test
    void intervalConstant_castAsSigned_returnsValue() {
        MySQLConstant interval = MySQLConstant.createIntervalConstant(5, "DAY");
        MySQLConstant result = interval.castAs(MySQLCastOperation.CastType.SIGNED);
        assertEquals(5, result.getInt());
    }

    // ========== MySQLTemporalFunction SQL Generation Tests ==========

    @Test
    void temporalFunction_asString_returnsCorrectSQL() {
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
    }
}