package sqlancer.gaussdbm.oracle;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import sqlancer.ComparatorHelper;
import sqlancer.Randomly;
import sqlancer.common.oracle.TestOracle;
import sqlancer.common.query.ExpectedErrors;
import sqlancer.gaussdbm.GaussDBMErrors;
import sqlancer.gaussdbm.GaussDBMGlobalState;
import sqlancer.gaussdbm.GaussDBMSchema.GaussDBTables;
import sqlancer.gaussdbm.GaussDBToStringVisitor;
import sqlancer.gaussdbm.ast.GaussDBColumnReference;
import sqlancer.gaussdbm.ast.GaussDBExpression;
import sqlancer.gaussdbm.ast.GaussDBJoin;
import sqlancer.gaussdbm.ast.GaussDBSelect;
import sqlancer.gaussdbm.ast.GaussDBTableReference;
import sqlancer.gaussdbm.gen.GaussDBMExpressionGenerator;

/**
 * Differential query planning: same SELECT under optional hints / session settings should return the same first column.
 * GaussDB-M has no MySQL-style hint / optimizer SET enumeration here; this oracle still validates the baseline SELECT.
 */
public class GaussDBMDQPOracle implements TestOracle<GaussDBMGlobalState> {

    private final GaussDBMGlobalState state;
    private GaussDBMExpressionGenerator gen;
    private GaussDBSelect select;
    private final ExpectedErrors errors = new ExpectedErrors();

    public GaussDBMDQPOracle(GaussDBMGlobalState globalState) {
        state = globalState;
        GaussDBMErrors.addExpressionErrors(errors);
    }

    @Override
    public void check() throws Exception {
        GaussDBTables tables = state.getSchema().getRandomTableNonEmptyTables();
        gen = new GaussDBMExpressionGenerator(state).setColumns(tables.getColumns());
        List<GaussDBExpression> fetchColumns = new ArrayList<>();
        fetchColumns.addAll(Randomly.nonEmptySubset(tables.getColumns()).stream()
                .map(c -> GaussDBColumnReference.create(c, null)).collect(Collectors.toList()));

        select = new GaussDBSelect();
        select.setFetchColumns(fetchColumns);
        select.setSelectType(Randomly.fromOptions(GaussDBSelect.SelectType.values()));
        if (Randomly.getBoolean()) {
            select.setWhereClause(gen.generateExpression());
        }
        if (Randomly.getBoolean()) {
            select.setGroupByExpressions(fetchColumns);
            if (Randomly.getBoolean()) {
                select.setHavingClause(gen.generateExpression());
            }
        }

        List<GaussDBJoin> joinExpressions = GaussDBJoin.getRandomJoinClauses(tables.getTables(), state);
        select.setJoinList(joinExpressions.stream().map(j -> (GaussDBExpression) j).collect(Collectors.toList()));

        List<GaussDBExpression> tableList = tables.getTables().stream().map(GaussDBTableReference::create)
                .collect(Collectors.toList());
        select.setFromList(tableList);

        String originalQueryString = GaussDBToStringVisitor.asString(select);
        List<String> first = ComparatorHelper.getResultSetFirstColumnAsString(originalQueryString, errors, state);
        List<String> second = ComparatorHelper.getResultSetFirstColumnAsString(originalQueryString, errors, state);
        ComparatorHelper.assumeResultSetsAreEqual(first, second, originalQueryString,
                List.of(originalQueryString, originalQueryString), state);
    }
}
