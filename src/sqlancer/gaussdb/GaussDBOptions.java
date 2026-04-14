package sqlancer.gaussdb;

import java.util.Arrays;
import java.util.List;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import sqlancer.DBMSSpecificOptions;

@Parameters(separators = "=", commandDescription = "GaussDB (M-Compatibility)")
public class GaussDBOptions implements DBMSSpecificOptions<GaussDBOracleFactory> {

    @Parameter(names = { "--help", "-h" }, description = "Lists all supported options for the GaussDB command", help = true)
    public boolean help;

    @Parameter(names = "--oracle", description = "Specifies which test oracle(s) should be used for GaussDB")
    public List<GaussDBOracleFactory> oracles = Arrays.asList(GaussDBOracleFactory.QUERY_PARTITIONING);

    @Override
    public List<GaussDBOracleFactory> getTestOracleFactory() {
        return oracles;
    }

    public boolean isHelp() {
        return help;
    }
}

