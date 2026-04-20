package sqlancer.gaussdbpg.oracle.tlp;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import sqlancer.ComparatorHelper;
import sqlancer.Randomly;
import sqlancer.gaussdbpg.GaussDBPGGlobalState;
import sqlancer.gaussdbpg.GaussDBPGSchema.GaussDBPGColumn;
import sqlancer.gaussdbpg.GaussDBPGToStringVisitor;
import sqlancer.gaussdbpg.ast.GaussDBPGColumnReference;
import sqlancer.gaussdbpg.ast.GaussDBPGExpression;

public class GaussDBPGTLPHavingOracle extends GaussDBPGTLPBase {

    public GaussDBPGTLPHavingOracle(GaussDBPGGlobalState state) {
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
        List<GaussDBPGExpression> groupByColumns = new ArrayList<>();
        int numGroupBy = Randomly.smallNumber() + 1;
        List<GaussDBPGColumn> availableColumns = targetTables.getColumns();
        for (int i = 0; i < numGroupBy && i < availableColumns.size(); i++) {
            GaussDBPGColumn c = Randomly.fromList(availableColumns);
            groupByColumns.add(new GaussDBPGColumnReference(c, null));
        }
        select.setGroupByExpressions(groupByColumns);
        select.setHavingClause(null);
        String originalQueryString = GaussDBPGToStringVisitor.asString(select);
        List<String> resultSet = ComparatorHelper.getResultSetFirstColumnAsString(originalQueryString, errors, state);

        boolean orderBy = Randomly.getBoolean();
        if (orderBy) {
            select.setOrderByClauses(gen.generateOrderBys());
        }
        select.setHavingClause(predicate);
        String firstQueryString = GaussDBPGToStringVisitor.asString(select);
        select.setHavingClause(negatedPredicate);
        String secondQueryString = GaussDBPGToStringVisitor.asString(select);
        select.setHavingClause(isNullPredicate);
        String thirdQueryString = GaussDBPGToStringVisitor.asString(select);
        List<String> combinedString = new ArrayList<>();
        List<String> secondResultSet = ComparatorHelper.getCombinedResultSet(firstQueryString, secondQueryString,
                thirdQueryString, combinedString, !orderBy, state, errors);
        ComparatorHelper.assumeResultSetsAreEqual(resultSet, secondResultSet, originalQueryString, combinedString,
                state);
    }

    @Override
    protected GaussDBPGExpression generatePredicate() {
        return gen.generateBooleanExpression();
    }

    @Override
    List<GaussDBPGExpression> generateFetchColumns() {
        // Generate simple column references as fetch columns
        List<GaussDBPGExpression> fetchColumns = new ArrayList<>();
        List<GaussDBPGColumn> targetColumns = Randomly.nonEmptySubset(targetTables.getColumns());
        for (GaussDBPGColumn c : targetColumns) {
            fetchColumns.add(new GaussDBPGColumnReference(c, null));
        }
        return fetchColumns;
    }
}