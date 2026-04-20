package sqlancer.gaussdba;

import sqlancer.SQLGlobalState;

public class GaussDBAGlobalState extends SQLGlobalState<GaussDBAOptions, GaussDBASchema> {

    @Override
    protected GaussDBASchema readSchema() throws Exception {
        return GaussDBASchema.fromConnection(getConnection(), getDatabaseName());
    }

    @Override
    public GaussDBASchema getSchema() {
        return super.getSchema();
    }

    public boolean usesPQS() {
        return getDbmsSpecificOptions().getTestOracleFactory()
                .stream().anyMatch(o -> o == GaussDBAOracleFactory.PQS);
    }
}