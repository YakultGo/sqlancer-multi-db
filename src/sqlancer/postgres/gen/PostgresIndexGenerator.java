package sqlancer.postgres.gen;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import sqlancer.IgnoreMeException;
import sqlancer.Randomly;
import sqlancer.common.DBMSCommon;
import sqlancer.common.query.ExpectedErrors;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.postgres.PostgresGlobalState;
import sqlancer.postgres.PostgresSchema.PostgresColumn;
import sqlancer.postgres.PostgresSchema.PostgresDataType;
import sqlancer.postgres.PostgresSchema.PostgresTable;
import sqlancer.postgres.PostgresVisitor;
import sqlancer.postgres.ast.PostgresExpression;

public final class PostgresIndexGenerator {

    private static final AtomicLong UNIQUE_INDEX_COUNTER = new AtomicLong();

    private PostgresIndexGenerator() {
    }

    public enum IndexType {
        BTREE, HASH, GIST, GIN, SPGIST, BRIN
    }

    public enum PostgresIndexModel {
        DEFAULT(0),
        UNIQUE(1),
        PRIMARY_KEY(2),
        COMPOSITE(3),
        PREFIX_EXPR(4),
        SUFFIX_EXPR(5),
        EXPRESSION(6);

        private final int optionValue;

        PostgresIndexModel(int optionValue) {
            this.optionValue = optionValue;
        }

        public static PostgresIndexModel fromOption(int optionValue) {
            for (PostgresIndexModel model : values()) {
                if (model.optionValue == optionValue) {
                    return model;
                }
            }
            throw new AssertionError(optionValue);
        }

        public static PostgresIndexModel pickRandomNonDefault() {
            return Randomly.fromOptions(UNIQUE, PRIMARY_KEY, COMPOSITE, PREFIX_EXPR, SUFFIX_EXPR, EXPRESSION);
        }
    }

    private static final class IndexElement {
        private final String sql;

        private IndexElement(String sql) {
            this.sql = sql;
        }
    }

    public static SQLQueryAdapter generate(PostgresGlobalState globalState) {
        PostgresIndexModel configuredModel = PostgresIndexModel
                .fromOption(globalState.getDbmsSpecificOptions().getPgIndexModel());
        PostgresIndexModel effectiveModel = configuredModel == PostgresIndexModel.DEFAULT
                ? PostgresIndexModel.pickRandomNonDefault()
                : configuredModel;
        switch (effectiveModel) {
        case PRIMARY_KEY:
            return generatePrimaryKey(globalState);
        case UNIQUE:
            return generateUniqueIndex(globalState);
        case COMPOSITE:
            return generateCompositeIndex(globalState);
        case PREFIX_EXPR:
            return generatePrefixExpressionIndex(globalState);
        case SUFFIX_EXPR:
            return generateSuffixExpressionIndex(globalState);
        case EXPRESSION:
            return generateExpressionIndex(globalState);
        default:
            throw new AssertionError(effectiveModel);
        }
    }

    private static SQLQueryAdapter generatePrimaryKey(PostgresGlobalState globalState) {
        ExpectedErrors errors = new ExpectedErrors();
        PostgresTable randomTable = globalState.getSchema().getRandomTable(t -> !t.isView());
        List<PostgresColumn> columns = getOrderedColumns(randomTable, Math.min(randomTable.getColumns().size(),
                Math.max(1, Randomly.smallNumber() + 1)), false, false);
        String constraintName = getNewIndexName(globalState);
        StringBuilder sb = new StringBuilder();
        sb.append("ALTER TABLE ");
        sb.append(randomTable.getName());
        sb.append(" ADD CONSTRAINT ");
        sb.append(constraintName);
        sb.append(" PRIMARY KEY(");
        sb.append(columns.stream().map(PostgresColumn::getName).collect(Collectors.joining(", ")));
        sb.append(")");
        appendOptionalConstraintTiming(sb);
        addPrimaryKeyErrors(errors);
        return new SQLQueryAdapter(sb.toString(), errors, true);
    }

