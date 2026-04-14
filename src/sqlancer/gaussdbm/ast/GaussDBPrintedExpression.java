package sqlancer.gaussdbm.ast;

import sqlancer.gaussdbm.GaussDBToStringVisitor;

/**
 * Embeds a pre-rendered SQL fragment (EET copy_expr); semantically equivalent to the original for evaluation.
 */
public class GaussDBPrintedExpression implements GaussDBExpression {

    private final GaussDBExpression original;
    private final String printedSql;

    public GaussDBPrintedExpression(GaussDBExpression original) {
        this.original = original;
        this.printedSql = GaussDBToStringVisitor.asString(original);
    }

    public GaussDBExpression getOriginal() {
        return original;
    }

    public String getPrintedSql() {
        return printedSql;
    }

    @Override
    public GaussDBConstant getExpectedValue() {
        return original.getExpectedValue();
    }
}
