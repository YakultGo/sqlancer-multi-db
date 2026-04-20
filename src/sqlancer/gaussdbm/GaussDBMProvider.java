package sqlancer.gaussdbm;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import com.google.auto.service.AutoService;

import sqlancer.MainOptions;
import sqlancer.Randomly;
import sqlancer.SQLConnection;
import sqlancer.SQLProviderAdapter;
import sqlancer.common.DBMSCommon;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.gaussdbm.gen.GaussDBMInsertGenerator;
import sqlancer.gaussdbm.gen.GaussDBMTableGenerator;

/**
 * GaussDB-M (M-Compatibility) provider.
 */
@AutoService(sqlancer.DatabaseProvider.class)
public class GaussDBMProvider extends SQLProviderAdapter<GaussDBMGlobalState, GaussDBMOptions> {

    private static boolean driverLoaded = false;

    public GaussDBMProvider() {
        super(GaussDBMGlobalState.class, GaussDBMOptions.class);
    }

    private static synchronized void loadDriver() {
        if (driverLoaded) {
            return;
        }
        // Try to load openGauss driver first (recommended for GaussDB)
        try {
            Class.forName("org.opengauss.Driver");
            System.err.println("[INFO] Loaded openGauss JDBC driver (org.opengauss.Driver)");
            driverLoaded = true;
            return;
        } catch (ClassNotFoundException e) {
            System.err.println("[INFO] openGauss driver not found, trying MySQL driver...");
        }
        // Fallback to MySQL driver for M-compatibility mode
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.err.println("[INFO] Loaded MySQL JDBC driver (com.mysql.cj.jdbc.Driver)");
            driverLoaded = true;
        } catch (ClassNotFoundException e) {
            throw new AssertionError("No JDBC driver available. Please ensure opengauss-jdbc or mysql driver is in classpath.", e);
        }
    }

    @Override
    public void generateDatabase(GaussDBMGlobalState globalState) throws Exception {
        while (globalState.getSchema().getDatabaseTables().size() < (int) Randomly.getNotCachedInteger(1, 2)) {
            String tableName = DBMSCommon.createTableName(globalState.getSchema().getDatabaseTables().size());
            SQLQueryAdapter createTable = GaussDBMTableGenerator.generate(globalState, tableName);
            globalState.executeStatement(createTable);
        }
        int inserts = (int) Randomly.getNotCachedInteger(1, globalState.getOptions().getMaxNumberInserts());
        for (int i = 0; i < inserts; i++) {
            globalState.executeStatement(GaussDBMInsertGenerator.insertRow(globalState));
        }
    }

    @Override
    public SQLConnection createDatabase(GaussDBMGlobalState globalState) throws SQLException {
        loadDriver();

        MainOptions options = globalState.getOptions();
        String username = options.getUserName();
        String password = options.getPassword();
        String host = options.getHost();
        int port = options.getPort();

        GaussDBMOptions gaussdbOptions = globalState.getDbmsSpecificOptions();
        String targetDatabase = gaussdbOptions != null && gaussdbOptions.targetDatabase != null
                ? gaussdbOptions.targetDatabase : "postgres";

        String baseParams = "sslmode=disable&connectTimeout=10&socketTimeout=30";

        Connection con = null;
        SQLException lastError = null;
        String jdbcUrl = null;

        String configuredUrl = options.getConnectionURL();
        if (configuredUrl != null && !configuredUrl.isBlank()) {
            jdbcUrl = configuredUrl.trim();
            if (!jdbcUrl.startsWith("jdbc:")) {
                jdbcUrl = "jdbc:" + jdbcUrl;
            }
            if (!jdbcUrl.contains("sslmode")) {
                jdbcUrl = jdbcUrl + (jdbcUrl.contains("?") ? "&" : "?") + baseParams;
            }
            System.err.println("[INFO] Using configured URL: " + jdbcUrl);

            Properties props = parseJdbcProperties(options.getJdbcProperties());
            if (username != null) {
                props.setProperty("user", username);
            }
            if (password != null) {
                props.setProperty("password", password);
            }

            try {
                con = DriverManager.getConnection(jdbcUrl, props);
            } catch (SQLException e) {
                lastError = e;
                System.err.println("[ERROR] Connection failed: " + e.getMessage());
            }
        } else {
            // Try multiple URL schemes for GaussDB M-compatibility
            String[] urlSchemes = { "opengauss", "gaussdb", "mysql" };

            for (String scheme : urlSchemes) {
                jdbcUrl = String.format("jdbc:%s://%s:%d/%s?%s", scheme, host, port, targetDatabase, baseParams);
                System.err.println("[INFO] Trying connection URL: " + jdbcUrl);

                Properties props = parseJdbcProperties(options.getJdbcProperties());
                if (username != null) {
                    props.setProperty("user", username);
                }
                if (password != null) {
                    props.setProperty("password", password);
                }

                try {
                    con = DriverManager.getConnection(jdbcUrl, props);
                    if (con != null) {
                        System.err.println("[INFO] Connected successfully using " + scheme + " scheme");
                        break;
                    }
                } catch (SQLException e) {
                    lastError = e;
                    System.err.println("[WARN] Connection failed with " + scheme + " scheme: " + e.getMessage());
                }
            }
        }

        if (con == null) {
            String msg = "Connection failed to GaussDB-M. Last error: " + (lastError != null ? lastError.getMessage() : "null");
            msg += "\n\nPossible solutions:";
            msg += "\n1. Ensure opengauss-jdbc driver is in classpath (recommended)";
            msg += "\n2. Use --connection-url to specify full JDBC URL";
            msg += "\n3. Create an M-compatible database: CREATE DATABASE tm WITH dbcompatibility 'B';";
            msg += "\n4. Use --target-database option to specify your M-compatible database";
            msg += "\n5. Verify host, port, username, password are correct";
            throw new SQLException(msg, lastError);
        }

        // Print connection info
        try {
            java.sql.DatabaseMetaData md = con.getMetaData();
            System.err.println("[INFO] Connected to: " + md.getDatabaseProductName() + " " + md.getDatabaseProductVersion());
            System.err.println("[INFO] JDBC Driver: " + md.getDriverName() + " " + md.getDriverVersion());
        } catch (SQLException e) {
            System.err.println("[WARN] Could not get database metadata: " + e.getMessage());
        }

        String databaseName = globalState.getDatabaseName();

        if (options.useCreateDatabase()) {
            try (Statement s = con.createStatement()) {
                globalState.getState().logStatement("DROP DATABASE IF EXISTS " + databaseName);
                globalState.getState().logStatement("CREATE DATABASE " + databaseName);
                s.execute("DROP DATABASE IF EXISTS " + databaseName);
                s.execute("CREATE DATABASE " + databaseName);
                try {
                    s.execute("USE " + databaseName);
                } catch (SQLException ignored) {
                }
            }
            return new SQLConnection(con);
        }

        try (Statement s = con.createStatement()) {
            globalState.getState().logStatement("DROP SCHEMA IF EXISTS " + databaseName);
            globalState.getState().logStatement("CREATE SCHEMA " + databaseName);
            s.execute("DROP SCHEMA IF EXISTS " + databaseName);
            s.execute("CREATE SCHEMA " + databaseName);
            try {
                s.execute("USE " + databaseName);
            } catch (SQLException ignored) {
                try {
                    s.execute("SET search_path TO " + databaseName);
                } catch (SQLException ignored2) {
                }
            }
        }

        return new SQLConnection(con);
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
        return "gaussdb-m";
    }

    public static void printOracleHelp() {
        System.out.println();
        System.out.println("GaussDB-M --oracle choices: " + java.util.Arrays.toString(GaussDBMOracleFactory.values()));
    }

    @Override
    public boolean addRowsToAllTables(GaussDBMGlobalState globalState) throws Exception {
        for (GaussDBMSchema.GaussDBTable table : globalState.getSchema().getDatabaseTables()) {
            if (table.getNrRows(globalState) == 0) {
                globalState.executeStatement(GaussDBMInsertGenerator.insertRow(globalState, table));
            }
        }
        return true;
    }
}

