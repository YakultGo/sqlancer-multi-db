package sqlancer.postgres.oracle.ext;

import java.sql.SQLException;
import sqlancer.IgnoreMeException;
import sqlancer.common.oracle.CODDTestBase;
import sqlancer.common.oracle.TestOracle;
import sqlancer.common.oracle.TestOracleUtils;
import sqlancer.common.query.ExpectedErrors;
import sqlancer.postgres.PostgresGlobalState;
import sqlancer.postgres.PostgresSchema.PostgresColumn;
import sqlancer.postgres.PostgresSchema.PostgresTable;
import sqlancer.postgres.PostgresVisitor;
import sqlancer.postgres.ast.PostgresConstant;
import sqlancer.postgres.ast.PostgresExpression;
import sqlancer.postgres.ast.PostgresOracleExpressionBag;
import sqlancer.postgres.gen.PostgresCommon;
import sqlancer.postgres.gen.PostgresExpressionGenerator;

/**
 * CODDTEST "framework style" for PostgreSQL.
 *
 * This keeps the base structure (auxiliary/original/folded query strings) and focuses on a stable folding scenario:
 * fold a boolean predicate by first evaluating it as a projection (aux query), then replacing it with a constant.
 */
public final class PostgresCODDTestOracle extends CODDTestBase<PostgresGlobalState>
        implements TestOracle<PostgresGlobalState> {

    public PostgresCODDTestOracle(PostgresGlobalState globalState) {
        super(globalState);
        PostgresCommon.addCommonExpressionErrors(errors);
        PostgresCommon.addCommonFetchErrors(errors);
        errors.addAll(PostgresCommon.getCommonInsertUpdateErrors());
    }

    @Override
    public void check() throws SQLException {
        sqlancer.common.schema.AbstractTables<PostgresTable, PostgresColumn> tables = TestOracleUtils
                .getRandomTableNonEmptyTables(state.getSchema());
        PostgresExpressionGenerator gen = new PostgresExpressionGenerator(state).setTablesAndColumns(tables);

        PostgresExpression foldedExpr = gen.generateBooleanExpression();
        PostgresOracleExpressionBag bag = new PostgresOracleExpressionBag(foldedExpr);

        // Auxiliary query: evaluate the predicate once to get a constant boolean (or NULL).
        auxiliaryQueryString = String.format("SELECT (%s) AS ref0 FROM %s LIMIT 1", PostgresVisitor.asString(bag),
                tables.getTables().get(0).getName());
        PostgresConstant constRes = evalBooleanConstant(auxiliaryQueryString);
        if (constRes == null) {
            throw new IgnoreMeException();
        }

        String tableList = tables.getTables().get(0).getName();
        originalQueryString = String.format("SELECT COUNT(*) FROM %s WHERE %s", tableList, PostgresVisitor.asString(bag));
        bag.updateInnerExpr(constRes);
        foldedQueryString = String.format("SELECT COUNT(*) FROM %s WHERE %s", tableList, PostgresVisitor.asString(bag));

        String originalCount = getSingleString(originalQueryString);
        String foldedCount = getSingleString(foldedQueryString);
        if (originalCount == null || foldedCount == null) {
            throw new IgnoreMeException();
        }
        if (!originalCount.equals(foldedCount)) {
            throw new AssertionError("CODDTEST logic bug: COUNT mismatch\n" + originalQueryString + "\n"
                    + foldedQueryString + "\naux=" + auxiliaryQueryString);
        }
    }

    private PostgresConstant evalBooleanConstant(String sql) throws SQLException {
        String s = getSingleString(sql);
        if (s == null) {
            return PostgresConstant.createNullConstant();
        }
        String v = s.trim().toLowerCase();
        if (v.equals("t") || v.equals("true") || v.equals("1")) {
            return PostgresConstant.createBooleanConstant(true);
        }
        if (v.equals("f") || v.equals("false") || v.equals("0")) {
            return PostgresConstant.createBooleanConstant(false);
        }
        return PostgresConstant.createNullConstant();
    }

    private String getSingleString(String sql) throws SQLException {
        sqlancer.common.query.SQLQueryAdapter q = new sqlancer.common.query.SQLQueryAdapter(sql, (ExpectedErrors) null);
        try (sqlancer.common.query.SQLancerResultSet rs = q.executeAndGet(state)) {
            if (rs == null) {
                throw new IgnoreMeException();
            }
            if (!rs.next()) {
                return null;
            }
            return rs.getString(1);
        } catch (SQLException e) {
            throw e;
        } catch (AssertionError e) {
            throw e;
        }
    }
}

