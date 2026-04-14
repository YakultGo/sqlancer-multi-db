package sqlancer.postgres.oracle.ext;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import sqlancer.ComparatorHelper;
import sqlancer.Randomly;
import sqlancer.common.oracle.TestOracle;
import sqlancer.common.query.ExpectedErrors;
import sqlancer.postgres.PostgresGlobalState;
import sqlancer.postgres.PostgresSchema.PostgresColumn;
import sqlancer.postgres.PostgresSchema.PostgresTable;
import sqlancer.postgres.PostgresVisitor;
import sqlancer.postgres.ast.PostgresColumnValue;
import sqlancer.postgres.ast.PostgresExpression;
import sqlancer.postgres.ast.PostgresSelect;
import sqlancer.postgres.ast.PostgresSelect.PostgresFromTable;
import sqlancer.postgres.gen.PostgresCommon;
import sqlancer.postgres.gen.PostgresExpressionGenerator;

public final class PostgresDQPOracle implements TestOracle<PostgresGlobalState> {

    private final PostgresGlobalState state;
    private final ExpectedErrors errors = new ExpectedErrors();

    public PostgresDQPOracle(PostgresGlobalState globalState) {
        if (globalState == null) {
            throw new IllegalArgumentException("globalState must not be null");
        }
        this.state = globalState;
        PostgresCommon.addCommonExpressionErrors(errors);
        PostgresCommon.addCommonFetchErrors(errors);
        PostgresCommon.addGroupingErrors(errors);
    }

    @Override
    public void check() throws Exception {
        sqlancer.postgres.PostgresSchema.PostgresTables tables = state.getSchema().getRandomTableNonEmptyTables();
        List<PostgresTable> pickedTables = Randomly.nonEmptySubset(tables.getTables());
        List<PostgresColumn> columns = tables.getColumns();
        PostgresExpressionGenerator gen = new PostgresExpressionGenerator(state).setColumns(columns);

        List<PostgresExpression> fetchColumns = new ArrayList<>();
        for (PostgresColumn c : Randomly.nonEmptySubset(columns)) {
            fetchColumns.add(new PostgresColumnValue(c, null));
        }

        PostgresSelect select = new PostgresSelect();
        select.setFetchColumns(fetchColumns);
        select.setFromList(pickedTables.stream().map(t -> new PostgresFromTable(t, false)).collect(Collectors.toList()));
        if (Randomly.getBoolean()) {
            select.setWhereClause(gen.generateExpression(0, sqlancer.postgres.PostgresSchema.PostgresDataType.BOOLEAN));
        }
        if (Randomly.getBoolean()) {
            select.setGroupByExpressions(fetchColumns);
            if (Randomly.getBoolean()) {
                select.setHavingClause(
                        gen.generateExpression(0, sqlancer.postgres.PostgresSchema.PostgresDataType.BOOLEAN));
            }
        }

        String query = PostgresVisitor.asString(select);
        List<String> first = ComparatorHelper.getResultSetFirstColumnAsString(query, errors, state);
        List<String> second = ComparatorHelper.getResultSetFirstColumnAsString(query, errors, state);
        ComparatorHelper.assumeResultSetsAreEqual(first, second, query, List.of(query, query), state);
    }
}

