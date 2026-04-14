package sqlancer.mysql.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Multiple SELECT branches combined with UNION / UNION ALL (MySQL compound query).
 */
public class MySQLUnionSelect implements MySQLExpression {

    private final List<MySQLSelect> branches;
    private final boolean unionAll;

    public MySQLUnionSelect(List<MySQLSelect> branches, boolean unionAll) {
        if (branches == null || branches.size() < 2) {
            throw new IllegalArgumentException("UNION requires at least two SELECT branches");
        }
        this.branches = Collections.unmodifiableList(new ArrayList<>(branches));
        this.unionAll = unionAll;
    }

    public List<MySQLSelect> getBranches() {
        return branches;
    }

    public boolean isUnionAll() {
        return unionAll;
    }

    @Override
    public MySQLConstant getExpectedValue() {
        throw new AssertionError("PQS not supported for UNION");
    }
}
