package sqlancer.gaussdba;

import java.util.Arrays;
import java.util.List;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import sqlancer.DBMSSpecificOptions;

@Parameters(separators = "=", commandDescription = "GaussDB A Compatibility Mode (Oracle Style)")
public class GaussDBAOptions implements DBMSSpecificOptions<GaussDBAOracleFactory> {

    @Parameter(names = { "--help", "-h" }, description = "Lists all supported options for GaussDB A", help = true)
    public boolean help;

    @Parameter(names = "--oracle", description = "Specifies which test oracle(s) should be used for GaussDB A")
    public List<GaussDBAOracleFactory> oracles = Arrays.asList(GaussDBAOracleFactory.QUERY_PARTITIONING);

    @Parameter(names = "--enable-clob-blob", description = "Enable CLOB/BLOB types")
    public boolean enableClobBlob = false;

    @Parameter(names = "--target-database", description = "A-compatible database to connect (must be created with 'CREATE DATABASE ... WITH dbcompatibility A')")
    public String targetDatabase = null;

    @Override
    public List<GaussDBAOracleFactory> getTestOracleFactory() {
        return oracles;
    }

    public boolean isHelp() {
        return help;
    }
}