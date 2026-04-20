package sqlancer.gaussdba.oracle.tlp;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import sqlancer.ComparatorHelper;
import sqlancer.Randomly;
import sqlancer.gaussdba.GaussDBAGlobalState;
import sqlancer.gaussdba.GaussDBASchema.GaussDBAColumn;
import sqlancer.gaussdba.GaussDBAToStringVisitor;
import sqlancer.gaussdba.ast.GaussDBAColumnReference;
import sqlancer.gaussdba.ast.GaussDBAExpression;

public class GaussDBATLPHavingOracle extends GaussDBATLPBase {

    public GaussDBATLPHavingOracle(GaussDBAGlobalState state) {
        super(state);
    }

    @Override
    public void check() throws SQLException {
        super.check();
        havingCheck();
    }

    protected void havingCheck() throws SQLException {
        if (Randomly.getBoolean()) {
            select.setWhereClause(gen.generateBooleanExpression());
        }
        // Generate GROUP BY columns
        List<GaussDBAExpression> groupByColumns = new ArrayList<>();
        int numGroupBy = Randomly.smallNumber() + 1;
        List<GaussDBAColumn> availableColumns = targetTables.getColumns();
        for (int i = 0; i < numGroupBy && i < availableColumns.size(); i++) {
            GaussDBAColumn c = Randomly.fromList(availableColumns);
            groupByColumns.add(new GaussDBAColumnReference(c, null));
        }
        select.setGroupByExpressions(groupByColumns);
        select.setHavingClause(null);
        String originalQueryString = GaussDBAToStringVisitor.asString(select);
        List<String> resultSet = ComparatorHelper.getResultSetFirstColumnAsString(originalQueryString, errors, state);

        boolean orderBy = Randomly.getBoolean();
        if (orderBy) {
            select.setOrderByClauses(gen.generateOrderBys());
        }
        select.setHavingClause(predicate);
        String firstQueryString = GaussDBAToStringVisitor.asString(select);
        select.setHavingClause(negatedPredicate);
        String secondQueryString = GaussDBAToStringVisitor.asString(select);
        select.setHavingClause(isNullPredicate);
        String thirdQueryString = GaussDBAToStringVisitor.asString(select);
        List<String> combinedString = new ArrayList<>();
        List<String> secondResultSet = ComparatorHelper.getCombinedResultSet(firstQueryString, secondQueryString,
                thirdQueryString, combinedString, !orderBy, state, errors);
        ComparatorHelper.assumeResultSetsAreEqual(resultSet, secondResultSet, originalQueryString, combinedString,
                state);
    }

    @Override
    protected GaussDBAExpression generatePredicate() {
        return gen.generateBooleanExpression();
    }

    @Override
    List<GaussDBAExpression> generateFetchColumns() {
        // Generate simple column references as fetch columns
        List<GaussDBAExpression> fetchColumns = new ArrayList<>();
        List<GaussDBAColumn> targetColumns = Randomly.nonEmptySubset(targetTables.getColumns());
        for (GaussDBAColumn c : targetColumns) {
            fetchColumns.add(new GaussDBAColumnReference(c, null));
        }
        return fetchColumns;
    }
}