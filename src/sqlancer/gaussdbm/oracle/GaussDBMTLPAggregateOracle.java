package sqlancer.gaussdbm.oracle;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import sqlancer.ComparatorHelper;
import sqlancer.IgnoreMeException;
import sqlancer.Randomly;
import sqlancer.common.oracle.TestOracle;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.common.query.SQLancerResultSet;
import sqlancer.gaussdbm.GaussDBMErrors;
import sqlancer.gaussdbm.GaussDBMGlobalState;
import sqlancer.gaussdbm.GaussDBToStringVisitor;
import sqlancer.gaussdbm.ast.GaussDBAggregate;
import sqlancer.gaussdbm.ast.GaussDBAggregate.GaussDBAggregateFunction;
import sqlancer.gaussdbm.ast.GaussDBExpression;
import sqlancer.gaussdbm.ast.GaussDBJoin;
import sqlancer.gaussdbm.ast.GaussDBSelect;
import sqlancer.gaussdbm.ast.GaussDBUnaryPostfixOperation;
import sqlancer.gaussdbm.ast.GaussDBUnaryPostfixOperation.UnaryPostfixOperator;
import sqlancer.gaussdbm.ast.GaussDBUnaryPrefixOperation;
import sqlancer.gaussdbm.ast.GaussDBUnaryPrefixOperation.UnaryPrefixOperator;

/**
 * TLP aggregate oracle for GaussDB-M (M-Compatibility), aligned with {@code MySQLTLPAggregateOracle}.
 */
public class GaussDBMTLPAggregateOracle extends GaussDBMTLPBase implements TestOracle<GaussDBMGlobalState> {

    private String generatedQueryString;

    public GaussDBMTLPAggregateOracle(GaussDBMGlobalState state) {
        super(state);
        GaussDBMErrors.addExpressionHavingErrors(errors);
    }

    @Override
    public void check() throws SQLException {
        super.check();
        aggregateCheck();
    }

    protected void aggregateCheck() throws SQLException {
        GaussDBAggregateFunction[] allowedFuncs = { GaussDBAggregateFunction.COUNT, GaussDBAggregateFunction.SUM,
                GaussDBAggregateFunction.MIN, GaussDBAggregateFunction.MAX };
        GaussDBAggregateFunction aggregateFunction = Randomly.fromOptions(allowedFuncs);
        List<GaussDBExpression> args = gen.generateExpressions(1);
        GaussDBAggregate aggregate = new GaussDBAggregate(args, aggregateFunction);

        select.setFetchColumns(Arrays.asList(aggregate));
        select.setOrderByClauses(List.of());

        String originalQuery = GaussDBToStringVisitor.asString(select);
        generatedQueryString = originalQuery;
        String firstResult = getAggregateResult(originalQuery);

        GaussDBExpression whereClause = gen.generateExpression();
        GaussDBExpression negatedClause = new GaussDBUnaryPrefixOperation(whereClause, UnaryPrefixOperator.NOT);
        GaussDBExpression isNullClause = new GaussDBUnaryPostfixOperation(whereClause, UnaryPostfixOperator.IS_NULL);

        List<GaussDBExpression> fromList = select.getFromList();
        List<GaussDBJoin> joinList = select.getJoinClauses();

        List<GaussDBExpression> groupByExprs = Randomly.getBooleanWithSmallProbability()
                ? gen.generateExpressions(Randomly.smallNumber() + 1) : null;
        GaussDBSelect leftSelect = getSelect(aggregate, fromList, whereClause, joinList, groupByExprs);
        GaussDBSelect middleSelect = getSelect(aggregate, fromList, negatedClause, joinList, groupByExprs);
        GaussDBSelect rightSelect = getSelect(aggregate, fromList, isNullClause, joinList, groupByExprs);

        String outerAgg = getOuterAggregateFunction(aggregate);
        String metamorphicQuery = "SELECT " + outerAgg + " FROM (";
        metamorphicQuery += GaussDBToStringVisitor.asString(leftSelect) + " UNION ALL "
                + GaussDBToStringVisitor.asString(middleSelect) + " UNION ALL "
                + GaussDBToStringVisitor.asString(rightSelect);
        metamorphicQuery += ") AS t0";

        String secondResult = getAggregateResult(metamorphicQuery);

        String firstQueryStr = String.format("-- %s;\n-- result: %s", originalQuery, firstResult);
        String secondQueryStr = String.format("-- %s;\n-- result: %s", metamorphicQuery, secondResult);
        state.getState().getLocalState().log(String.format("%s\n%s", firstQueryStr, secondQueryStr));

        boolean zeroNullEqual = ("0".equals(firstResult) && secondResult == null)
                || (firstResult == null && "0".equals(secondResult));
        if (zeroNullEqual) {
            return;
        }
        if (isExtremeFloatMismatch(firstResult, secondResult)) {
            throw new IgnoreMeException();
        }
        if (firstResult == null && secondResult != null || firstResult != null && secondResult == null
                || firstResult != null && !firstResult.contentEquals(secondResult)
                        && !ComparatorHelper.isEqualDouble(firstResult, secondResult)) {
            if (secondResult != null && secondResult.contains("Inf")) {
                throw new IgnoreMeException();
            }
            throw new AssertionError(String.format("the results mismatch!\n%s\n%s", firstQueryStr, secondQueryStr));
        }
    }

