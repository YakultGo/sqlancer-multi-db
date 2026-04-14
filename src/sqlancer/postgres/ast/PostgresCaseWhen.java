package sqlancer.postgres.ast;

import sqlancer.postgres.PostgresSchema.PostgresDataType;

/**
 * Simple CASE WHEN expression used by EET transforms.
 */
public final class PostgresCaseWhen implements PostgresExpression {

    private final PostgresExpression whenExpr;
    private final PostgresExpression thenExpr;
    private final PostgresExpression elseExpr;

    public PostgresCaseWhen(PostgresExpression whenExpr, PostgresExpression thenExpr, PostgresExpression elseExpr) {
        if (whenExpr == null) {
            throw new IllegalArgumentException("whenExpr must not be null");
        }
        if (thenExpr == null) {
            throw new IllegalArgumentException("thenExpr must not be null");
        }
        if (elseExpr == null) {
            throw new IllegalArgumentException("elseExpr must not be null");
        }
        this.whenExpr = whenExpr;
        this.thenExpr = thenExpr;
        this.elseExpr = elseExpr;
    }

    public PostgresExpression getWhenExpr() {
        return whenExpr;
    }

    public PostgresExpression getThenExpr() {
        return thenExpr;
    }

    public PostgresExpression getElseExpr() {
        return elseExpr;
    }

    @Override
    public PostgresDataType getExpressionType() {
        PostgresDataType t = thenExpr.getExpressionType();
        if (t != null) {
            return t;
        }
        return elseExpr.getExpressionType();
    }
}

