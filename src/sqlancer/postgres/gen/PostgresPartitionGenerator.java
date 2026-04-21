package sqlancer.postgres.gen;

import java.util.List;
import java.util.stream.Collectors;

import sqlancer.IgnoreMeException;
import sqlancer.Randomly;
import sqlancer.common.query.ExpectedErrors;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.postgres.PostgresGlobalState;
import sqlancer.postgres.PostgresSchema.PostgresColumn;
import sqlancer.postgres.PostgresSchema.PostgresDataType;
import sqlancer.postgres.PostgresSchema.PostgresTable;
import sqlancer.postgres.PostgresSchema.PostgresTable.PartitionStrategy;

public final class PostgresPartitionGenerator {

    private static final int HASH_MODULUS = 8;
    private static final String DEFAULT_PARTITION_SUFFIX = "_default";

    private PostgresPartitionGenerator() {
    }

    public static SQLQueryAdapter createPartition(PostgresGlobalState globalState) {
        List<PostgresTable> candidates = globalState.getSchema().getDatabaseTables().stream()
                .filter(PostgresPartitionGenerator::canCreatePartitionFor).collect(Collectors.toList());
        if (candidates.isEmpty()) {
            throw new IgnoreMeException();
        }
        PostgresTable parent = Randomly.fromList(candidates);
        int partitionIndex = getNextPartitionIndex(globalState, parent);
        if (parent.getPartitionStrategy() == PartitionStrategy.HASH && partitionIndex >= HASH_MODULUS) {
            throw new IgnoreMeException();
        }
        boolean createDefaultPartition = shouldCreateDefaultPartition(globalState, parent);

        ExpectedErrors errors = createPartitionErrors();
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ");
        if (createDefaultPartition) {
            sb.append(createDefaultPartitionName(parent));
        } else {
            sb.append(createPartitionName(globalState, parent, partitionIndex));
        }
        sb.append(" PARTITION OF ");
        sb.append(parent.getName());
        sb.append(" ");
        if (createDefaultPartition) {
            sb.append("DEFAULT");
        } else {
            appendPartitionBound(sb, parent, getPartitionKeyColumn(parent), partitionIndex);
        }
        return new SQLQueryAdapter(sb.toString(), errors, true);
    }

    public static SQLQueryAdapter detachPartition(PostgresGlobalState globalState) {
        List<PostgresTable> candidates = globalState.getSchema().getDatabaseTables().stream()
                .filter(t -> t.isPartition() && t.getPartitionParent() != null).collect(Collectors.toList());
        if (candidates.isEmpty()) {
            throw new IgnoreMeException();
        }
        PostgresTable child = Randomly.fromList(candidates);
        ExpectedErrors errors = new ExpectedErrors();
        errors.add("is not a partition of relation");
        errors.add("relation does not exist");
        errors.add("cannot detach partitions concurrently");
        StringBuilder sb = new StringBuilder();
        sb.append("ALTER TABLE ");
        sb.append(child.getPartitionParent());
        sb.append(" DETACH PARTITION ");
        sb.append(child.getName());
        if (Randomly.getBooleanWithRatherLowProbability()) {
            sb.append(" FINALIZE");
        }
        return new SQLQueryAdapter(sb.toString(), errors, true);
    }

    private static boolean canCreatePartitionFor(PostgresTable parent) {
        if (!parent.isPartitioned() || !parent.hasSimplePartitionKey()) {
            return false;
        }
        PostgresColumn keyColumn = getPartitionKeyColumn(parent);
        if (keyColumn == null || keyColumn.getCompoundType().isArray()) {
            return false;
        }
        switch (parent.getPartitionStrategy()) {
        case RANGE:
            return isRangePartitionType(keyColumn.getCompoundType().getDataType());
        case LIST:
            return isListPartitionType(keyColumn.getCompoundType().getDataType());
        case HASH:
            return true;
        case NONE:
        default:
            return false;
        }
    }

    public static boolean hasCreatePartitionCandidate(PostgresGlobalState globalState) {
        return globalState.getSchema().getDatabaseTables().stream().anyMatch(PostgresPartitionGenerator::canCreatePartitionFor);
    }

    private static boolean isRangePartitionType(PostgresDataType dataType) {
        return dataType == PostgresDataType.INT || dataType == PostgresDataType.DATE
                || dataType == PostgresDataType.TIMESTAMP || dataType == PostgresDataType.TIMESTAMPTZ;
    }

    private static boolean isListPartitionType(PostgresDataType dataType) {
        return dataType == PostgresDataType.INT || dataType == PostgresDataType.TEXT
                || dataType == PostgresDataType.ENUM || dataType == PostgresDataType.BOOLEAN;
    }

