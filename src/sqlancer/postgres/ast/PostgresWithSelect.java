package sqlancer.postgres.ast;

import java.util.List;

import sqlancer.postgres.PostgresSchema.PostgresDataType;

public final class PostgresWithSelect implements PostgresExpression {

    private final List<PostgresCteDefinition> ctes;
    private final PostgresSelect mainSelect;

    public PostgresWithSelect(List<PostgresCteDefinition> ctes, PostgresSelect mainSelect) {
        if (ctes == null || ctes.isEmpty()) {
            throw new IllegalArgumentException("ctes must not be null/empty");
        }
        if (mainSelect == null) {
            throw new IllegalArgumentException("mainSelect must not be null");
        }
        this.ctes = ctes;
        this.mainSelect = mainSelect;
    }

    public List<PostgresCteDefinition> getCtes() {
        return ctes;
    }

    public PostgresSelect getMainSelect() {
        return mainSelect;
    }

    @Override
    public PostgresDataType getExpressionType() {
        return null;
    }
}

