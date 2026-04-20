package sqlancer.gaussdbpg.oracle.ext;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import sqlancer.ComparatorHelper;
import sqlancer.IgnoreMeException;
import sqlancer.common.oracle.TestOracle;
import sqlancer.gaussdbpg.GaussDBPGGlobalState;
import sqlancer.gaussdbpg.GaussDBPGToStringVisitor;
import sqlancer.gaussdbpg.ast.GaussDBPGSelect.GaussDBPGSelectType;
import sqlancer.gaussdbpg.oracle.tlp.GaussDBPGTLPBase;

public final class GaussDBPGTLPDistinctOracle extends GaussDBPGTLPBase implements TestOracle<GaussDBPGGlobalState> {

    private String generatedQueryString;

    public GaussDBPGTLPDistinctOracle(GaussDBPGGlobalState state) {
        super(state);
    }

    @Override
    public void check() throws SQLException {
        super.check();
        select.setOrderByClauses(List.of());
        select.setSelectType(GaussDBPGSelectType.DISTINCT);
        select.setWhereClause(null);
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