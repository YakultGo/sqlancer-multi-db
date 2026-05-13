package sqlancer.postgres.gen;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import sqlancer.Randomly;
import sqlancer.common.DBMSCommon;
import sqlancer.common.query.ExpectedErrors;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.postgres.PostgresGlobalState;
import sqlancer.postgres.PostgresCompoundDataType;
import sqlancer.postgres.PostgresSchema;
import sqlancer.postgres.PostgresSchema.PostgresColumn;
import sqlancer.postgres.PostgresSchema.PostgresDataType;
import sqlancer.postgres.PostgresSchema.PostgresTable;
import sqlancer.postgres.PostgresVisitor;
import sqlancer.postgres.ast.PostgresExpression;

public class PostgresTableGenerator {

    private final String tableName;
    private boolean columnCanHavePrimaryKey;
    private boolean columnHasPrimaryKey;
    private final StringBuilder sb = new StringBuilder();
    private boolean isTemporaryTable;
    private boolean isUnloggedTable;
    private boolean isPartitionedTable;
    private final PostgresSchema newSchema;
    private final List<PostgresColumn> columnsToBeAdded = new ArrayList<>();
    protected final ExpectedErrors errors = new ExpectedErrors();
    private final PostgresTable table;
    private final PostgresGlobalState globalState;

    public PostgresTableGenerator(String tableName, PostgresSchema newSchema, PostgresGlobalState globalState) {
        this.newSchema = newSchema;
        this.globalState = globalState;
        if (Randomly.getBoolean()) {
            isTemporaryTable = true;
            this.tableName = createTemporaryTableName(tableName, newSchema);
        } else if (Randomly.getBoolean()) {
            isUnloggedTable = true;
            this.tableName = tableName;
        } else {
            this.tableName = tableName;
        }
        table = new PostgresTable(this.tableName, columnsToBeAdded, null, null, null, false, false);
        errors.add("invalid input syntax for");
        errors.add("is not unique");
        errors.add("integer out of range");
        errors.add("division by zero");
        errors.add("cannot create partitioned table as inheritance child");
        errors.add("cannot cast");
        errors.add("ERROR: functions in index expression must be marked IMMUTABLE");
        errors.add("functions in partition key expression must be marked IMMUTABLE");
        errors.add("functions in index predicate must be marked IMMUTABLE");
        errors.add("has no default operator class for access method");
        errors.add("does not exist for access method");
        errors.add("does not accept data type");
        errors.add("but default expression is of type text");
        errors.add("has pseudo-type unknown");
        errors.add("no collation was derived for partition key column");
        errors.add("inherits from generated column but specifies identity");
        errors.add("inherits from generated column but specifies default");
        // Some PostgreSQL installations return localized messages (e.g., Chinese) for this error.
        // Match a stable substring across locales.
        errors.add("NULL/NOT NULL");
        PostgresCommon.addCommonExpressionErrors(errors);
        PostgresCommon.addCommonTableErrors(errors);
    }

    public static SQLQueryAdapter generate(String tableName, PostgresSchema newSchema, PostgresGlobalState globalState) {
        return new PostgresTableGenerator(tableName, newSchema, globalState).generate();
    }

    protected SQLQueryAdapter generate() {
        columnCanHavePrimaryKey = true;
        sb.append("CREATE");
        if (isTemporaryTable) {
            sb.append(" ");
            sb.append(Randomly.fromOptions("TEMPORARY", "TEMP"));
        } else if (isUnloggedTable) {
            sb.append(" UNLOGGED");
        }
        sb.append(" TABLE");
        if (Randomly.getBoolean()) {
            sb.append(" IF NOT EXISTS");
        }
        sb.append(" ");
        sb.append(tableName);
        if (Randomly.getBoolean() && !newSchema.getDatabaseTables().isEmpty()) {
            createLike();
        } else {
            createStandard();
        }
        return new SQLQueryAdapter(sb.toString(), errors, true);
    }

