package sqlancer.gaussdbm;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import com.google.auto.service.AutoService;

import sqlancer.MainOptions;
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

    public GaussDBMProvider() {
        super(GaussDBMGlobalState.class, GaussDBMOptions.class);
    }

    @Override
    public void generateDatabase(GaussDBMGlobalState globalState) throws Exception {
        while (globalState.getSchema().getDatabaseTables().size() < (int) sqlancer.Randomly.getNotCachedInteger(1, 2)) {
            String tableName = DBMSCommon.createTableName(globalState.getSchema().getDatabaseTables().size());
            SQLQueryAdapter createTable = GaussDBMTableGenerator.generate(globalState, tableName);
            globalState.executeStatement(createTable);
        }
        int inserts = (int) sqlancer.Randomly.getNotCachedInteger(1, globalState.getOptions().getMaxNumberInserts());
        for (int i = 0; i < inserts; i++) {
            globalState.executeStatement(GaussDBMInsertGenerator.insertRow(globalState));
        }
    }

    @Override
    public SQLConnection createDatabase(GaussDBMGlobalState globalState) throws SQLException {
        MainOptions options = globalState.getOptions();
        loadDriverIfRequested(options);

        String username = options.getUserName();
        String password = options.getPassword();

        String jdbcUrl = options.getConnectionURL();
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            throw new AssertionError(
                    "GaussDB-M requires --connection-url. Example: --connection-url=jdbc:gaussdb://127.0.0.1:8000/test");
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

