package sqlancer.postgres.ast;

import sqlancer.postgres.PostgresSchema.PostgresDataType;

public final class PostgresDerivedTable implements PostgresExpression {

    private final PostgresSelect select;
    private final String alias;

    public PostgresDerivedTable(PostgresSelect select, String alias) {
        if (select == null) {
            throw new IllegalArgumentException("select must not be null");
        }
        if (alias == null || alias.isBlank()) {
            throw new IllegalArgumentException("alias must not be null/blank");
        }
        this.select = select;
        this.alias = alias;
    }

    public PostgresSelect getSelect() {
        return select;
    }

    public String getAlias() {
        return alias;
    }

    @Override
    public PostgresDataType getExpressionType() {
        return null;
    }
}

