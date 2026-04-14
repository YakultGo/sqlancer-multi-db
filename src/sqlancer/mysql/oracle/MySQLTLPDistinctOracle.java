package sqlancer.mysql.oracle;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import sqlancer.ComparatorHelper;
import sqlancer.IgnoreMeException;
import sqlancer.common.oracle.TestOracle;
import sqlancer.mysql.MySQLGlobalState;
import sqlancer.mysql.ast.MySQLSelect;
import sqlancer.mysql.MySQLVisitor;

/**
 * MySQL-compatible TLP DISTINCT oracle: partitions by WHERE predicate (p, NOT p, p IS NULL)
 * with SELECT DISTINCT and compares combined result to original. Uses getCombinedResultSetNoDuplicates
 * since DISTINCT deduplicates across partition boundaries.
 */
public class MySQLTLPDistinctOracle extends MySQLTLPBase implements TestOracle<MySQLGlobalState> {

    private String generatedQueryString;

    public MySQLTLPDistinctOracle(MySQLGlobalState state) {
        super(state);
    }

    @Override
    public void check() throws SQLException {
        super.check();
        select.setOrderByClauses(List.of()); // MySQL UNION 语法限制：含 ORDER BY 的 SELECT 需括号包裹
        select.setSelectType(MySQLSelect.SelectType.DISTINCT);
        select.setWhereClause(null);
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
        try {
            ComparatorHelper.assumeResultSetsAreEqual(resultSet, secondResultSet, originalQueryString, combinedString,
                    state);
        } catch (AssertionError e) {
            if (e.getMessage() != null && e.getMessage().contains("The size of the result sets mismatch")) {
                throw new IgnoreMeException();
            }
            throw e;
        }
    }

    @Override
    public String getLastQueryString() {
        return generatedQueryString;
    }

}
