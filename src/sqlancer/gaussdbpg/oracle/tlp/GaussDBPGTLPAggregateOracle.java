package sqlancer.gaussdbpg.oracle.tlp;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import sqlancer.ComparatorHelper;
import sqlancer.IgnoreMeException;
import sqlancer.Randomly;
import sqlancer.common.oracle.TestOracle;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.common.query.SQLancerResultSet;
import sqlancer.gaussdbpg.GaussDBPGGlobalState;
import sqlancer.gaussdbpg.GaussDBPGToStringVisitor;
import sqlancer.gaussdbpg.ast.GaussDBPGAggregate;
import sqlancer.gaussdbpg.ast.GaussDBPGAggregate.GaussDBPGAggregateFunction;
import sqlancer.gaussdbpg.ast.GaussDBPGExpression;
import sqlancer.gaussdbpg.ast.GaussDBPGJoin;
import sqlancer.gaussdbpg.ast.GaussDBPGSelect;
import sqlancer.gaussdbpg.ast.GaussDBPGUnaryPrefixOperation;
import sqlancer.gaussdbpg.ast.GaussDBPGUnaryPrefixOperation.UnaryPrefixOperator;
import sqlancer.gaussdbpg.ast.GaussDBPGUnaryPostfixOperation;
import sqlancer.gaussdbpg.ast.GaussDBPGUnaryPostfixOperation.UnaryPostfixOperator;
import sqlancer.gaussdbpg.ast.GaussDBPGColumnReference;
import sqlancer.gaussdbpg.GaussDBPGSchema.GaussDBPGColumn;

public class GaussDBPGTLPAggregateOracle extends GaussDBPGTLPBase implements TestOracle<GaussDBPGGlobalState> {

    private String firstResult;
    private String secondResult;
    private String originalQuery;
    private String metamorphicQuery;

    public GaussDBPGTLPAggregateOracle(GaussDBPGGlobalState state) {
        super(state);
    }

    @Override
    public void check() throws SQLException {
        super.check();
        aggregateCheck();
    }

    protected void aggregateCheck() throws SQLException {
        GaussDBPGAggregateFunction aggregateFunction = Randomly.fromOptions(
                GaussDBPGAggregateFunction.MAX,
                GaussDBPGAggregateFunction.MIN,
                GaussDBPGAggregateFunction.SUM,
                GaussDBPGAggregateFunction.COUNT);

        // Create a simple aggregate with a column argument
        GaussDBPGColumn column = Randomly.fromList(targetTables.getColumns());
        GaussDBPGExpression columnRef = new GaussDBPGColumnReference(column, null);
        GaussDBPGAggregate aggregate = new GaussDBPGAggregate(Arrays.asList(columnRef), aggregateFunction);

        select.setFetchColumns(Arrays.asList(aggregate));
        if (Randomly.getBooleanWithRatherLowProbability()) {
            select.setOrderByClauses(gen.generateOrderBys());
        }
        originalQuery = GaussDBPGToStringVisitor.asString(select);
        firstResult = getAggregateResult(originalQuery);
        metamorphicQuery = createMetamorphicUnionQuery(select, aggregate, select.getFromList());
        secondResult = getAggregateResult(metamorphicQuery);

        String queryFormatString = "-- %s;\n-- result: %s";
        String firstQueryString = String.format(queryFormatString, originalQuery, firstResult);
        String secondQueryString = String.format(queryFormatString, metamorphicQuery, secondResult);
        state.getState().getLocalState().log(String.format("%s\n%s", firstQueryString, secondQueryString));
        if (firstResult == null && secondResult != null || firstResult != null && secondResult == null
                || firstResult != null && !firstResult.contentEquals(secondResult)
                        && !ComparatorHelper.isEqualDouble(firstResult, secondResult)) {
            if (secondResult != null && secondResult.contains("Inf")) {
                throw new IgnoreMeException();
            }
            String assertionMessage = String.format("the results mismatch!\n%s\n%s", firstQueryString,
                    secondQueryString);
            throw new AssertionError(assertionMessage);
        }
    }

    private String createMetamorphicUnionQuery(GaussDBPGSelect select, GaussDBPGAggregate aggregate,
            List<GaussDBPGExpression> from) {
        String metamorphicQuery;
        GaussDBPGExpression whereClause = gen.generateBooleanExpression();
        GaussDBPGExpression negatedClause = new GaussDBPGUnaryPrefixOperation(whereClause, UnaryPrefixOperator.NOT);
        GaussDBPGExpression isNullClause = new GaussDBPGUnaryPostfixOperation(whereClause, UnaryPostfixOperator.IS_NULL);
        List<GaussDBPGExpression> mappedAggregate = Arrays.asList(aggregate);
        GaussDBPGSelect leftSelect = getSelect(mappedAggregate, from, whereClause, select.getJoinClauses());
        GaussDBPGSelect middleSelect = getSelect(mappedAggregate, from, negatedClause, select.getJoinClauses());
        GaussDBPGSelect rightSelect = getSelect(mappedAggregate, from, isNullClause, select.getJoinClauses());
        metamorphicQuery = "SELECT " + getOuterAggregateFunction(aggregate) + " FROM (";
        metamorphicQuery += GaussDBPGToStringVisitor.asString(leftSelect) + " UNION ALL "
                + GaussDBPGToStringVisitor.asString(middleSelect) + " UNION ALL " + GaussDBPGToStringVisitor.asString(rightSelect);
        metamorphicQuery += ") as asdf";
        return metamorphicQuery;
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
        String resultString;
        SQLQueryAdapter q = new SQLQueryAdapter(queryString, errors);
        try (SQLancerResultSet result = q.executeAndGet(state)) {
            state.getManager().incrementSelectQueryCount();
            if (result == null) {
                throw new IgnoreMeException();
            }
            if (!result.next()) {
                resultString = null;
            } else {
                resultString = result.getString(1);
            }
        } catch (Exception e) {
            throw new AssertionError(queryString, e);
        }
        return resultString;
    }

    private String getOuterAggregateFunction(GaussDBPGAggregate aggregate) {
        GaussDBPGAggregateFunction func = aggregate.getFunc();
        switch (func) {
        case COUNT:
            return "SUM(agg0)";
        default:
            return func.toString() + "(agg0)";
        }
    }

    private GaussDBPGSelect getSelect(List<GaussDBPGExpression> aggregates, List<GaussDBPGExpression> from,
            GaussDBPGExpression whereClause, List<GaussDBPGJoin> joinList) {
        GaussDBPGSelect leftSelect = new GaussDBPGSelect();
        leftSelect.setFetchColumns(aggregates);
        leftSelect.setFromList(from);
        leftSelect.setWhereClause(whereClause);
        leftSelect.setJoinClauses(joinList);
        if (Randomly.getBooleanWithSmallProbability()) {
            List<GaussDBPGExpression> groupByColumns = new ArrayList<>();
            int numGroupBy = Randomly.smallNumber() + 1;
            List<GaussDBPGColumn> availableColumns = targetTables.getColumns();
            for (int i = 0; i < numGroupBy && i < availableColumns.size(); i++) {
                GaussDBPGColumn c = Randomly.fromList(availableColumns);
                groupByColumns.add(new GaussDBPGColumnReference(c, null));
            }
            leftSelect.setGroupByExpressions(groupByColumns);
        }
        return leftSelect;
    }
}