    private static SQLQueryAdapter generateUniqueIndex(PostgresGlobalState globalState) {
        ExpectedErrors errors = new ExpectedErrors();
        PostgresTable randomTable = globalState.getSchema().getRandomTable(t -> !t.isView());
        List<IndexElement> elements;
        if (Randomly.getBooleanWithRatherLowProbability()) {
            elements = List.of(createExpressionIndexElement(globalState, randomTable, IndexType.BTREE, errors));
        } else {
            int nrColumns = Math.min(randomTable.getColumns().size(), Math.max(1, Randomly.smallNumber() + 1));
            elements = createPlainIndexElements(globalState, randomTable, nrColumns, IndexType.BTREE, errors);
        }
        addSharedIndexErrors(errors);
        addUniqueErrors(errors);
        return generateCreateIndex(randomTable, true, IndexType.BTREE, elements, globalState, errors);
    }

    private static SQLQueryAdapter generateCompositeIndex(PostgresGlobalState globalState) {
        ExpectedErrors errors = new ExpectedErrors();
        PostgresTable randomTable = globalState.getSchema()
                .getRandomTableOrBailout(t -> !t.isView() && t.getColumns().size() >= 2);
        IndexType method = Randomly.fromOptions(IndexType.BTREE, IndexType.GIST, IndexType.GIN, IndexType.BRIN);
        int nrColumns = Math.min(randomTable.getColumns().size(), Math.max(2, Randomly.smallNumber() + 2));
        List<IndexElement> elements = createPlainIndexElements(globalState, randomTable, nrColumns, method, errors);
        addSharedIndexErrors(errors);
        return generateCreateIndex(randomTable, false, method, elements, globalState, errors);
    }

    private static SQLQueryAdapter generatePrefixExpressionIndex(PostgresGlobalState globalState) {
        ExpectedErrors errors = new ExpectedErrors();
        PostgresTable randomTable = getRandomTableWithTextColumn(globalState);
        IndexType method = IndexType.BTREE;
        List<IndexElement> elements = createSubstringPositionIndexElements(globalState, randomTable, method, errors,
                true, true);
        addSharedIndexErrors(errors);
        return generateCreateIndex(randomTable, false, method, elements, globalState, errors);
    }

    private static SQLQueryAdapter generateSuffixExpressionIndex(PostgresGlobalState globalState) {
        ExpectedErrors errors = new ExpectedErrors();
        PostgresTable randomTable = getRandomTableWithTextColumn(globalState);
        IndexType method = IndexType.BTREE;
        List<IndexElement> elements = createSubstringPositionIndexElements(globalState, randomTable, method, errors,
                false, false);
        addSharedIndexErrors(errors);
        return generateCreateIndex(randomTable, false, method, elements, globalState, errors);
    }

    private static SQLQueryAdapter generateExpressionIndex(PostgresGlobalState globalState) {
        ExpectedErrors errors = new ExpectedErrors();
        PostgresTable randomTable = globalState.getSchema().getRandomTable(t -> !t.isView());
        int nrElements = Math.min(randomTable.getColumns().size(), Math.max(1, Randomly.smallNumber() + 1));
        IndexType method = chooseExpressionIndexType(nrElements);
        List<IndexElement> elements = new ArrayList<>();
        for (int i = 0; i < nrElements; i++) {
            elements.add(createExpressionIndexElement(globalState, randomTable, method, errors));
        }
        addSharedIndexErrors(errors);
        return generateCreateIndex(randomTable, false, method, elements, globalState, errors);
    }

