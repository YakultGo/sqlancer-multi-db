package sqlancer.gaussdba.oracle.ext;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import sqlancer.ComparatorHelper;
import sqlancer.IgnoreMeException;
import sqlancer.common.oracle.TestOracle;
import sqlancer.gaussdba.GaussDBAGlobalState;
import sqlancer.gaussdba.GaussDBAToStringVisitor;
import sqlancer.gaussdba.ast.GaussDBASelect.GaussDBASelectType;
import sqlancer.gaussdba.oracle.tlp.GaussDBATLPBase;

public final class GaussDBATLPDistinctOracle extends GaussDBATLPBase implements TestOracle<GaussDBAGlobalState> {

    private String generatedQueryString;

    public GaussDBATLPDistinctOracle(GaussDBAGlobalState state) {
        super(state);
    }

    @Override
    public void check() throws SQLException {
        super.check();
        select.setOrderByClauses(List.of());
        select.setSelectType(GaussDBASelectType.DISTINCT);
        select.setWhereClause(null);
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