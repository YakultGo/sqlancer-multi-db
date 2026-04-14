package sqlancer.postgres.ast;

import sqlancer.postgres.PostgresSchema.PostgresDataType;

public final class PostgresCteTableReference implements PostgresExpression {

    private final String name;

    public PostgresCteTableReference(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be null/blank");
        }
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public PostgresDataType getExpressionType() {
        return null;
    }
}