    private static PostgresColumn getPartitionKeyColumn(PostgresTable parent) {
        if (!parent.hasSimplePartitionKey()) {
            return null;
        }
        String keyColumnName = parent.getPartitionKeyColumns().get(0);
        for (PostgresColumn column : parent.getColumns()) {
            if (column.getName().equals(keyColumnName)) {
                return column;
            }
        }
        return null;
    }

    private static int getNextPartitionIndex(PostgresGlobalState globalState, PostgresTable parent) {
        String prefix = parent.getName() + "_p";
        int next = 0;
        for (PostgresTable table : globalState.getSchema().getDatabaseTables()) {
            if (!table.getName().startsWith(prefix)) {
                continue;
            }
            String suffix = table.getName().substring(prefix.length());
            try {
                next = Math.max(next, Integer.parseInt(suffix) + 1);
            } catch (NumberFormatException ignored) {
                next++;
            }
        }
        return next;
    }

    static String createPartitionName(PostgresGlobalState globalState, PostgresTable parent, int partitionIndex) {
        String prefix = parent.getName() + "_p";
        int candidateIndex = partitionIndex;
        while (true) {
            String candidate = prefix + candidateIndex;
            boolean exists = globalState.getSchema().getDatabaseTables().stream()
                    .anyMatch(table -> table.getName().equals(candidate));
            if (!exists) {
                return candidate;
            }
            candidateIndex++;
        }
    }

    private static boolean shouldCreateDefaultPartition(PostgresGlobalState globalState, PostgresTable parent) {
        if (parent.getPartitionStrategy() == PartitionStrategy.HASH || Randomly.getBooleanWithRatherLowProbability()) {
            return false;
        }
        String defaultPartitionName = createDefaultPartitionName(parent);
        return globalState.getSchema().getDatabaseTables().stream()
                .noneMatch(table -> table.getName().equals(defaultPartitionName));
    }

    private static String createDefaultPartitionName(PostgresTable parent) {
        return parent.getName() + DEFAULT_PARTITION_SUFFIX;
    }

    static void appendPartitionBound(StringBuilder sb, PostgresTable parent, PostgresColumn keyColumn,
            int partitionIndex) {
        switch (parent.getPartitionStrategy()) {
        case RANGE:
            sb.append("FOR VALUES FROM (");
            sb.append(rangeLiteral(keyColumn.getCompoundType().getDataType(), partitionIndex));
            sb.append(") TO (");
            sb.append(rangeLiteral(keyColumn.getCompoundType().getDataType(), partitionIndex + 1));
            sb.append(")");
            break;
        case LIST:
            sb.append("FOR VALUES IN (");
            sb.append(listLiteral(keyColumn.getCompoundType().getDataType(), partitionIndex));
            sb.append(")");
            break;
        case HASH:
            sb.append("FOR VALUES WITH (MODULUS ");
            sb.append(HASH_MODULUS);
            sb.append(", REMAINDER ");
            sb.append(partitionIndex);
            sb.append(")");
            break;
        case NONE:
        default:
            throw new IgnoreMeException();
        }
    }

    private static String rangeLiteral(PostgresDataType dataType, int partitionIndex) {
        int value = partitionIndex * 100;
        switch (dataType) {
        case INT:
            return String.valueOf(value);
        case DATE:
            return "'2000-01-01'::date + " + value;
        case TIMESTAMP:
            return "'2000-01-01 00:00:00'::timestamp + interval '" + value + " days'";
        case TIMESTAMPTZ:
            return "'2000-01-01 00:00:00+00'::timestamptz + interval '" + value + " days'";
        default:
            throw new IgnoreMeException();
        }
    }

    private static String listLiteral(PostgresDataType dataType, int partitionIndex) {
        switch (dataType) {
        case INT:
            return String.valueOf(partitionIndex);
        case TEXT:
            return "'" + escapeSql("sqlancer_partition_" + partitionIndex) + "'";
        case ENUM:
            String[] labels = { "a", "b", "c", "d" };
            if (partitionIndex >= labels.length) {
                throw new IgnoreMeException();
            }
            return "'" + escapeSql(labels[partitionIndex]) + "'";
        case BOOLEAN:
            if (partitionIndex > 1) {
                throw new IgnoreMeException();
            }
            return partitionIndex == 0 ? "false" : "true";
        default:
            throw new IgnoreMeException();
        }
    }

    private static String escapeSql(String text) {
        return text.replace("'", "''");
    }

    private static ExpectedErrors createPartitionErrors() {
        ExpectedErrors errors = new ExpectedErrors();
        errors.add("would overlap partition");
        errors.add("cannot create partition");
        errors.add("contains a whole-row variable");
        errors.add("cannot use column reference in partition bound expression");
        errors.add("specified more than once");
        errors.add("remainder for hash partition must be less than modulus");
        errors.add("partition would overlap");
        errors.add("relation already exists");
        errors.add("a hash-partitioned table may not have a default partition");
        errors.add("no partition of relation");
        errors.add("invalid input syntax");
        return errors;
    }
}