    private static String createTemporaryTableName(String baseName, PostgresSchema schema) {
        int suffix = 0;
        while (true) {
            String candidate = suffix == 0 ? baseName + "_temp" : baseName + "_temp" + (suffix - 1);
            if (schema.getDatabaseTables().stream().noneMatch(table -> table.getName().equals(candidate))) {
                return candidate;
            }
            suffix++;
        }
    }

    private void createStandard() throws AssertionError {
        sb.append("(");
        isPartitionedTable = shouldGeneratePartitionedTable();
        int columnCount = Math.max(1, globalState.getDbmsSpecificOptions().getPgTableColumns());
        for (int i = 0; i < columnCount; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            String name = DBMSCommon.createColumnName(i);
            createColumn(name);
        }
        if (shouldGenerateTableConstraints()) {
            errors.add("constraints on temporary tables may reference only temporary tables");
            errors.add("constraints on unlogged tables may reference only permanent or unlogged tables");
            errors.add("constraints on permanent tables may reference only permanent tables");
            errors.add("cannot be implemented");
            errors.add("there is no unique constraint matching given keys for referenced table");
            errors.add("cannot reference partitioned table");
            errors.add("unsupported ON COMMIT and foreign key combination");
            errors.add("ERROR: invalid ON DELETE action for foreign key constraint containing generated column");
            errors.add("exclusion constraints are not supported on partitioned tables");
            PostgresCommon.addTableConstraints(columnHasPrimaryKey, sb, table, globalState, errors);
        }
        sb.append(")");
        generateInherits();
        generatePartitionBy();
        generateUsing();
        if (!isPartitionedTable && !isTemporaryTable) {
            PostgresCommon.generateWith(sb, globalState, errors);
        }
        if (Randomly.getBoolean() && isTemporaryTable) {
            sb.append(" ON COMMIT ");
            sb.append(Randomly.fromOptions("PRESERVE ROWS", "DELETE ROWS", "DROP"));
            sb.append(" ");
        }
    }

    private void createLike() {
        sb.append("(");
        sb.append("LIKE ");
        sb.append(newSchema.getRandomTable().getName());
        if (Randomly.getBoolean()) {
            for (int i = 0; i < Randomly.smallNumber(); i++) {
                String option = Randomly.fromOptions("DEFAULTS", "CONSTRAINTS", "INDEXES", "STORAGE", "COMMENTS",
                        "GENERATED", "IDENTITY", "STATISTICS", "STORAGE", "ALL");
                sb.append(" ");
                sb.append(Randomly.fromOptions("INCLUDING", "EXCLUDING"));
                sb.append(" ");
                sb.append(option);
            }
        }
        sb.append(")");
    }

    private void createColumn(String name) throws AssertionError {
        sb.append(name);
        sb.append(" ");
        PostgresCompoundDataType type = getRandomColumnType();
        boolean serial = PostgresCommon.appendDataType(type, sb, true, globalState.getCollates());
        PostgresColumn c = new PostgresColumn(name, type);
        c.setTable(table);
        columnsToBeAdded.add(c);
        sb.append(" ");
        if (Randomly.getBoolean() && !type.isArray()) {
            createColumnConstraint(type.getDataType(), serial);
        }
    }

    private PostgresCompoundDataType getRandomColumnType() {
        if (Randomly.getBooleanWithSmallProbability()) {
            return PostgresExpressionGenerator.getRandomArrayType(Randomly.getBoolean() ? 1 : 2);
        }
        return PostgresExpressionGenerator.getCompoundDataType(PostgresDataType.getRandomType());
    }

