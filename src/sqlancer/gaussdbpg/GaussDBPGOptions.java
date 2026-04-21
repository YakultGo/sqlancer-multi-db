package sqlancer.gaussdbpg;

import java.util.Arrays;
import java.util.List;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import sqlancer.DBMSSpecificOptions;

@Parameters(separators = "=", commandDescription = "GaussDB PG Compatibility Mode")
public class GaussDBPGOptions implements DBMSSpecificOptions<GaussDBPGOracleFactory> {

    @Parameter(names = { "--help", "-h" }, description = "Lists all supported options for GaussDB PG", help = true, hidden = true)
    public boolean help;

    @Parameter(names = "--oracle", description = "Specifies which test oracle should be used, Options: [AGGREGATE, CERT, DISTINCT, DQE, DQP, EET, FUZZER, GROUP_BY, HAVING, NOREC, PQS, QUERY_PARTITIONING, TLP_WHERE]")
    public List<GaussDBPGOracleFactory> oracles = Arrays.asList(GaussDBPGOracleFactory.QUERY_PARTITIONING);

    @Parameter(names = "--enable-time-types", description = "Enable DATE, TIME, TIMESTAMP types")
    public boolean enableTimeTypes = false;

    @Parameter(names = "--target-database", description = "PG-compatible database to connect (must be created with 'CREATE DATABASE ... WITH dbcompatibility pg')")
    public String targetDatabase = null;

    @Override
    public List<GaussDBPGOracleFactory> getTestOracleFactory() {
        return oracles;
    }

    public boolean isHelp() {
        return help;
    }
}