    private static SQLQueryAdapter generateCreateIndex(PostgresTable randomTable, boolean unique, IndexType method,
            List<IndexElement> elements, PostgresGlobalState globalState, ExpectedErrors errors) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE");
        if (unique) {
            sb.append(" UNIQUE");
        }
        sb.append(" INDEX ");
        if (Randomly.getBooleanWithRatherLowProbability()) {
            sb.append("IF NOT EXISTS ");
        }
        sb.append(getNewIndexName(globalState));
        sb.append(" ON ");
        if (Randomly.getBoolean()) {
            sb.append("ONLY ");
        }
        sb.append(randomTable.getName());
        if (method != IndexType.BTREE || Randomly.getBoolean()) {
            sb.append(" USING ");
            sb.append(method);
        }
        sb.append("(");
        sb.append(elements.stream().map(e -> e.sql).collect(Collectors.joining(", ")));
        sb.append(")");
        if (canUseInclude(method) && Randomly.getBoolean()) {
            sb.append(" INCLUDE(");
            List<PostgresColumn> columns = randomTable.getRandomNonEmptyColumnSubset();
            sb.append(columns.stream().map(PostgresColumn::getName).collect(Collectors.joining(", ")));
            sb.append(")");
        }
        appendOptionalUniqueNullTreatment(sb, unique, errors);
        appendIndexStorageParameters(globalState, sb, method);
        if (globalState != null && Randomly.getBoolean()) {
            sb.append(" WHERE ");
            PostgresExpression expr = new PostgresExpressionGenerator(globalState).setColumns(randomTable.getColumns())
                    .setGlobalState(globalState).generateExpression(PostgresDataType.BOOLEAN);
            sb.append(PostgresVisitor.asString(expr));
        }
        return new SQLQueryAdapter(sb.toString(), errors, true);
    }

    private static void appendOptionalConstraintTiming(StringBuilder sb) {
        if (Randomly.getBooleanWithRatherLowProbability()) {
            boolean deferrable = Randomly.getBoolean();
            sb.append(deferrable ? " DEFERRABLE" : " NOT DEFERRABLE");
            if (deferrable && Randomly.getBoolean()) {
                sb.append(" INITIALLY ");
                sb.append(Randomly.fromOptions("IMMEDIATE", "DEFERRED"));
            }
        }
    }

    private static void appendOptionalUniqueNullTreatment(StringBuilder sb, boolean unique, ExpectedErrors errors) {
        if (!unique || !Randomly.getBooleanWithRatherLowProbability()) {
            return;
        }
        sb.append(" NULLS ");
        if (Randomly.getBoolean()) {
            sb.append("NOT ");
        }
        sb.append("DISTINCT");
        errors.add("syntax error at or near \"NULLS\"");
    }

    private static List<IndexElement> createPlainIndexElements(PostgresGlobalState globalState,
            PostgresTable randomTable, int nrColumns, IndexType method, ExpectedErrors errors) {
        List<PostgresColumn> orderedColumns = getOrderedColumns(randomTable, nrColumns, false, false);
        List<IndexElement> elements = new ArrayList<>();
        for (PostgresColumn column : orderedColumns) {
            elements.add(createColumnIndexElement(globalState, column, method, errors));
        }
        return elements;
    }

    private static List<IndexElement> createSubstringPositionIndexElements(PostgresGlobalState globalState,
            PostgresTable randomTable, IndexType method, ExpectedErrors errors, boolean expressionFirst,
            boolean usePrefix) {
        int minElements = randomTable.getColumns().size() >= 2 ? 2 : 1;
        int nrColumns = Math.min(randomTable.getColumns().size(), Math.max(minElements, Randomly.smallNumber() + 2));
        List<PostgresColumn> columns = getOrderedColumns(randomTable, nrColumns, false, false);
        PostgresColumn substringColumn = getRandomTextColumn(randomTable.getColumns());
        if (!columns.contains(substringColumn)) {
            if (columns.size() >= nrColumns) {
                columns.remove(Randomly.fromList(columns));
            }
            columns.add(substringColumn);
        }
        List<IndexElement> elements = new ArrayList<>();
        IndexElement substringElement = createSubstringIndexElement(globalState, substringColumn, method, errors,
                usePrefix);
        if (expressionFirst) {
            elements.add(substringElement);
        }
        for (PostgresColumn column : columns) {
            if (column != substringColumn) {
                elements.add(createColumnIndexElement(globalState, column, method, errors));
            }
        }
        if (!expressionFirst) {
            elements.add(substringElement);
        }
        return elements;
    }

    private static IndexElement createColumnIndexElement(PostgresGlobalState globalState, PostgresColumn column,
            IndexType method, ExpectedErrors errors) {
        StringBuilder sb = new StringBuilder();
        sb.append(column.getName());
        appendOptionalCollation(globalState, sb, column);
        appendIndexElementOptions(globalState, sb, method, errors);
        return new IndexElement(sb.toString());
    }

    private static IndexElement createExpressionIndexElement(PostgresGlobalState globalState, PostgresTable randomTable,
            IndexType method, ExpectedErrors errors) {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        sb.append(PostgresVisitor.asString(new PostgresExpressionGenerator(globalState)
                .setColumns(randomTable.getColumns()).generateExpression(0)));
        sb.append(")");
        appendIndexElementOptions(globalState, sb, method, errors);
        return new IndexElement(sb.toString());
    }

    private static IndexElement createSubstringIndexElement(PostgresGlobalState globalState, PostgresColumn column,
            IndexType method, ExpectedErrors errors, boolean usePrefix) {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        sb.append(usePrefix ? "left(" : "right(");
        sb.append(column.getName());
        sb.append(", ");
        sb.append(globalState.getRandomly().getInteger(1, 101));
        sb.append(")");
        appendOptionalCollation(globalState, sb, column);
        sb.append(")");
        appendIndexElementOrderingOptions(sb, method);
        return new IndexElement(sb.toString());
    }

    private static void appendIndexElementOptions(PostgresGlobalState globalState, StringBuilder sb, IndexType method,
            ExpectedErrors errors) {
        if (Randomly.getBooleanWithRatherLowProbability()) {
            sb.append(" ");
            sb.append(globalState.getRandomOpclass());
            errors.add("does not accept");
            errors.add("does not exist for access method");
        }
        if (method == IndexType.BTREE || method == IndexType.GIST || method == IndexType.BRIN) {
            appendIndexElementOrderingOptions(sb, method);
        }
    }

    private static void appendIndexElementOrderingOptions(StringBuilder sb, IndexType method) {
        if (method == IndexType.BTREE || method == IndexType.GIST || method == IndexType.BRIN) {
            if (Randomly.getBoolean()) {
                sb.append(" ");
                sb.append(Randomly.fromOptions("ASC", "DESC"));
            }
            if (Randomly.getBooleanWithRatherLowProbability()) {
                sb.append(" NULLS ");
                sb.append(Randomly.fromOptions("FIRST", "LAST"));
            }
        }
    }

    private static void appendOptionalCollation(PostgresGlobalState globalState, StringBuilder sb,
            PostgresColumn column) {
        if (isTextType(column) && !globalState.getCollates().isEmpty()
                && Randomly.getBooleanWithRatherLowProbability()) {
            sb.append(" COLLATE ");
            appendQuotedIdentifier(sb, globalState.getRandomCollate());
        }
    }

    private static void appendIndexStorageParameters(PostgresGlobalState globalState, StringBuilder sb,
            IndexType method) {
        if (!Randomly.getBooleanWithRatherLowProbability()) {
            return;
        }
        sb.append(" WITH (");
        switch (method) {
        case BRIN:
            if (Randomly.getBoolean()) {
                sb.append("pages_per_range = ");
                sb.append(globalState.getRandomly().getInteger(1, 257));
            } else {
                sb.append("autosummarize = ");
                sb.append(Randomly.fromOptions("on", "off"));
            }
            break;
        case BTREE:
            if (Randomly.getBooleanWithRatherLowProbability()) {
                sb.append("deduplicate_items = ");
                sb.append(Randomly.fromOptions("on", "off"));
            } else {
                appendFillfactor(globalState, sb);
            }
            break;
        case HASH:
        case SPGIST:
            appendFillfactor(globalState, sb);
            break;
        case GIST:
            if (Randomly.getBoolean()) {
                appendFillfactor(globalState, sb);
            } else {
                sb.append("buffering = ");
                sb.append(Randomly.fromOptions("on", "off", "auto"));
            }
            break;
        case GIN:
            if (Randomly.getBoolean()) {
                sb.append("fastupdate = ");
                sb.append(Randomly.fromOptions("on", "off"));
            } else {
                sb.append("gin_pending_list_limit = ");
                sb.append(globalState.getRandomly().getInteger(64, 4097));
            }
            break;
        default:
            throw new AssertionError(method);
        }
        sb.append(")");
    }

    private static void appendFillfactor(PostgresGlobalState globalState, StringBuilder sb) {
        sb.append("fillfactor = ");
        sb.append(globalState.getRandomly().getInteger(10, 101));
    }

    private static void appendQuotedIdentifier(StringBuilder sb, String identifier) {
        sb.append('"');
        sb.append(identifier.replace("\"", "\"\""));
        sb.append('"');
    }

    private static List<PostgresColumn> getOrderedColumns(PostgresTable randomTable, int targetCount,
            boolean preferPrefixOrder, boolean preferSuffixOrder) {
        int nrColumns = Math.max(1, Math.min(targetCount, randomTable.getColumns().size()));
        List<PostgresColumn> columns = Randomly.nonEmptySubset(randomTable.getColumns(), nrColumns);
        if (columns.size() >= 2 && (preferPrefixOrder || preferSuffixOrder)) {
            PostgresColumn emphasized = Randomly.fromList(columns);
            columns.remove(emphasized);
            if (preferPrefixOrder) {
                columns.add(0, emphasized);
            } else {
                columns.add(emphasized);
            }
        }
        return columns;
    }

    private static IndexType chooseExpressionIndexType(int nrElements) {
        if (nrElements == 1) {
            return Randomly.fromOptions(IndexType.BTREE, IndexType.HASH, IndexType.GIST, IndexType.SPGIST);
        }
        return Randomly.fromOptions(IndexType.BTREE, IndexType.GIST, IndexType.SPGIST);
    }

    private static PostgresTable getRandomTableWithTextColumn(PostgresGlobalState globalState) {
        PostgresTable table = globalState.getSchema()
                .getRandomTableOrBailout(t -> !t.isView() && t.getColumns().stream().anyMatch(c -> isTextType(c)));
        if (table.getColumns().stream().noneMatch(c -> isTextType(c))) {
            throw new IgnoreMeException();
        }
        return table;
    }

    private static PostgresColumn getRandomTextColumn(List<PostgresColumn> columns) {
        List<PostgresColumn> textColumns = columns.stream().filter(PostgresIndexGenerator::isTextType)
                .collect(Collectors.toList());
        if (textColumns.isEmpty()) {
            throw new IgnoreMeException();
        }
        return Randomly.fromList(textColumns);
    }

    private static boolean isTextType(PostgresColumn column) {
        return column.getType() == PostgresDataType.TEXT || column.getType() == PostgresDataType.VARCHAR
                || column.getType() == PostgresDataType.CHAR;
    }

    private static boolean canUseInclude(IndexType method) {
        return method == IndexType.BTREE || method == IndexType.GIST || method == IndexType.SPGIST;
    }

    private static void addSharedIndexErrors(ExpectedErrors errors) {
        errors.add("already contains data");
        errors.add("You might need to add explicit type casts");
        errors.add(" collations are not supported");
        errors.add("because it has pending trigger events");
        errors.add("could not determine which collation to use for index expression");
        errors.add("could not determine which collation to use for string comparison");
        errors.add("is duplicated");
        errors.add("already exists");
        errors.add("has no default operator class");
        errors.add("does not support");
        errors.add("does not support multicolumn indexes");
        errors.add("does not support included columns");
        errors.add("cannot cast");
        errors.add("invalid input syntax for");
        errors.add("must be type ");
        errors.add("integer out of range");
        errors.add("division by zero");
        errors.add("out of range");
        errors.add("functions in index predicate must be marked IMMUTABLE");
        errors.add("functions in index expression must be marked IMMUTABLE");
        errors.add("result of range difference would not be contiguous");
        errors.add("which is part of the partition key");
        PostgresCommon.addCommonExpressionErrors(errors);
    }

    private static void addUniqueErrors(ExpectedErrors errors) {
        errors.add("access method \"gin\" does not support unique indexes");
        errors.add("access method \"hash\" does not support unique indexes");
        errors.add("could not create unique index");
        errors.add("unsupported UNIQUE constraint with partition key definition");
        errors.add("insufficient columns in UNIQUE constraint definition");
    }

    private static void addPrimaryKeyErrors(ExpectedErrors errors) {
        addSharedIndexErrors(errors);
        errors.add("multiple primary keys for table");
        errors.add("primary key constraints are not supported on partitioned tables");
        errors.add("could not create unique index");
        errors.add("contains null values");
    }

    private static String getNewIndexName(PostgresGlobalState globalState) {
        while (true) {
            String indexName = DBMSCommon.createIndexName((int) UNIQUE_INDEX_COUNTER.getAndIncrement());
            if (globalState.getSchema().getDatabaseTables().stream().flatMap(t -> t.getIndexes().stream())
                    .noneMatch(i -> i.getIndexName().equals(indexName))) {
                return indexName;
            }
        }
    }
}