    private void generatePartitionBy() {
        if (!isPartitionedTable) {
            return;
        }
        sb.append(" PARTITION BY ");
        String partitionOption = getPartitionOption();
        sb.append(partitionOption);
        sb.append("(");
        errors.add("unrecognized parameter");
        errors.add("cannot use constant expression");
        errors.add("cannot add NO INHERIT constraint to partitioned table");
        errors.add("unrecognized parameter");
        errors.add("unsupported PRIMARY KEY constraint with partition key definition");
        errors.add("which is part of the partition key.");
        errors.add("unsupported UNIQUE constraint with partition key definition");
        errors.add("does not accept data type");
        PostgresCommon.addCommonExpressionErrors(errors);
        List<PostgresColumn> simplePartitionColumns = getSimplePartitionColumns(partitionOption);
        if (!simplePartitionColumns.isEmpty() && !Randomly.getBooleanWithRatherLowProbability()) {
            sb.append(Randomly.fromList(simplePartitionColumns).getName());
            sb.append(")");
            return;
        }
        int n = partitionOption.contentEquals("LIST") ? 1 : Randomly.smallNumber() + 1;
        for (int i = 0; i < n; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append("(");
            PostgresExpression expr = PostgresExpressionGenerator.generateExpression(globalState, columnsToBeAdded);
            sb.append(PostgresVisitor.asString(expr));
            sb.append(")");
            if (Randomly.getBoolean()) {
                sb.append(globalState.getRandomOpclass());
                errors.add("does not exist for access method");
            }
        }
        sb.append(")");
    }

    private boolean shouldGeneratePartitionedTable() {
        // PostgreSQL rejects UNLOGGED partitioned tables and temporary partitioned tables.
        if (isUnloggedTable || isTemporaryTable) {
            return false;
        }
        return Randomly.getBoolean();
    }

    private boolean shouldGenerateTableConstraints() {
        if (!isPartitionedTable) {
            return Randomly.getBoolean();
        }
        return Randomly.getBooleanWithRatherLowProbability();
    }

    private String getPartitionOption() {
        List<String> optionsWithSimpleColumns = new ArrayList<>();
        for (String option : List.of("RANGE", "LIST", "HASH")) {
            if (!getSimplePartitionColumns(option).isEmpty()) {
                optionsWithSimpleColumns.add(option);
            }
        }
        if (!optionsWithSimpleColumns.isEmpty() && !Randomly.getBooleanWithRatherLowProbability()) {
            return Randomly.fromList(optionsWithSimpleColumns);
        }
        return Randomly.fromOptions("RANGE", "LIST", "HASH");
    }

    private List<PostgresColumn> getSimplePartitionColumns(String partitionOption) {
        List<PostgresColumn> supportedColumns = new ArrayList<>();
        for (PostgresColumn column : columnsToBeAdded) {
            if (column.getCompoundType().isArray()) {
                continue;
            }
            PostgresDataType dataType = column.getCompoundType().getDataType();
            switch (partitionOption) {
            case "RANGE":
                if (dataType == PostgresDataType.INT || dataType == PostgresDataType.DATE
                        || dataType == PostgresDataType.TIMESTAMP || dataType == PostgresDataType.TIMESTAMPTZ) {
                    supportedColumns.add(column);
                }
                break;
            case "LIST":
                if (dataType == PostgresDataType.INT || dataType == PostgresDataType.TEXT
                        || dataType == PostgresDataType.VARCHAR || dataType == PostgresDataType.CHAR
                        || dataType == PostgresDataType.ENUM || dataType == PostgresDataType.BOOLEAN) {
                    supportedColumns.add(column);
                }
                break;
            case "HASH":
                supportedColumns.add(column);
                break;
            default:
                throw new AssertionError(partitionOption);
            }
        }
        return supportedColumns;
    }

    private void generateUsing() {
        /*
         * Postgres does not allow specifying USING clause for partitioned tables since they don't have any storage
         * associated with them
         */
        if (isPartitionedTable) {
            return;
        }
        if (Randomly.getBoolean()) {
            return;
        }
        sb.append(" USING ");
        sb.append(globalState.getRandomTableAccessMethod());
    }

    private void generateInherits() {
        if (isPartitionedTable) {
            return;
        }
        if (Randomly.getBoolean() && !newSchema.getDatabaseTablesWithoutViews().isEmpty()) {
            sb.append(" INHERITS(");
            sb.append(newSchema.getDatabaseTablesRandomSubsetNotEmpty().stream().map(t -> t.getName())
                    .collect(Collectors.joining(", ")));
            sb.append(")");
            errors.add("has a type conflict");
            errors.add("has a generation conflict");
            errors.add("cannot create partitioned table as inheritance child");
            errors.add("cannot inherit from temporary relation");
            errors.add("cannot inherit from partitioned table");
            errors.add("has a collation conflict");
            errors.add("inherits conflicting default values");
            errors.add("specifies generation expression");
        }
    }

