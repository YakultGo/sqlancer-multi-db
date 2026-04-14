package sqlancer.gaussdbm.ast;

public class GaussDBOracleExpressionBag implements GaussDBExpression {

    private GaussDBExpression innerExpr;

    public GaussDBOracleExpressionBag(GaussDBExpression expr) {
        this.innerExpr = expr;
    }

    public void updateInnerExpr(GaussDBExpression expr) {
        this.innerExpr = expr;
    }

    public GaussDBExpression getInnerExpr() {
        return innerExpr;
    }

    @Override
    public GaussDBConstant getExpectedValue() {
        if (innerExpr != null) {
            return innerExpr.getExpectedValue();
        }
        return null;
    }
}
