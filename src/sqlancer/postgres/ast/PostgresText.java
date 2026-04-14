package sqlancer.postgres.ast;

import sqlancer.postgres.PostgresSchema.PostgresDataType;

/**
 * Raw SQL text node used by EET/CODD generators for outer-scope references (e.g. {@code alias.ref0}).
 */
public final class PostgresText implements PostgresExpression {

    private final String text;

    public PostgresText(String text) {
        if (text == null) {
            throw new IllegalArgumentException("text must not be null");
        }
        this.text = text;
    }

    public String getText() {
        return text;
    }

    @Override
    public PostgresDataType getExpressionType() {
        return null;
    }
}

