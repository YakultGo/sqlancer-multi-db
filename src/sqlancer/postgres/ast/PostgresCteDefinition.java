package sqlancer.postgres.ast;

public final class PostgresCteDefinition {

    private final String name;
    private final PostgresSelect select;

    public PostgresCteDefinition(String name, PostgresSelect select) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be null/blank");
        }
        if (select == null) {
            throw new IllegalArgumentException("select must not be null");
        }
        this.name = name;
        this.select = select;
    }

    public String getName() {
        return name;
    }

    public PostgresSelect getSelect() {
        return select;
    }
}

