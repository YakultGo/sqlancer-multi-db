package sqlancer.mysql.ast;

/**
 * Derived table in FROM: {@code ( subquery ) AS alias}.
 */
public class MySQLDerivedTable implements MySQLExpression {

    private final MySQLSelect subquery;
    private final String alias;

    public MySQLDerivedTable(MySQLSelect subquery, String alias) {
        this.subquery = subquery;
        this.alias = alias;
    }

    public MySQLSelect getSubquery() {
        return subquery;
    }

    public String getAlias() {
        return alias;
    }

    @Override
    public MySQLConstant getExpectedValue() {
        throw new AssertionError("PQS not supported for derived table");
    }
}
