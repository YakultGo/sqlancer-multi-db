package sqlancer.gaussdba.oracle.ext;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import sqlancer.ComparatorHelper;
import sqlancer.Randomly;
import sqlancer.common.oracle.TestOracle;
import sqlancer.gaussdba.GaussDBAGlobalState;
import sqlancer.gaussdba.GaussDBASchema.GaussDBAColumn;
import sqlancer.gaussdba.GaussDBAToStringVisitor;
import sqlancer.gaussdba.ast.GaussDBAColumnReference;
import sqlancer.gaussdba.ast.GaussDBAExpression;
import sqlancer.gaussdba.oracle.tlp.GaussDBATLPBase;

public final class GaussDBATLPGroupByOracle extends GaussDBATLPBase implements TestOracle<GaussDBAGlobalState> {

    private String generatedQueryString;

    public GaussDBATLPGroupByOracle(GaussDBAGlobalState state) {
        super(state);
    }

    @Override
    public void check() throws SQLException {
        super.check();
        List<GaussDBAExpression> fetchColumns = generateNonStarFetchColumns();
        select.setFetchColumns(fetchColumns);
        select.setGroupByExpressions(fetchColumns);
        select.setWhereClause(null);
        select.setOrderByClauses(List.of());
        String originalQueryString = GaussDBAToStringVisitor.asString(select);
        generatedQueryString = originalQueryString;
        List<String> resultSet = ComparatorHelper.getResultSetFirstColumnAsString(originalQueryString, errors, state);

        select.setWhereClause(predicate);
        String firstQueryString = GaussDBAToStringVisitor.asString(select);
        select.setWhereClause(negatedPredicate);
        String secondQueryString = GaussDBAToStringVisitor.asString(select);
        select.setWhereClause(isNullPredicate);
        String thirdQueryString = GaussDBAToStringVisitor.asString(select);
        List<String> combinedString = new ArrayList<>();
        List<String> secondResultSet = ComparatorHelper.getCombinedResultSetNoDuplicates(firstQueryString,
                secondQueryString, thirdQueryString, combinedString, true, state, errors);
        ComparatorHelper.assumeResultSetsAreEqual(resultSet, secondResultSet, originalQueryString, combinedString,
                state);
    }

    private List<GaussDBAExpression> generateNonStarFetchColumns() {
        List<GaussDBAColumn> cols = Randomly.nonEmptySubset(targetTables.getColumns());
        List<GaussDBAExpression> fetchColumns = new ArrayList<>();
        for (GaussDBAColumn c : cols) {
            fetchColumns.add(new GaussDBAColumnReference(c, null));
        }
        return fetchColumns;
    }

    @Override
    public String getLastQueryString() {
        return generatedQueryString;
    }
}