    private enum ColumnConstraint {
        NULL_OR_NOT_NULL, UNIQUE, PRIMARY_KEY, DEFAULT, CHECK, GENERATED
    };

    private void createColumnConstraint(PostgresDataType type, boolean serial) {
        List<ColumnConstraint> constraintSubset = Randomly.nonEmptySubset(ColumnConstraint.values());
        if (Randomly.getBoolean()) {
            // make checks constraints less likely
            constraintSubset.remove(ColumnConstraint.CHECK);
        }
        if (!columnCanHavePrimaryKey || columnHasPrimaryKey) {
            constraintSubset.remove(ColumnConstraint.PRIMARY_KEY);
        }
        if (isPartitionedTable) {
            constraintSubset.remove(ColumnConstraint.PRIMARY_KEY);
            constraintSubset.remove(ColumnConstraint.UNIQUE);
        }
        if (constraintSubset.contains(ColumnConstraint.GENERATED)
                && constraintSubset.contains(ColumnConstraint.DEFAULT)) {
            // otherwise: ERROR: both default and identity specified for column
            constraintSubset.remove(Randomly.fromOptions(ColumnConstraint.GENERATED, ColumnConstraint.DEFAULT));
        }
        if (constraintSubset.contains(ColumnConstraint.GENERATED) && type != PostgresDataType.INT) {
            // otherwise: ERROR: identity column type must be smallint, integer, or bigint
            constraintSubset.remove(ColumnConstraint.GENERATED);
        }
        if (serial) {
            constraintSubset.remove(ColumnConstraint.GENERATED);
            constraintSubset.remove(ColumnConstraint.DEFAULT);
            constraintSubset.remove(ColumnConstraint.NULL_OR_NOT_NULL);

        }
        for (ColumnConstraint c : constraintSubset) {
            sb.append(" ");
            switch (c) {
            case NULL_OR_NOT_NULL:
                sb.append(Randomly.fromOptions("NOT NULL", "NULL"));
                errors.add("conflicting NULL/NOT NULL declarations");
                break;
            case UNIQUE:
                sb.append("UNIQUE");
                break;
            case PRIMARY_KEY:
                sb.append("PRIMARY KEY");
                columnHasPrimaryKey = true;
                break;
            case DEFAULT:
                sb.append("DEFAULT");
                sb.append(" (");
                sb.append(PostgresVisitor.asString(PostgresExpressionGenerator.generateExpression(globalState, type)));
                sb.append(")");
                // CREATE TEMPORARY TABLE t1(c0 smallint DEFAULT ('566963878'));
                errors.add("out of range");
                errors.add("is a generated column");
                break;
            case CHECK:
                sb.append("CHECK (");
                sb.append(PostgresVisitor.asString(PostgresExpressionGenerator.generateExpression(globalState,
                        columnsToBeAdded, PostgresDataType.BOOLEAN)));
                sb.append(")");
                if (Randomly.getBoolean()) {
                    sb.append(" NO INHERIT");
                }
                errors.add("out of range");
                break;
            case GENERATED:
                sb.append("GENERATED ");
                if (Randomly.getBoolean()) {
                    sb.append(" ALWAYS AS (");
                    sb.append(PostgresVisitor.asString(
                            PostgresExpressionGenerator.generateExpression(globalState, columnsToBeAdded, type)));
                    sb.append(") STORED");
                    errors.add("A generated column cannot reference another generated column.");
                    errors.add("cannot use generated column in partition key");
                    errors.add("generation expression is not immutable");
                    errors.add("cannot use column reference in DEFAULT expression");
                } else {
                    sb.append(Randomly.fromOptions("ALWAYS", "BY DEFAULT"));
                    sb.append(" AS IDENTITY");
                }
                break;
            default:
                throw new AssertionError(sb);
            }
        }
    }

}
