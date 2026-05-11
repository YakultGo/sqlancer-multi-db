package sqlancer.postgres.gen;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import sqlancer.IgnoreMeException;
import sqlancer.Randomly;
import sqlancer.common.query.ExpectedErrors;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.postgres.PostgresGlobalState;
import sqlancer.postgres.PostgresSchema.PostgresColumn;
import sqlancer.postgres.PostgresSchema.PostgresStatisticsObject;
import sqlancer.postgres.PostgresSchema.PostgresTable;

public final class PostgresStatisticsGenerator {

    private static final String STATISTICS_NAME_PREFIX = "sqlancer_s_"
            + Long.toUnsignedString(System.nanoTime(), Character.MAX_RADIX) + "_";
    private static final AtomicLong UNIQUE_STATISTICS_COUNTER = new AtomicLong();

    private PostgresStatisticsGenerator() {
    }

    public static SQLQueryAdapter insert(PostgresGlobalState globalState) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE STATISTICS ");
        if (Randomly.getBoolean()) {
            sb.append(" IF NOT EXISTS");
        }
        PostgresTable randomTable = globalState.getSchema().getRandomTable(t -> !t.isView()); // TODO materialized view
        if (randomTable.getColumns().size() < 2) {
            throw new IgnoreMeException();
        }
        sb.append(" ");
        sb.append(getNewStatisticsName(globalState));
        if (Randomly.getBoolean()) {
            sb.append(" (");
            List<String> statsSubset;
            statsSubset = Randomly.nonEmptySubset("ndistinct", "dependencies", "mcv");
            sb.append(statsSubset.stream().collect(Collectors.joining(", ")));
            sb.append(")");
        }

        List<PostgresColumn> randomColumns = randomTable.getRandomNonEmptyColumnSubset(
                globalState.getRandomly().getInteger(2, randomTable.getColumns().size()));
        sb.append(" ON ");
        sb.append(randomColumns.stream().map(c -> c.getName()).collect(Collectors.joining(", ")));
        sb.append(" FROM ");
        sb.append(randomTable.getName());
        return new SQLQueryAdapter(sb.toString(),
                ExpectedErrors.from("cannot have more than 8 columns in statistics", "already exists"), true);
    }

    public static SQLQueryAdapter remove(PostgresGlobalState globalState) {
        StringBuilder sb = new StringBuilder("DROP STATISTICS ");
        PostgresTable randomTable = globalState.getSchema().getRandomTable();
        List<PostgresStatisticsObject> statistics = randomTable.getStatistics();
        if (statistics.isEmpty()) {
            throw new IgnoreMeException();
        }
        sb.append(Randomly.fromList(statistics).getName());
        return new SQLQueryAdapter(sb.toString(), true);
    }

    public static SQLQueryAdapter alter(PostgresGlobalState globalState) {
        StringBuilder sb = new StringBuilder("ALTER STATISTICS ");
        PostgresTable randomTable = globalState.getSchema().getRandomTable();
        List<PostgresStatisticsObject> statistics = randomTable.getStatistics();
        if (statistics.isEmpty()) {
            throw new IgnoreMeException();
        }
        PostgresStatisticsObject randomStatistic = Randomly.fromList(statistics);
        sb.append(randomStatistic.getName());
        sb.append(" SET STATISTICS ");
        sb.append(Randomly.getNotCachedInteger(-1, 10000)); // -1 means default
        return new SQLQueryAdapter(sb.toString(), true);
    }

    private static String getNewStatisticsName(PostgresGlobalState globalState) {
        while (true) {
            String candidateName = STATISTICS_NAME_PREFIX + UNIQUE_STATISTICS_COUNTER.getAndIncrement();
            if (!statisticsNameExistsInSchema(globalState, candidateName)
                    && !statisticsNameExistsInDatabase(globalState, candidateName)) {
                return candidateName;
            }
        }
    }

    private static boolean statisticsNameExistsInSchema(PostgresGlobalState globalState, String candidateName) {
        return globalState.getSchema().getDatabaseTables().stream().flatMap(t -> t.getStatistics().stream())
                .anyMatch(stat -> stat.getName().contentEquals(candidateName));
    }

    private static boolean statisticsNameExistsInDatabase(PostgresGlobalState globalState, String candidateName) {
        String escapedName = candidateName.replace("'", "''");
        String query = "SELECT 1 FROM pg_statistic_ext stx "
                + "JOIN pg_namespace n ON n.oid = stx.stxnamespace "
                + "WHERE stx.stxname = '" + escapedName + "' "
                + "AND (n.nspname = 'public' OR n.nspname LIKE 'pg_temp_%') LIMIT 1";
        try (Statement s = globalState.getConnection().createStatement(); ResultSet rs = s.executeQuery(query)) {
            return rs.next();
        } catch (SQLException ignored) {
            return false;
        }
    }

}
