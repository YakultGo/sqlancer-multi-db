package sqlancer.mysql.ast;

public class MySQLOracleExpressionBag implements MySQLExpression {

    private MySQLExpression innerExpr;

    public MySQLOracleExpressionBag(MySQLExpression expr) {
        this.innerExpr = expr;
    }

    public void updateInnerExpr(MySQLExpression expr) {
        this.innerExpr = expr;
    }

    public MySQLExpression getInnerExpr() {
        return this.innerExpr;
    }

    @Override
    public MySQLConstant getExpectedValue() {
        if (innerExpr != null) {
            return innerExpr.getExpectedValue();
        }
        return null;
    }

}