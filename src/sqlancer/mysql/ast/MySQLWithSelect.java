package sqlancer.mysql.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@code WITH cte1 AS (...), cte2 AS (...) SELECT ...} (MySQL 8.0+ CTE).
 */
public class MySQLWithSelect implements MySQLExpression {

    private final List<MySQLCteDefinition> ctes;
    private final MySQLSelect mainQuery;

    public MySQLWithSelect(List<MySQLCteDefinition> ctes, MySQLSelect mainQuery) {
        if (ctes == null || ctes.isEmpty()) {
            throw new IllegalArgumentException("WITH requires at least one CTE");
        }
        this.ctes = Collections.unmodifiableList(new ArrayList<>(ctes));
        this.mainQuery = mainQuery;
    }

    public List<MySQLCteDefinition> getCtes() {
        return ctes;
    }

    public MySQLSelect getMainQuery() {
        return mainQuery;
    }

    @Override
    public MySQLConstant getExpectedValue() {
        throw new AssertionError("PQS not supported for WITH");
    }
}
