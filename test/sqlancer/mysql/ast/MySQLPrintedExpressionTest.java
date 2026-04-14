package sqlancer.mysql.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import sqlancer.mysql.MySQLSchema;
import sqlancer.mysql.MySQLVisitor;

class MySQLPrintedExpressionTest {

    @Test
    void toString_wrapsPrintedSql() {
        MySQLSchema.MySQLColumn aCol = new MySQLSchema.MySQLColumn("a", MySQLSchema.MySQLDataType.INT, false, 0);
        MySQLColumnReference ref = new MySQLColumnReference(aCol, MySQLConstant.createNullConstant());
        MySQLPrintedExpression printed = new MySQLPrintedExpression(ref);
        String s = MySQLVisitor.asString(printed);
        assertTrue(s.startsWith("("));
        assertTrue(s.endsWith(")"));
        assertTrue(s.contains("a"));
    }

    @Test
    void getExpectedValue_delegatesToOriginal() {
        MySQLSchema.MySQLColumn aCol = new MySQLSchema.MySQLColumn("a", MySQLSchema.MySQLDataType.INT, false, 0);
        MySQLColumnReference ref = new MySQLColumnReference(aCol, MySQLConstant.createIntConstant(7));
        MySQLPrintedExpression printed = new MySQLPrintedExpression(ref);
        assertEquals(7, printed.getExpectedValue().getInt());
    }
}
