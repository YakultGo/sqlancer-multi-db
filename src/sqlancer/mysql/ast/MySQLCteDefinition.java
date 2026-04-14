package sqlancer.mysql.ast;

/**
 * Single CTE: {@code name AS ( subquery )}.
 */
public class MySQLCteDefinition {

    private final String name;
    private final MySQLSelect subquery;

    public MySQLCteDefinition(String name, MySQLSelect subquery) {
        this.name = name;
        this.subquery = subquery;
    }

    public String getName() {
        return name;
    }

    public MySQLSelect getSubquery() {
        return subquery;
    }
}
