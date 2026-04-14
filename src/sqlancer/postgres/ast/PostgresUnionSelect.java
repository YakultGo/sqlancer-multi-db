package sqlancer.postgres.ast;

import java.util.List;

import sqlancer.postgres.PostgresSchema.PostgresDataType;

public final class PostgresUnionSelect implements PostgresExpression {

    private final List<PostgresSelect> selects;
    private final boolean unionAll;

    public PostgresUnionSelect(List<PostgresSelect> selects, boolean unionAll) {
        if (selects == null || selects.isEmpty()) {
            throw new IllegalArgumentException("selects must not be null/empty");
        }
        this.selects = selects;
        this.unionAll = unionAll;
    }

    public List<PostgresSelect> getSelects() {
        return selects;
    }

    public boolean isUnionAll() {
        return unionAll;
    }

    @Override
    public PostgresDataType getExpressionType() {
        return null;
    }
}

