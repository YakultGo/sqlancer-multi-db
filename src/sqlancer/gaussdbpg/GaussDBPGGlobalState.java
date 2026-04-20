package sqlancer.gaussdbpg;

import java.sql.SQLException;

import sqlancer.SQLGlobalState;

public class GaussDBPGGlobalState extends SQLGlobalState<GaussDBPGOptions, GaussDBPGSchema> {

    @Override
    protected GaussDBPGSchema readSchema() throws SQLException {
        return GaussDBPGSchema.fromConnection(getConnection(), getDatabaseName());
    }

    public boolean usesPQS() {
        return getDbmsSpecificOptions().oracles.stream()
                .anyMatch(o -> o == GaussDBPGOracleFactory.PQS);
    }
}