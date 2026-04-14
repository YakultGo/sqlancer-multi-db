package sqlancer.mysql.ast;

/**
 * Reference to a CTE name in FROM clause (e.g. {@code FROM my_cte}).
 */
public class MySQLCteTableReference implements MySQLExpression {

    private final String cteName;

    public MySQLCteTableReference(String cteName) {
        this.cteName = cteName;
    }

    public String getCteName() {
        return cteName;
    }

    @Override
    public MySQLConstant getExpectedValue() {
        throw new AssertionError("PQS not supported for CTE reference");
    }
}
