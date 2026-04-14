package sqlancer.gaussdb;

import java.sql.SQLException;

import sqlancer.SQLGlobalState;

public class GaussDBGlobalState extends SQLGlobalState<GaussDBOptions, GaussDBSchema> {

    @Override
    protected GaussDBSchema readSchema() throws SQLException {
        return GaussDBSchema.fromConnection(getConnection(), getDatabaseName());
    }

    public boolean usesPQS() {
        return getDbmsSpecificOptions().oracles.stream().anyMatch(o -> o == GaussDBOracleFactory.PQS);
    }
}

