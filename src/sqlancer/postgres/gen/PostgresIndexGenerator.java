package sqlancer.postgres.gen;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import sqlancer.Randomly;
import sqlancer.common.DBMSCommon;
import sqlancer.common.query.ExpectedErrors;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.postgres.PostgresCompoundDataType;
import sqlancer.postgres.PostgresGlobalState;
import sqlancer.postgres.PostgresSchema.PostgresColumn;
import sqlancer.postgres.PostgresSchema.PostgresDataType;
import sqlancer.postgres.PostgresSchema.PostgresIndex;
import sqlancer.postgres.PostgresSchema.PostgresTable;
import sqlancer.postgres.PostgresVisitor;
import sqlancer.postgres.ast.PostgresCastOperation;
import sqlancer.postgres.ast.PostgresColumnReference;
import sqlancer.postgres.ast.PostgresExpression;
import sqlancer.postgres.ast.PostgresPostfixOperation;
import sqlancer.postgres.ast.PostgresPostfixOperation.PostfixOperator;
import sqlancer.postgres.ast.PostgresPostfixText;
import sqlancer.postgres.ast.PostgresPrefixOperation;
import sqlancer.postgres.ast.PostgresPrefixOperation.PrefixOperator;

public final class PostgresIndexGenerator {

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
        String constraintName = getNewIndexName(randomTable);
        StringBuilder sb = new StringBuilder();
        sb.append("ALTER TABLE ");
        sb.append(randomTable.getName());
        sb.append(" ADD CONSTRAINT ");
        sb.append(constraintName);
        sb.append(" PRIMARY KEY(");
        sb.append(columns.stream().map(PostgresColumn::getName).collect(Collectors.joining(", ")));
        sb.append(")");
        addPrimaryKeyErrors(errors);
        return new SQLQueryAdapter(sb.toString(), errors);
    }

    private static SQLQueryAdapter generateUniqueIndex(PostgresGlobalState globalState) {
        ExpectedErrors errors = new ExpectedErrors();
        PostgresTable randomTable = globalState.getSchema().getRandomTable(t -> !t.isView());
        int nrColumns = Math.min(randomTable.getColumns().size(), Math.max(1, Randomly.smallNumber() + 1));
        List<IndexElement> elements = createPlainIndexElements(globalState, randomTable, nrColumns, IndexType.BTREE,
                errors);
        addSharedIndexErrors(errors);
        addUniqueErrors(errors);
        return generateCreateIndex(randomTable, true, IndexType.BTREE, elements, globalState, errors);
    }

    private static SQLQueryAdapter generateCompositeIndex(PostgresGlobalState globalState) {
        ExpectedErrors errors = new ExpectedErrors();
        PostgresTable randomTable = globalState.getSchema().getRandomTable(t -> !t.isView());
        IndexType method = Randomly.fromOptions(IndexType.BTREE, IndexType.GIST, IndexType.GIN, IndexType.BRIN);
        int nrColumns = Math.min(randomTable.getColumns().size(), Math.max(2, Randomly.smallNumber() + 2));
        List<IndexElement> elements = createPlainIndexElements(globalState, randomTable, nrColumns, method, errors);
        addSharedIndexErrors(errors);
        return generateCreateIndex(randomTable, false, method, elements, globalState, errors);
    }

    private static SQLQueryAdapter generatePrefixExpressionIndex(PostgresGlobalState globalState) {
        ExpectedErrors errors = new ExpectedErrors();
        PostgresTable randomTable = globalState.getSchema().getRandomTable(t -> !t.isView());
        IndexType method = chooseExpressionIndexType();
        List<IndexElement> elements = createExpressionPositionIndexElements(globalState, randomTable,
                PostgresIndexModel.PREFIX_EXPR, method, errors, true);
        addSharedIndexErrors(errors);
        return generateCreateIndex(randomTable, false, method, elements, globalState, errors);
    }

    private static SQLQueryAdapter generateSuffixExpressionIndex(PostgresGlobalState globalState) {
        ExpectedErrors errors = new ExpectedErrors();
        PostgresTable randomTable = globalState.getSchema().getRandomTable(t -> !t.isView());
        IndexType method = chooseExpressionIndexType();
        List<IndexElement> elements = createExpressionPositionIndexElements(globalState, randomTable,
                PostgresIndexModel.SUFFIX_EXPR, method, errors, false);
        addSharedIndexErrors(errors);
        return generateCreateIndex(randomTable, false, method, elements, globalState, errors);
    }

    private static SQLQueryAdapter generateExpressionIndex(PostgresGlobalState globalState) {
        ExpectedErrors errors = new ExpectedErrors();
        PostgresTable randomTable = globalState.getSchema().getRandomTable(t -> !t.isView());
        IndexType method = chooseExpressionIndexType();
        int nrElements = Math.min(randomTable.getColumns().size(), Math.max(1, Randomly.smallNumber() + 1));
        List<PostgresColumn> columns = getOrderedColumns(randomTable, nrElements, false, false);
        List<IndexElement> elements = new ArrayList<>();
        for (PostgresColumn column : columns) {
            elements.add(createExpressionIndexElement(globalState, randomTable, column, PostgresIndexModel.EXPRESSION,
                    method, errors));
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
        sb.append(getNewIndexName(randomTable));
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
        if (globalState != null && Randomly.getBoolean()) {
            sb.append(" WHERE ");
            PostgresExpression expr = new PostgresExpressionGenerator(globalState).setColumns(randomTable.getColumns())
                    .setGlobalState(globalState).generateExpression(PostgresDataType.BOOLEAN);
            sb.append(PostgresVisitor.asString(expr));
        }
        return new SQLQueryAdapter(sb.toString(), errors);
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

    private static List<IndexElement> createExpressionPositionIndexElements(PostgresGlobalState globalState,
            PostgresTable randomTable, PostgresIndexModel model, IndexType method, ExpectedErrors errors,
            boolean expressionFirst) {
        int nrColumns = Math.min(randomTable.getColumns().size(), Math.max(1, Randomly.smallNumber() + 1));
        List<PostgresColumn> columns = getOrderedColumns(randomTable, nrColumns, false, false);
        PostgresColumn expressionColumn = Randomly.fromList(columns);
        List<IndexElement> elements = new ArrayList<>();
        IndexElement expression = createExpressionIndexElement(globalState, randomTable, expressionColumn, model, method,
                errors);
        if (expressionFirst) {
            elements.add(expression);
        }
        for (PostgresColumn column : columns) {
            if (column != expressionColumn) {
                elements.add(createColumnIndexElement(globalState, column, method, errors));
            }
        }
        if (!expressionFirst) {
            elements.add(expression);
        }
        return elements;
    }

    private static IndexElement createColumnIndexElement(PostgresGlobalState globalState, PostgresColumn column,
            IndexType method, ExpectedErrors errors) {
        StringBuilder sb = new StringBuilder();
        sb.append(column.getName());
        appendIndexElementOptions(globalState, sb, method, errors);
        return new IndexElement(sb.toString());
    }

    private static IndexElement createExpressionIndexElement(PostgresGlobalState globalState, PostgresTable randomTable,
            PostgresColumn column, PostgresIndexModel model, IndexType method, ExpectedErrors errors) {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        sb.append(getExpressionSql(globalState, randomTable, column, model));
        sb.append(")");
        appendIndexElementOptions(globalState, sb, method, errors);
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

    private static String getExpressionSql(PostgresGlobalState globalState, PostgresTable randomTable,
            PostgresColumn column, PostgresIndexModel model) {
        switch (model) {
        case PREFIX_EXPR:
            return PostgresVisitor.asString(createPrefixExpression(column));
        case SUFFIX_EXPR:
            return PostgresVisitor.asString(createSuffixExpression(column));
        case EXPRESSION:
            return PostgresVisitor.asString(
                    PostgresExpressionGenerator.generateExpression(globalState, randomTable.getColumns()));
        default:
            return PostgresVisitor.asString(
                    PostgresExpressionGenerator.generateExpression(globalState, randomTable.getColumns()));
        }
    }

    private static PostgresExpression createPrefixExpression(PostgresColumn column) {
        PostgresExpression columnReference = new PostgresColumnReference(column);
        switch (column.getType()) {
        case BOOLEAN:
            return new PostgresPrefixOperation(columnReference, PrefixOperator.NOT);
        case INT:
            return new PostgresPrefixOperation(columnReference,
                    Randomly.fromOptions(PrefixOperator.UNARY_PLUS, PrefixOperator.UNARY_MINUS));
        default:
            return new PostgresCastOperation(columnReference, PostgresCompoundDataType.create(PostgresDataType.TEXT));
        }
    }

    private static PostgresExpression createSuffixExpression(PostgresColumn column) {
        PostgresExpression columnReference = new PostgresColumnReference(column);
        if (Randomly.getBoolean()) {
            return new PostgresPostfixOperation(columnReference,
                    Randomly.fromOptions(PostfixOperator.IS_NULL, PostfixOperator.IS_NOT_NULL));
        }
        return new PostgresPostfixText(columnReference, "::text", null, PostgresDataType.TEXT);
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

    private static IndexType chooseExpressionIndexType() {
        return Randomly.fromOptions(IndexType.BTREE, IndexType.GIST, IndexType.SPGIST);
    }

    private static boolean canUseInclude(IndexType method) {
        return method != IndexType.HASH;
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

    private static String getNewIndexName(PostgresTable randomTable) {
        List<PostgresIndex> indexes = randomTable.getIndexes();
        int indexI = 0;
        while (true) {
            String indexName = DBMSCommon.createIndexName(indexI++);
            if (indexes.stream().noneMatch(i -> i.getIndexName().equals(indexName))) {
                return indexName;
            }
        }
    }
}
