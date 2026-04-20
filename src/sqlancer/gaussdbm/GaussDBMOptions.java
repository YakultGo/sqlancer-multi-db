package sqlancer.gaussdbm;

import java.util.Arrays;
import java.util.List;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import sqlancer.DBMSSpecificOptions;

@Parameters(separators = "=", commandDescription = "GaussDB-M (M-Compatibility)")
public class GaussDBMOptions implements DBMSSpecificOptions<GaussDBMOracleFactory> {

    @Parameter(names = { "--help", "-h" }, description = "Lists all supported options for the GaussDB-M command", help = true)
    public boolean help;

    @Parameter(names = "--oracle", description = "Specifies which test oracle(s) should be used for GaussDB-M")
    public List<GaussDBMOracleFactory> oracles = Arrays.asList(GaussDBMOracleFactory.QUERY_PARTITIONING);

    @Parameter(names = "--target-database", description = "Target database name (M-compatible database)")
    public String targetDatabase = null;

    @Override
    public List<GaussDBMOracleFactory> getTestOracleFactory() {
        return oracles;
    }

    public boolean isHelp() {
        return help;
    }
}

