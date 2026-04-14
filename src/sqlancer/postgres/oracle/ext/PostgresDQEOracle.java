package sqlancer.postgres.oracle.ext;

import static sqlancer.ComparatorHelper.getResultSetFirstColumnAsString;

import java.sql.SQLException;
import java.util.List;

import sqlancer.ComparatorHelper;
import sqlancer.IgnoreMeException;
import sqlancer.Randomly;
import sqlancer.common.oracle.TestOracle;
import sqlancer.common.query.ExpectedErrors;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.postgres.PostgresGlobalState;
import sqlancer.postgres.PostgresSchema.PostgresDataType;
import sqlancer.postgres.PostgresSchema.PostgresTable;
import sqlancer.postgres.PostgresVisitor;
import sqlancer.postgres.ast.PostgresExpression;
import sqlancer.postgres.gen.PostgresCommon;
import sqlancer.postgres.gen.PostgresExpressionGenerator;

public final class PostgresDQEOracle implements TestOracle<PostgresGlobalState> {

    private static final String COLUMN_ROWID = "rowId";
    private static final String COLUMN_UPDATED = "updated";

    private final PostgresGlobalState state;
    private final ExpectedErrors errors = new ExpectedErrors();

    public PostgresDQEOracle(PostgresGlobalState globalState) {
        if (globalState == null) {
            throw new IllegalArgumentException("globalState must not be null");
        }
        this.state = globalState;
        PostgresCommon.addCommonExpressionErrors(errors);
        PostgresCommon.addCommonFetchErrors(errors);
        PostgresCommon.addCommonInsertUpdateErrors(errors);
    }

    @Override
    public void check() throws SQLException {
        PostgresTable table = Randomly.fromList(state.getSchema().getRandomTableNonEmptyTables().getTables());
        String tableName = table.getName();

        addAuxiliaryColumns(tableName);

        PostgresExpressionGenerator gen = new PostgresExpressionGenerator(state).setColumns(table.getColumns());
        PostgresExpression where = gen.generateExpression(0, PostgresDataType.BOOLEAN);
        String whereStr = PostgresVisitor.asString(where);

        // Use a transaction with savepoints so DQE leaves the database unchanged.
        new SQLQueryAdapter("BEGIN").execute(state, false);
        try {
            new SQLQueryAdapter("SAVEPOINT dqe_sp1").execute(state, false);

            String selectStmt = String.format("SELECT %s FROM %s WHERE %s", COLUMN_ROWID, tableName, whereStr);
            List<String> selectRows = getResultSetFirstColumnAsString(selectStmt, errors, state);

            String updateStmt = String.format("UPDATE %s SET %s = 1 WHERE %s", tableName, COLUMN_UPDATED, whereStr);
            new SQLQueryAdapter(updateStmt, errors).execute(state, false);

            String selectUpdatedStmt = String.format("SELECT %s FROM %s WHERE %s = 1", COLUMN_ROWID, tableName,
                    COLUMN_UPDATED);
            List<String> updatedRows = getResultSetFirstColumnAsString(selectUpdatedStmt, errors, state);
            ComparatorHelper.assumeResultSetsAreEqual(selectRows, updatedRows, selectStmt,
                    List.of(selectStmt, updateStmt, selectUpdatedStmt), state);

            new SQLQueryAdapter("ROLLBACK TO SAVEPOINT dqe_sp1").execute(state, false);
            new SQLQueryAdapter("SAVEPOINT dqe_sp2").execute(state, false);

            String deleteStmt = String.format("DELETE FROM %s WHERE %s RETURNING %s", tableName, whereStr,
                    COLUMN_ROWID);
            List<String> deletedRows = getResultSetFirstColumnAsString(deleteStmt, errors, state);
            ComparatorHelper.assumeResultSetsAreEqual(selectRows, deletedRows, selectStmt,
                    List.of(selectStmt, deleteStmt), state);

            new SQLQueryAdapter("ROLLBACK TO SAVEPOINT dqe_sp2").execute(state, false);
        } catch (AssertionError e) {
            throw e;
        } catch (SQLException e) {
            throw new IgnoreMeException();
        } finally {
            new SQLQueryAdapter("ROLLBACK").execute(state, false);
        }
    }

    private void addAuxiliaryColumns(String tableName) throws SQLException {
        new SQLQueryAdapter(String.format("ALTER TABLE %s DROP COLUMN IF EXISTS %s", tableName, COLUMN_ROWID))
                .execute(state, false);
        new SQLQueryAdapter(String.format("ALTER TABLE %s DROP COLUMN IF EXISTS %s", tableName, COLUMN_UPDATED))
                .execute(state, false);

        new SQLQueryAdapter(String.format("ALTER TABLE %s ADD COLUMN %s TEXT", tableName, COLUMN_ROWID))
                .execute(state, false);
        new SQLQueryAdapter(String.format("ALTER TABLE %s ADD COLUMN %s INT DEFAULT 0", tableName, COLUMN_UPDATED))
                .execute(state, false);

        // No extension dependency; good-enough uniqueness for oracle bookkeeping.
        new SQLQueryAdapter(String.format("UPDATE %s SET %s = md5(random()::text || clock_timestamp()::text)",
                tableName, COLUMN_ROWID)).execute(state, false);
    }
}

