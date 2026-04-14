package sqlancer.postgres.oracle.ext.eet;

import java.sql.SQLException;
import java.util.List;

import sqlancer.IgnoreMeException;
import sqlancer.Reproducer;
import sqlancer.common.oracle.TestOracle;
import sqlancer.common.oracle.TestOracleUtils;
import sqlancer.common.schema.AbstractTables;
import sqlancer.postgres.PostgresGlobalState;
import sqlancer.postgres.PostgresSchema.PostgresColumn;
import sqlancer.postgres.PostgresSchema.PostgresTable;
import sqlancer.postgres.PostgresVisitor;
import sqlancer.postgres.ast.PostgresExpression;
import sqlancer.postgres.gen.PostgresExpressionGenerator;

public final class PostgresEETOracle implements TestOracle<PostgresGlobalState> {

    public static final int MAX_PROCESS_ROW_NUM = 10_000;

    private final PostgresGlobalState state;
    private final EETQueryExecutor executor;
    private String lastQueryString;
    private Reproducer<PostgresGlobalState> lastReproducer;

    public PostgresEETOracle(PostgresGlobalState state) {
        this(state, null);
    }

    public PostgresEETOracle(PostgresGlobalState state, EETQueryExecutor executor) {
        this.state = state;
        this.executor = executor != null ? executor : new PostgresEETDefaultQueryExecutor(state);
    }

    @Override
    public void check() throws SQLException {
        lastReproducer = null;

        AbstractTables<PostgresTable, PostgresColumn> targetTables = TestOracleUtils
                .getRandomTableNonEmptyTables(state.getSchema());
        PostgresExpressionGenerator gen = new PostgresExpressionGenerator(state).setTablesAndColumns(targetTables);
        PostgresExpression root = PostgresEETQueryGenerator.generateEETQueryRandomShape(state, gen, targetTables);

        PostgresEETQueryTransformer qtf = new PostgresEETQueryTransformer(gen);
        long reductionSeed = state.getRandomly().getSeed();
        PostgresExpression transformed = qtf.eqTransformRoot(root);

        String originalQuery = PostgresVisitor.asString(root);
        String transformedQuery = PostgresVisitor.asString(transformed);
        lastQueryString = originalQuery + "\n-- EET transformed:\n" + transformedQuery;

        if (state.getOptions().logEachSelect()) {
            state.getLogger().writeCurrent(lastQueryString);
        }

        List<List<String>> originalResult;
        List<List<String>> transformedResult;
        try {
            originalResult = executor.executeQuery(originalQuery);
            transformedResult = executor.executeQuery(transformedQuery);
        } catch (IgnoreMeException e) {
            throw e;
        } catch (SQLException e) {
            throw new IgnoreMeException();
        }

        if (originalResult.size() > MAX_PROCESS_ROW_NUM || transformedResult.size() > MAX_PROCESS_ROW_NUM) {
            throw new IgnoreMeException();
        }

        if (EETMultisetComparator.compareResultMultisets(originalResult, transformedResult)) {
            return;
        }

        // retry once to reduce flakiness
        List<List<String>> orig2;
        List<List<String>> trans2;
        try {
            orig2 = executor.executeQuery(originalQuery);
            trans2 = executor.executeQuery(transformedQuery);
        } catch (SQLException e) {
            throw new IgnoreMeException();
        }
        if (!EETMultisetComparator.compareResultMultisets(orig2, trans2)) {
            lastReproducer = new PostgresEETReproducer(root, transformed, gen, targetTables, reductionSeed, executor);
            throw new AssertionError(String.format("EET logic bug: multiset mismatch\n%s\n", lastQueryString));
        }
    }

    @Override
    public String getLastQueryString() {
        return lastQueryString;
    }

    @Override
    public Reproducer<PostgresGlobalState> getLastReproducer() {
        return lastReproducer;
    }
}

