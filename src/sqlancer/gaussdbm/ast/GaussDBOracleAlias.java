package sqlancer.gaussdbm.ast;

public class GaussDBOracleAlias implements GaussDBExpression {

    private final GaussDBExpression originalExpression;
    private final GaussDBExpression aliasExpression;

    public GaussDBOracleAlias(GaussDBExpression originalExpr, GaussDBExpression aliasExpr) {
        this.originalExpression = originalExpr;
        this.aliasExpression = aliasExpr;
    }

    public GaussDBExpression getOriginalExpression() {
        return originalExpression;
    }

    public GaussDBExpression getAliasExpression() {
        return aliasExpression;
    }

    @Override
    public GaussDBConstant getExpectedValue() {
        if (aliasExpression != null) {
            return aliasExpression.getExpectedValue();
        }
        return originalExpression != null ? originalExpression.getExpectedValue() : null;
    }
}
