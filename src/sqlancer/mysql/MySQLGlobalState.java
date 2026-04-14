
package sqlancer.mysql;

import java.sql.SQLException;

import sqlancer.SQLGlobalState;

public class MySQLGlobalState extends SQLGlobalState<MySQLOptions, MySQLSchema> {

    private boolean supportsTableCompression = true;

    @Override
    protected MySQLSchema readSchema() throws SQLException {
        return MySQLSchema.fromConnection(getConnection(), getDatabaseName());
    }

    public boolean usesPQS() {
        return getDbmsSpecificOptions().oracles.stream().anyMatch(o -> o == MySQLOracleFactory.PQS);
    }

    public boolean supportsTableCompression() {
        return supportsTableCompression;
    }

    public void setSupportsTableCompression(boolean supported) {
        this.supportsTableCompression = supported;
    }

}