    private static boolean isExtremeFloatMismatch(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        String sa = a.trim();
        String sb = b.trim();
        boolean aExtreme = sa.contains("E308") || sa.contains("E-308");
        boolean bExtreme = sb.contains("E308") || sb.contains("E-308");
        boolean aZero = "0".equals(sa) || sa.matches("0\\.0*") || sa.matches("-0\\.0*");
        boolean bZero = "0".equals(sb) || sb.matches("0\\.0*") || sb.matches("-0\\.0*");
        if ((aExtreme && bZero) || (bExtreme && aZero)) {
            return true;
        }
        try {
            double da = Double.parseDouble(sa);
            double db = Double.parseDouble(sb);
            double extreme = 1e300;
            double nearZero = 1.0;
            return (Math.abs(da) <= nearZero && Math.abs(db) > extreme)
                    || (Math.abs(db) <= nearZero && Math.abs(da) > extreme);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String getOuterAggregateFunction(GaussDBAggregate aggregate) {
        switch (aggregate.getFunc()) {
        case COUNT:
        case COUNT_DISTINCT:
            return "COALESCE(SUM(ref0), 0)";
        case SUM:
        case SUM_DISTINCT:
        case MIN:
        case MIN_DISTINCT:
        case MAX:
        case MAX_DISTINCT:
            return aggregate.getFunc().getName() + "(ref0)";
        default:
            throw new AssertionError(aggregate.getFunc());
        }
    }

    private String getAggregateResult(String queryString) throws SQLException {
        if (state.getOptions().logEachSelect()) {
            state.getLogger().writeCurrent(queryString);
            try {
                state.getLogger().getCurrentFileWriter().flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        SQLQueryAdapter q = new SQLQueryAdapter(queryString, errors);
        try (SQLancerResultSet result = q.executeAndGet(state)) {
            if (result == null) {
                throw new IgnoreMeException();
            }
            if (!result.next()) {
                return null;
            }
            return result.getString(1);
        } catch (Exception e) {
            if (e instanceof IgnoreMeException) {
                throw (IgnoreMeException) e;
            }
            throw new AssertionError(queryString, e);
        }
    }

    private GaussDBSelect getSelect(GaussDBAggregate aggregate, List<GaussDBExpression> from, GaussDBExpression whereClause,
            List<GaussDBJoin> joinList, List<GaussDBExpression> groupByExprs) {
        GaussDBSelect s = new GaussDBSelect();
        s.setFetchColumns(new ArrayList<>(Arrays.asList(aggregate)));
        s.setFromList(new ArrayList<>(from));
        s.setWhereClause(whereClause);
        s.setJoinList(joinList != null ? joinList.stream().map(j -> (GaussDBExpression) j).collect(Collectors.toList())
                : new ArrayList<>());
        s.setOrderByClauses(List.of());
        if (groupByExprs != null) {
            s.setGroupByExpressions(new ArrayList<>(groupByExprs));
        }
        return s;
    }

    @Override
    public String getLastQueryString() {
        return generatedQueryString;
    }
}
