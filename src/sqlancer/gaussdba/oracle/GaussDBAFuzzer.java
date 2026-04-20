package sqlancer.gaussdba.oracle;

import java.sql.SQLException;

import sqlancer.common.oracle.TestOracle;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.gaussdba.GaussDBAGlobalState;
import sqlancer.gaussdba.GaussDBASchema.GaussDBATable;
import sqlancer.gaussdba.gen.GaussDBAInsertGenerator;
import sqlancer.gaussdba.gen.GaussDBATableGenerator;

public class GaussDBAFuzzer implements TestOracle<GaussDBAGlobalState> {

    private final GaussDBAGlobalState state;

    public GaussDBAFuzzer(GaussDBAGlobalState state) {
        this.state = state;
    }

    @Override
    public void check() throws SQLException {
        // Randomly create tables or insert data
        int action = state.getRandomly().getInteger(0, 2);

        switch (action) {
        case 0:
            // Create a new table
            String tableName = "t" + state.getSchema().getDatabaseTables().size();
            SQLQueryAdapter createTable = GaussDBATableGenerator.generate(state, tableName);
            try {
                state.executeStatement(createTable);
            } catch (Exception e) {
                // Ignore errors during table creation in fuzzer
            }
            break;
        case 1:
            // Insert a row into a random table
            if (!state.getSchema().getDatabaseTables().isEmpty()) {
                GaussDBATable table = state.getSchema().getRandomDatabaseTable();
                SQLQueryAdapter insertRow = GaussDBAInsertGenerator.insertRow(state, table);
                try {
                    state.executeStatement(insertRow);
                } catch (Exception e) {
                    // Ignore errors during insert in fuzzer
                }
            }
            break;
        case 2:
            // Execute a simple SELECT
            if (!state.getSchema().getDatabaseTables().isEmpty()) {
                GaussDBATable table = state.getSchema().getRandomDatabaseTable();
                String selectQuery = "SELECT * FROM " + table.getName() + " LIMIT 10";
                SQLQueryAdapter select = new SQLQueryAdapter(selectQuery);
                try {
                    state.executeStatement(select);
                } catch (Exception e) {
                    // Ignore errors during select in fuzzer
                }
            }
            break;
        default:
            throw new AssertionError(action);
        }
    }
}