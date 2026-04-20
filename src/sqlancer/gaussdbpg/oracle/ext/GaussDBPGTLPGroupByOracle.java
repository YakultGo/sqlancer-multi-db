package sqlancer.gaussdbpg.oracle.ext;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import sqlancer.ComparatorHelper;
import sqlancer.Randomly;
import sqlancer.common.oracle.TestOracle;
import sqlancer.gaussdbpg.GaussDBPGGlobalState;
import sqlancer.gaussdbpg.GaussDBPGSchema.GaussDBPGColumn;
import sqlancer.gaussdbpg.GaussDBPGToStringVisitor;
import sqlancer.gaussdbpg.ast.GaussDBPGColumnReference;
import sqlancer.gaussdbpg.ast.GaussDBPGExpression;
import sqlancer.gaussdbpg.oracle.tlp.GaussDBPGTLPBase;

public final class GaussDBPGTLPGroupByOracle extends GaussDBPGTLPBase implements TestOracle<GaussDBPGGlobalState> {

    private String generatedQueryString;

    public GaussDBPGTLPGroupByOracle(GaussDBPGGlobalState state) {
        super(state);
    }

    @Override
    public void check() throws SQLException {
        super.check();
        List<GaussDBPGExpression> fetchColumns = generateNonStarFetchColumns();
        select.setFetchColumns(fetchColumns);
        select.setGroupByExpressions(fetchColumns);
        select.setWhereClause(null);
        select.setOrderByClauses(List.of());
        String originalQueryString = GaussDBPGToStringVisitor.asString(select);
        generatedQueryString = originalQueryString;
        List<String> resultSet = ComparatorHelper.getResultSetFirstColumnAsString(originalQueryString, errors, state);

        select.setWhereClause(predicate);
        String firstQueryString = GaussDBPGToStringVisitor.asString(select);
        select.setWhereClause(negatedPredicate);
        String secondQueryString = GaussDBPGToStringVisitor.asString(select);
        select.setWhereClause(isNullPredicate);
        String thirdQueryString = GaussDBPGToStringVisitor.asString(select);
        List<String> combinedString = new ArrayList<>();
        List<String> secondResultSet = ComparatorHelper.getCombinedResultSetNoDuplicates(firstQueryString,
                secondQueryString, thirdQueryString, combinedString, true, state, errors);
        ComparatorHelper.assumeResultSetsAreEqual(resultSet, secondResultSet, originalQueryString, combinedString,
                state);
    }

    private List<GaussDBPGExpression> generateNonStarFetchColumns() {
        List<GaussDBPGColumn> cols = Randomly.nonEmptySubset(targetTables.getColumns());
        List<GaussDBPGExpression> fetchColumns = new ArrayList<>();
        for (GaussDBPGColumn c : cols) {
            fetchColumns.add(new GaussDBPGColumnReference(c, null));
        }
        return fetchColumns;
    }

    @Override
    public String getLastQueryString() {
        return generatedQueryString;
    }
}