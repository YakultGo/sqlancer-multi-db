package sqlancer.gaussdbpg.oracle;

import sqlancer.Randomly;
import sqlancer.common.oracle.TestOracle;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.gaussdbpg.GaussDBPGGlobalState;
import sqlancer.gaussdbpg.GaussDBPGSchema.GaussDBPGTable;

public class GaussDBPGFuzzer implements TestOracle<GaussDBPGGlobalState> {

    private final GaussDBPGGlobalState state;

    public GaussDBPGFuzzer(GaussDBPGGlobalState state) {
        this.state = state;
    }

    @Override
    public void check() throws Exception {
        // Simple fuzzer: execute random SELECT queries
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ");
        if (state.getSchema().getDatabaseTables().isEmpty()) {
            sb.append("1");
        } else {
            GaussDBPGTable table = state.getSchema().getRandomDatabaseTable();
            sb.append("* FROM ");
            sb.append(table.getName());
            if (Randomly.getBoolean()) {
                sb.append(" LIMIT ");
                sb.append((int) Randomly.getNotCachedInteger(1, 100));
            }
        }
        SQLQueryAdapter query = new SQLQueryAdapter(sb.toString(), true);
        state.executeStatement(query);
    }

    @Override
    public String getLastQueryString() {
        return null;
    }
}