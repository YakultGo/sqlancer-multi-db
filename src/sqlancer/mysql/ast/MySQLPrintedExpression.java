package sqlancer.mysql.ast;

import sqlancer.mysql.MySQLVisitor;

/**
 * Embeds a pre-rendered SQL fragment as an expression (EET copy_expr / printed_expr). Semantically equivalent to the
 * original expression for evaluation purposes.
 */
public class MySQLPrintedExpression implements MySQLExpression {

    private final MySQLExpression original;
    private final String printedSql;

    public MySQLPrintedExpression(MySQLExpression original) {
        this.original = original;
        this.printedSql = MySQLVisitor.asString(original);
    }

    public MySQLExpression getOriginal() {
        return original;
    }

    public String getPrintedSql() {
        return printedSql;
    }

    @Override
    public MySQLConstant getExpectedValue() {
        return original.getExpectedValue();
    }
}
