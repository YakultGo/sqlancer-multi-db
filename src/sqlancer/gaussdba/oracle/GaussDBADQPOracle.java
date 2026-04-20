package sqlancer.gaussdba.oracle;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import sqlancer.ComparatorHelper;
import sqlancer.Randomly;
import sqlancer.common.oracle.TestOracle;
import sqlancer.common.query.ExpectedErrors;
import sqlancer.gaussdba.GaussDBAErrors;
import sqlancer.gaussdba.GaussDBAGlobalState;
import sqlancer.gaussdba.GaussDBASchema.GaussDBATables;
import sqlancer.gaussdba.GaussDBAToStringVisitor;
import sqlancer.gaussdba.ast.GaussDBAColumnReference;
import sqlancer.gaussdba.ast.GaussDBAExpression;
import sqlancer.gaussdba.ast.GaussDBAJoin;
import sqlancer.gaussdba.ast.GaussDBASelect;
import sqlancer.gaussdba.ast.GaussDBATableReference;
import sqlancer.gaussdba.gen.GaussDBAExpressionGenerator;

/**
 * Differential Query Planning Oracle for GaussDB A compatibility mode.
 * Verifies that executing the same query multiple times produces consistent results.
 */
public class GaussDBADQPOracle implements TestOracle<GaussDBAGlobalState> {

    private final GaussDBAGlobalState state;
    private GaussDBAExpressionGenerator gen;
    private GaussDBASelect select;
    private final ExpectedErrors errors;

    public GaussDBADQPOracle(GaussDBAGlobalState globalState) {
        state = globalState;
        errors = new ExpectedErrors();
        GaussDBAErrors.addExpressionErrors(errors);
    }

    @Override
    public void check() throws Exception {
        GaussDBATables tables = state.getSchema().getRandomTableNonEmptyTables();
        gen = new GaussDBAExpressionGenerator(state).setColumns(tables.getColumns());

        List<GaussDBAExpression> fetchColumns = new ArrayList<>();
        fetchColumns.addAll(Randomly.nonEmptySubset(tables.getColumns()).stream()
                .map(c -> GaussDBAColumnReference.create(c, null))
                .collect(Collectors.toList()));

        select = new GaussDBASelect();
        select.setFetchColumns(fetchColumns);
        select.setSelectType(Randomly.fromOptions(GaussDBASelect.GaussDBASelectType.values()));

        if (Randomly.getBoolean()) {
            select.setWhereClause(gen.generateBooleanExpression());
        }
        if (Randomly.getBoolean()) {
            select.setGroupByExpressions(fetchColumns);
            if (Randomly.getBoolean()) {
                select.setHavingClause(gen.generateBooleanExpression());
            }
        }

        List<GaussDBAJoin> joinExpressions = GaussDBAJoin.getRandomJoinClauses(
                tables.getTables(), state);
        select.setJoinClauses(joinExpressions);

        List<GaussDBAExpression> tableList = tables.getTables().stream()
                .map(GaussDBATableReference::create)
                .collect(Collectors.toList());
        select.setFromList(tableList);

        if (Randomly.getBoolean()) {
            select.setOrderByClauses(gen.generateOrderBys());
        }

        String originalQueryString = GaussDBAToStringVisitor.asString(select);
        List<String> first = ComparatorHelper.getResultSetFirstColumnAsString(originalQueryString, errors, state);
        List<String> second = ComparatorHelper.getResultSetFirstColumnAsString(originalQueryString, errors, state);

        ComparatorHelper.assumeResultSetsAreEqual(first, second, originalQueryString,
                List.of(originalQueryString, originalQueryString), state);
    }
}