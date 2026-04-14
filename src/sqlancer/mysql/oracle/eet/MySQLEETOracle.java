package sqlancer.mysql.oracle.eet;

import java.sql.SQLException;
import java.util.List;

import sqlancer.IgnoreMeException;
import sqlancer.Reproducer;
import sqlancer.common.oracle.TestOracle;
import sqlancer.common.oracle.TestOracleUtils;
import sqlancer.common.schema.AbstractTables;
import sqlancer.mysql.MySQLGlobalState;
import sqlancer.mysql.MySQLSchema.MySQLColumn;
import sqlancer.mysql.MySQLSchema.MySQLTable;
import sqlancer.mysql.MySQLVisitor;
import sqlancer.mysql.ast.MySQLExpression;
import sqlancer.mysql.gen.MySQLExpressionGenerator;

public class MySQLEETOracle implements TestOracle<MySQLGlobalState> {

    public static final int MAX_PROCESS_ROW_NUM = 10_000;

    private final MySQLGlobalState state;
    private final EETQueryExecutor executor;
    private String lastQueryString;
    private Reproducer<MySQLGlobalState> lastReproducer;

    public MySQLEETOracle(MySQLGlobalState state) {
        this(state, null);
    }

    public MySQLEETOracle(MySQLGlobalState state, EETQueryExecutor executor) {
        this.state = state;
        this.executor = executor != null ? executor : new EETDefaultQueryExecutor(state);
    }

    @Override
    public void check() throws SQLException {
        lastReproducer = null;
        AbstractTables<MySQLTable, MySQLColumn> targetTables = TestOracleUtils.getRandomTableNonEmptyTables(state.getSchema());
        MySQLExpressionGenerator gen = new MySQLExpressionGenerator(state).setTablesAndColumns(targetTables);
        MySQLExpression root = MySQLEETQueryGenerator.generateEETQueryRandomShape(state, gen, targetTables);

        MySQLEETQueryTransformer qtf = new MySQLEETQueryTransformer(gen);
        long reductionSeed = state.getRandomly().getSeed();
        MySQLExpression transformed = qtf.eqTransformRoot(root);

        String originalQuery = MySQLVisitor.asString(root);
        String transformedQuery = MySQLVisitor.asString(transformed);
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

        List<List<String>> orig2;
        List<List<String>> trans2;
        try {
            orig2 = executor.executeQuery(originalQuery);
            trans2 = executor.executeQuery(transformedQuery);
        } catch (SQLException e) {
            throw new IgnoreMeException();
        }
        if (!EETMultisetComparator.compareResultMultisets(orig2, trans2)) {
            lastReproducer = new MySQLEETReproducer(root, transformed, gen, targetTables, reductionSeed, executor);
            throw new AssertionError(String.format("EET logic bug: multiset mismatch\n%s\n", lastQueryString));
        }
    }

    @Override
    public String getLastQueryString() {
        return lastQueryString;
    }

    @Override
    public Reproducer<MySQLGlobalState> getLastReproducer() {
        return lastReproducer;
    }
}
