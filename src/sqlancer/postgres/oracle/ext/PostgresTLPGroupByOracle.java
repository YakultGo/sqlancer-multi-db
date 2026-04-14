package sqlancer.postgres.oracle.ext;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import sqlancer.ComparatorHelper;
import sqlancer.Randomly;
import sqlancer.common.oracle.TestOracle;
import sqlancer.postgres.PostgresGlobalState;
import sqlancer.postgres.PostgresSchema.PostgresColumn;
import sqlancer.postgres.PostgresVisitor;
import sqlancer.postgres.ast.PostgresColumnValue;
import sqlancer.postgres.ast.PostgresExpression;
import sqlancer.postgres.oracle.tlp.PostgresTLPBase;

public final class PostgresTLPGroupByOracle extends PostgresTLPBase implements TestOracle<PostgresGlobalState> {

    private String generatedQueryString;

    public PostgresTLPGroupByOracle(PostgresGlobalState state) {
        super(state);
    }

    @Override
    public void check() throws SQLException {
        super.check();
        List<PostgresExpression> fetchColumns = generateNonStarFetchColumns();
        select.setFetchColumns(fetchColumns);
        select.setGroupByExpressions(fetchColumns);
        select.setWhereClause(null);
        select.setOrderByClauses(List.of());
        String originalQueryString = PostgresVisitor.asString(select);
        generatedQueryString = originalQueryString;
        List<String> resultSet = ComparatorHelper.getResultSetFirstColumnAsString(originalQueryString, errors, state);

        select.setWhereClause(predicate);
        String firstQueryString = PostgresVisitor.asString(select);
        select.setWhereClause(negatedPredicate);
        String secondQueryString = PostgresVisitor.asString(select);
        select.setWhereClause(isNullPredicate);
        String thirdQueryString = PostgresVisitor.asString(select);
        List<String> combinedString = new ArrayList<>();
        List<String> secondResultSet = ComparatorHelper.getCombinedResultSetNoDuplicates(firstQueryString,
                secondQueryString, thirdQueryString, combinedString, true, state, errors);
        ComparatorHelper.assumeResultSetsAreEqual(resultSet, secondResultSet, originalQueryString, combinedString,
                state);
    }

    private List<PostgresExpression> generateNonStarFetchColumns() {
        List<PostgresColumn> cols = Randomly.nonEmptySubset(targetTables.getColumns());
        List<PostgresExpression> fetchColumns = new ArrayList<>();
        for (PostgresColumn c : cols) {
            fetchColumns.add(new PostgresColumnValue(c, null));
        }
        return fetchColumns;
    }

    @Override
    public String getLastQueryString() {
        return generatedQueryString;
    }
}

