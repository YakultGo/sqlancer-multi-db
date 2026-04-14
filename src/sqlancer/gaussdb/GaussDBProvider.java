package sqlancer.gaussdb;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import sqlancer.MainOptions;
import sqlancer.SQLConnection;
import sqlancer.SQLProviderAdapter;
import sqlancer.common.DBMSCommon;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.gaussdb.gen.GaussDBInsertGenerator;
import sqlancer.gaussdb.gen.GaussDBTableGenerator;

/**
 * GaussDB (M-Compatibility) provider with an independent AST/generator stack.
 */
public class GaussDBProvider extends SQLProviderAdapter<GaussDBGlobalState, GaussDBOptions> {

    public GaussDBProvider() {
        super(GaussDBGlobalState.class, GaussDBOptions.class);
    }

    @Override
    public void generateDatabase(GaussDBGlobalState globalState) throws Exception {
        while (globalState.getSchema().getDatabaseTables().size() < (int) sqlancer.Randomly.getNotCachedInteger(1, 2)) {
            String tableName = DBMSCommon.createTableName(globalState.getSchema().getDatabaseTables().size());
            SQLQueryAdapter createTable = GaussDBTableGenerator.generate(globalState, tableName);
            globalState.executeStatement(createTable);
        }
        int inserts = (int) sqlancer.Randomly.getNotCachedInteger(1, globalState.getOptions().getMaxNumberInserts());
        for (int i = 0; i < inserts; i++) {
            globalState.executeStatement(GaussDBInsertGenerator.insertRow(globalState));
        }
    }

    @Override
    public SQLConnection createDatabase(GaussDBGlobalState globalState) throws SQLException {
        MainOptions options = globalState.getOptions();
        loadDriverIfRequested(options);

        String username = options.getUserName();
        String password = options.getPassword();

        String jdbcUrl = options.getConnectionURL();
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            throw new AssertionError(
                    "GaussDB requires --connection-url. Example: --connection-url=jdbc:gaussdb://127.0.0.1:8000/test");
        }
        jdbcUrl = jdbcUrl.trim();
        if (!jdbcUrl.startsWith("jdbc:")) {
            jdbcUrl = "jdbc:" + jdbcUrl;
        }

        Properties props = parseJdbcProperties(options.getJdbcProperties());
        if (username != null) {
            props.setProperty("user", username);
        }
        if (password != null) {
            props.setProperty("password", password);
        }

        java.sql.Connection con = DriverManager.getConnection(jdbcUrl, props);
        String databaseName = globalState.getDatabaseName();

        if (options.useCreateDatabase()) {
            try (Statement s = con.createStatement()) {
                globalState.getState().logStatement("DROP DATABASE IF EXISTS " + databaseName);
                globalState.getState().logStatement("CREATE DATABASE " + databaseName);
                s.execute("DROP DATABASE IF EXISTS " + databaseName);
                s.execute("CREATE DATABASE " + databaseName);
                // Switch to newly created database if supported.
                try {
                    s.execute("USE " + databaseName);
                } catch (SQLException ignored) {
                }
            }
            return new SQLConnection(con);
        }

        // Fallback: schema-based isolation (more likely to work with limited privileges).
        try (Statement s = con.createStatement()) {
            globalState.getState().logStatement("DROP SCHEMA IF EXISTS " + databaseName);
            globalState.getState().logStatement("CREATE SCHEMA " + databaseName);
            s.execute("DROP SCHEMA IF EXISTS " + databaseName);
            s.execute("CREATE SCHEMA " + databaseName);
            try {
                s.execute("USE " + databaseName);
            } catch (SQLException ignored) {
                // Some servers use search_path instead of USE in this mode.
                try {
                    s.execute("SET search_path TO " + databaseName);
                } catch (SQLException ignored2) {
                }
            }
        }

        return new SQLConnection(con);
    }

    private static void loadDriverIfRequested(MainOptions opt) {
        String cls = opt.getJdbcDriverClass();
        if (cls == null || cls.isBlank()) {
            return;
        }
        try {
            Class.forName(cls);
        } catch (ClassNotFoundException e) {
            throw new AssertionError("JDBC driver class not found: " + cls, e);
        }
    }

    private static Properties parseJdbcProperties(String propString) {
        Properties props = new Properties();
        if (propString == null || propString.isBlank()) {
            return props;
        }
        String[] parts = propString.split(";");
        for (String p : parts) {
            String trimmed = p.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String[] kv = trimmed.split("=", 2);
            if (kv.length != 2) {
                continue;
            }
            props.setProperty(kv[0].trim(), kv[1].trim());
        }
        return props;
    }

    @Override
    public String getDBMSName() {
        return "gaussdb";
    }

    public static void printOracleHelp() {
        System.out.println();
        System.out.println("GaussDB --oracle choices: " + java.util.Arrays.toString(GaussDBOracleFactory.values()));
    }

    @Override
    public boolean addRowsToAllTables(GaussDBGlobalState globalState) throws Exception {
        for (GaussDBSchema.GaussDBTable table : globalState.getSchema().getDatabaseTables()) {
            if (table.getNrRows(globalState) == 0) {
                globalState.executeStatement(GaussDBInsertGenerator.insertRow(globalState, table));
            }
        }
        return true;
    }
}

