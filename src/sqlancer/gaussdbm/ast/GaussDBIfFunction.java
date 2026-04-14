package sqlancer.gaussdbm.ast;

/**
 * MySQL-compatible {@code IF(condition, thenExpr, elseExpr)} for NoREC-style unoptimized queries.
 */
public class GaussDBIfFunction implements GaussDBExpression {

    private final GaussDBExpression condition;
    private final GaussDBExpression thenExpr;
    private final GaussDBExpression elseExpr;

    public GaussDBIfFunction(GaussDBExpression condition, GaussDBExpression thenExpr, GaussDBExpression elseExpr) {
        this.condition = condition;
        this.thenExpr = thenExpr;
        this.elseExpr = elseExpr;
    }

    public GaussDBExpression getCondition() {
        return condition;
    }

    public GaussDBExpression getThenExpr() {
        return thenExpr;
    }

    public GaussDBExpression getElseExpr() {
        return elseExpr;
    }

    @Override
    public GaussDBConstant getExpectedValue() {
        GaussDBConstant c = condition.getExpectedValue();
        if (c.isNull()) {
            return GaussDBConstant.createNullConstant();
        }
        return c.asBooleanNotNull() ? thenExpr.getExpectedValue() : elseExpr.getExpectedValue();
    }
}
