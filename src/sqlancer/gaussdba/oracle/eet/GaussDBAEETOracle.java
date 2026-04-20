package sqlancer.gaussdba.oracle.eet;

import java.sql.SQLException;
import java.util.List;

import sqlancer.IgnoreMeException;
import sqlancer.Reproducer;
import sqlancer.common.oracle.TestOracle;
import sqlancer.common.oracle.TestOracleUtils;
import sqlancer.common.schema.AbstractTables;
import sqlancer.gaussdba.GaussDBAGlobalState;
import sqlancer.gaussdba.GaussDBASchema.GaussDBAColumn;
import sqlancer.gaussdba.GaussDBASchema.GaussDBATable;
import sqlancer.gaussdba.GaussDBAToStringVisitor;
import sqlancer.gaussdba.ast.GaussDBAExpression;
import sqlancer.gaussdba.gen.GaussDBAExpressionGenerator;

public class GaussDBAEETOracle implements TestOracle<GaussDBAGlobalState> {

    public static final int MAX_PROCESS_ROW_NUM = 10_000;

    private final GaussDBAGlobalState state;
    private final EETQueryExecutor executor;
    private String lastQueryString;
    private Reproducer<GaussDBAGlobalState> lastReproducer;

    public GaussDBAEETOracle(GaussDBAGlobalState state) {
        this(state, null);
    }

    public GaussDBAEETOracle(GaussDBAGlobalState state, EETQueryExecutor executor) {
        this.state = state;
        this.executor = executor != null ? executor : new GaussDBAEETDefaultQueryExecutor(state);
    }

    @Override
    public void check() throws SQLException {
        lastReproducer = null;

        AbstractTables<GaussDBATable, GaussDBAColumn> targetTables = TestOracleUtils
                .getRandomTableNonEmptyTables(state.getSchema());
        GaussDBAExpressionGenerator gen = new GaussDBAExpressionGenerator(state).setTablesAndColumns(targetTables);
        GaussDBAExpression root = GaussDBAEETQueryGenerator.generateEETQueryRandomShape(state, gen, targetTables);

        GaussDBAEETQueryTransformer qtf = new GaussDBAEETQueryTransformer(gen);
        long reductionSeed = state.getRandomly().getSeed();
        GaussDBAExpression transformed = qtf.eqTransformRoot(root);

        String originalQuery = GaussDBAToStringVisitor.asString(root);
        String transformedQuery = GaussDBAToStringVisitor.asString(transformed);
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
            lastReproducer = new GaussDBAEETReproducer(root, transformed, gen, targetTables, reductionSeed, executor);
            throw new AssertionError(String.format("EET logic bug: multiset mismatch\n%s\n", lastQueryString));
        }
    }

    @Override
    public String getLastQueryString() {
        return lastQueryString;
    }

    @Override
    public Reproducer<GaussDBAGlobalState> getLastReproducer() {
        return lastReproducer;
    }
}