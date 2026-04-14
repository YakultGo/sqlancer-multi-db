package sqlancer.postgres.ast;

import sqlancer.postgres.PostgresSchema.PostgresDataType;

public final class PostgresOracleExpressionBag implements PostgresExpression {

    private PostgresExpression expr;

    public PostgresOracleExpressionBag(PostgresExpression expr) {
        if (expr == null) {
            throw new IllegalArgumentException("expr must not be null");
        }
        this.expr = expr;
    }

    public PostgresExpression getExpr() {
        return expr;
    }

    public void updateInnerExpr(PostgresExpression newExpr) {
        if (newExpr == null) {
            throw new IllegalArgumentException("newExpr must not be null");
        }
        this.expr = newExpr;
    }

    @Override
    public PostgresDataType getExpressionType() {
        return expr.getExpressionType();
    }
}

