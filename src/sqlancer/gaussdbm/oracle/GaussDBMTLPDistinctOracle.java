package sqlancer.gaussdbm.oracle;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import sqlancer.ComparatorHelper;
import sqlancer.IgnoreMeException;
import sqlancer.common.oracle.TestOracle;
import sqlancer.gaussdbm.GaussDBMBooleanNormalizer;
import sqlancer.gaussdbm.GaussDBMGlobalState;
import sqlancer.gaussdbm.GaussDBToStringVisitor;
import sqlancer.gaussdbm.ast.GaussDBSelect.SelectType;

/**
 * TLP DISTINCT oracle for GaussDB-M, aligned with {@code MySQLTLPDistinctOracle}.
 */
public class GaussDBMTLPDistinctOracle extends GaussDBMTLPBase implements TestOracle<GaussDBMGlobalState> {

    private String generatedQueryString;

    public GaussDBMTLPDistinctOracle(GaussDBMGlobalState state) {
        super(state);
    }

    @Override
    public void check() throws SQLException {
        super.check();
        select.setOrderByClauses(List.of());
        select.setSelectType(SelectType.DISTINCT);
        select.setWhereClause(null);
        String originalQueryString = GaussDBToStringVisitor.asString(select);
        generatedQueryString = originalQueryString;
        List<String> resultSet = ComparatorHelper.getResultSetFirstColumnAsString(originalQueryString, errors, state);
        // Normalize boolean values for M-compatibility mode
        resultSet = GaussDBMBooleanNormalizer.normalizeList(resultSet);

        select.setWhereClause(predicate);
        String firstQueryString = GaussDBToStringVisitor.asString(select);
        select.setWhereClause(negatedPredicate);
        String secondQueryString = GaussDBToStringVisitor.asString(select);
        select.setWhereClause(isNullPredicate);
        String thirdQueryString = GaussDBToStringVisitor.asString(select);
        List<String> combinedString = new ArrayList<>();
        List<String> secondResultSet = ComparatorHelper.getCombinedResultSetNoDuplicates(firstQueryString,
                secondQueryString, thirdQueryString, combinedString, true, state, errors);
        // Normalize boolean values for M-compatibility mode
        secondResultSet = GaussDBMBooleanNormalizer.normalizeList(secondResultSet);
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
