package sqlancer.postgres.ast;

import sqlancer.postgres.PostgresSchema.PostgresDataType;

/**
 * Wrapper node used by EET reducer to preserve original printing while allowing structural replacement.
 */
public final class PostgresPrintedExpression implements PostgresExpression {

    private final PostgresExpression original;

    public PostgresPrintedExpression(PostgresExpression original) {
        if (original == null) {
            throw new IllegalArgumentException("original must not be null");
        }
        this.original = original;
    }

    public PostgresExpression getOriginal() {
        return original;
    }

    @Override
    public PostgresDataType getExpressionType() {
        return original.getExpressionType();
    }

    @Override
    public PostgresConstant getExpectedValue() {
        return original.getExpectedValue();
    }
}

