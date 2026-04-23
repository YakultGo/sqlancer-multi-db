package sqlancer.postgres.gen;

import java.util.stream.Collectors;

import sqlancer.Randomly;
import sqlancer.common.query.ExpectedErrors;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.postgres.PostgresGlobalState;
import sqlancer.postgres.PostgresSchema.PostgresTable;

public final class PostgresTruncateGenerator {

    private PostgresTruncateGenerator() {
    }

    public static SQLQueryAdapter create(PostgresGlobalState globalState) {
        StringBuilder sb = new StringBuilder();
        sb.append("TRUNCATE");
        if (Randomly.getBoolean()) {
            sb.append(" TABLE");
        }
        boolean truncateOnly = Randomly.getBooleanWithRatherLowProbability()
                && globalState.getSchema().getDatabaseTables().stream().anyMatch(t -> t.isPartitioned());
        if (truncateOnly) {
            sb.append(" ONLY");
        }
        sb.append(" ");
        if (truncateOnly) {
            PostgresTable table = globalState.getSchema().getRandomTable(t -> t.isPartitioned());
            sb.append(table.getName());
        } else {
            sb.append(globalState.getSchema().getDatabaseTablesRandomSubsetNotEmpty().stream().map(t -> t.getName())
                    .collect(Collectors.joining(", ")));
        }
        if (Randomly.getBoolean()) {
            sb.append(" ");
            sb.append(Randomly.fromOptions("RESTART IDENTITY", "CONTINUE IDENTITY"));
        }
        if (Randomly.getBoolean()) {
            sb.append(" ");
            sb.append(Randomly.fromOptions("CASCADE", "RESTRICT"));
        }
        return new SQLQueryAdapter(sb.toString(),
                ExpectedErrors.from("cannot truncate a table referenced in a foreign key constraint", "is not a table",
                        "is not distributed", "cannot truncate only a partitioned table"));
    }

}
