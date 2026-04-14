package sqlancer.mysql.oracle;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import sqlancer.ComparatorHelper;
import sqlancer.Randomly;
import sqlancer.common.oracle.TestOracle;
import sqlancer.mysql.MySQLGlobalState;
import sqlancer.mysql.ast.MySQLColumnReference;
import sqlancer.mysql.ast.MySQLExpression;
import sqlancer.mysql.MySQLVisitor;

/**
 * MySQL-compatible TLP GROUP BY oracle: partitions by WHERE predicate (p, NOT p, p IS NULL)
 * with GROUP BY and compares combined result to original. Uses getCombinedResultSetNoDuplicates
 * since GROUP BY deduplicates across partition boundaries.
 */
public class MySQLTLPGroupByOracle extends MySQLTLPBase implements TestOracle<MySQLGlobalState> {

    private String generatedQueryString;

    public MySQLTLPGroupByOracle(MySQLGlobalState state) {
        super(state);
    }

    @Override
    public void check() throws SQLException {
        super.check();
        select.setGroupByExpressions(select.getFetchColumns());
        select.setWhereClause(null);
        select.setOrderByClauses(List.of()); // MySQL UNION 语法限制：含 ORDER BY 的 SELECT 需括号包裹
        String originalQueryString = MySQLVisitor.asString(select);
        generatedQueryString = originalQueryString;
        List<String> resultSet = ComparatorHelper.getResultSetFirstColumnAsString(originalQueryString, errors, state);

        select.setWhereClause(predicate);
        String firstQueryString = MySQLVisitor.asString(select);
        select.setWhereClause(negatedPredicate);
        String secondQueryString = MySQLVisitor.asString(select);
        select.setWhereClause(isNullPredicate);
        String thirdQueryString = MySQLVisitor.asString(select);
        List<String> combinedString = new ArrayList<>();
        List<String> secondResultSet = ComparatorHelper.getCombinedResultSetNoDuplicates(firstQueryString,
                secondQueryString, thirdQueryString, combinedString, true, state, errors);
        ComparatorHelper.assumeResultSetsAreEqual(resultSet, secondResultSet, originalQueryString, combinedString,
                state);
    }

    @Override
    List<MySQLExpression> generateFetchColumns() {
        return Randomly.nonEmptySubset(targetTables.getColumns()).stream()
                .map(c -> new MySQLColumnReference(c, null))
                .collect(Collectors.toList());
    }

    @Override
    public String getLastQueryString() {
        return generatedQueryString;
    }

}
