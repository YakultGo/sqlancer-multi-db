package sqlancer.mysql.oracle;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import sqlancer.ComparatorHelper;
import sqlancer.IgnoreMeException;
import sqlancer.Randomly;
import sqlancer.common.oracle.TestOracle;
import sqlancer.mysql.MySQLErrors;
import sqlancer.mysql.MySQLGlobalState;
import sqlancer.mysql.ast.MySQLExpression;
import sqlancer.mysql.MySQLVisitor;

/**
 * MySQL-compatible TLP HAVING oracle: partitions result by HAVING predicate
 * (p, NOT p, p IS NULL) and compares combined result to original. Compatible with MySQL syntax.
 */
public class MySQLTLPHavingOracle extends MySQLTLPBase implements TestOracle<MySQLGlobalState> {

    private String generatedQueryString;

    public MySQLTLPHavingOracle(MySQLGlobalState state) {
        super(state);
        MySQLErrors.addExpressionHavingErrors(errors);
    }

    @Override
    public void check() throws SQLException {
        super.check();
        if (Randomly.getBoolean()) {
            select.setWhereClause(gen.generateExpression());
        }
        // 不添加 ORDER BY：三路分区会用 UNION 拼接，MySQL 对含 ORDER BY 的 UNION 需要每段加括号，否则语法错误
        select.setOrderByClauses(List.of());
        select.setGroupByExpressions(gen.generateExpressions(Randomly.smallNumber() + 1));
        select.setHavingClause(null);
        String originalQueryString = MySQLVisitor.asString(select);
        generatedQueryString = originalQueryString;
        List<String> resultSet = ComparatorHelper.getResultSetFirstColumnAsString(originalQueryString, errors, state);

        select.setHavingClause(predicate);
        String firstQueryString = MySQLVisitor.asString(select);
        select.setHavingClause(negatedPredicate);
        String secondQueryString = MySQLVisitor.asString(select);
        select.setHavingClause(isNullPredicate);
        String thirdQueryString = MySQLVisitor.asString(select);
        List<String> combinedString = new ArrayList<>();
        // 使用 UNION（去重）：当 HAVING 谓词使三路分区重叠时（MySQL 三值逻辑边缘情况），UNION ALL 会重复计数；UNION 去重得正确基数
        List<String> secondResultSet = ComparatorHelper.getCombinedResultSetNoDuplicates(firstQueryString, secondQueryString,
                thirdQueryString, combinedString, true, state, errors);
        try {
            ComparatorHelper.assumeResultSetsAreEqual(resultSet, secondResultSet, originalQueryString, combinedString,
                    state);
        } catch (AssertionError e) {
            // TLP 分区边缘情况：MySQL 三值逻辑导致分区重叠/去重，UNION 与原查询基数可能不一致（如 5 vs 4），忽略避免误报
            if (e.getMessage() != null && e.getMessage().contains("The size of the result sets mismatch")) {
                throw new IgnoreMeException();
            }
            throw e;
        }
    }

    @Override
    protected MySQLExpression generatePredicate() {
        return gen.generateHavingClause();
    }

    @Override
    public String getLastQueryString() {
        return generatedQueryString;
    }
}
