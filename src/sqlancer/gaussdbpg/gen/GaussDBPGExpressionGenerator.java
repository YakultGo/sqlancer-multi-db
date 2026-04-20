package sqlancer.gaussdbpg.gen;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import sqlancer.Randomly;
import sqlancer.common.gen.ExpressionGenerator;
import sqlancer.common.gen.NoRECGenerator;
import sqlancer.common.gen.TLPWhereGenerator;
import sqlancer.common.schema.AbstractTables;
import sqlancer.gaussdbpg.GaussDBPGGlobalState;
import sqlancer.gaussdbpg.GaussDBPGSchema.GaussDBPGColumn;
import sqlancer.gaussdbpg.GaussDBPGSchema.GaussDBPGRowValue;
import sqlancer.gaussdbpg.GaussDBPGSchema.GaussDBPGTable;
import sqlancer.gaussdbpg.GaussDBPGToStringVisitor;
import sqlancer.gaussdbpg.ast.GaussDBPGColumnValue;
import sqlancer.gaussdbpg.ast.GaussDBPGDataType;
import sqlancer.gaussdbpg.ast.GaussDBPGBinaryComparisonOperation;
import sqlancer.gaussdbpg.ast.GaussDBPGBinaryComparisonOperation.GaussDBPGBinaryComparisonOperator;
import sqlancer.gaussdbpg.ast.GaussDBPGBinaryLogicalOperation;
import sqlancer.gaussdbpg.ast.GaussDBPGBinaryLogicalOperation.GaussDBPGBinaryLogicalOperator;
import sqlancer.gaussdbpg.ast.GaussDBPGBetweenOperation;
import sqlancer.gaussdbpg.ast.GaussDBPGCastOperation;
import sqlancer.gaussdbpg.ast.GaussDBPGColumnReference;
import sqlancer.gaussdbpg.ast.GaussDBPGAggregate;
import sqlancer.gaussdbpg.ast.GaussDBPGAggregate.GaussDBPGAggregateFunction;
import sqlancer.gaussdbpg.ast.GaussDBPGConstant;
import sqlancer.gaussdbpg.ast.GaussDBPGExpression;
import sqlancer.gaussdbpg.ast.GaussDBPGInOperation;
import sqlancer.gaussdbpg.ast.GaussDBPGJoin;
import sqlancer.gaussdbpg.ast.GaussDBPGJoin.GaussDBPGJoinType;
import sqlancer.gaussdbpg.ast.GaussDBPGSelect;
import sqlancer.gaussdbpg.ast.GaussDBPGTableReference;
import sqlancer.gaussdbpg.ast.GaussDBPGUnaryPostfixOperation;
import sqlancer.gaussdbpg.ast.GaussDBPGUnaryPostfixOperation.UnaryPostfixOperator;
import sqlancer.gaussdbpg.ast.GaussDBPGUnaryPrefixOperation;
import sqlancer.gaussdbpg.ast.GaussDBPGUnaryPrefixOperation.UnaryPrefixOperator;

public class GaussDBPGExpressionGenerator
        implements ExpressionGenerator<GaussDBPGExpression>,
        TLPWhereGenerator<GaussDBPGSelect, GaussDBPGJoin, GaussDBPGExpression, GaussDBPGTable, GaussDBPGColumn>,
        NoRECGenerator<GaussDBPGSelect, GaussDBPGJoin, GaussDBPGExpression, GaussDBPGTable, GaussDBPGColumn> {

    private final GaussDBPGGlobalState state;
    private List<GaussDBPGTable> tables = new ArrayList<>();
    private AbstractTables<GaussDBPGTable, GaussDBPGColumn> targetTables;
    private List<GaussDBPGColumn> columns;
    private GaussDBPGRowValue rowValue;

    public GaussDBPGExpressionGenerator(GaussDBPGGlobalState state) {
        this.state = state;
    }

    public GaussDBPGExpressionGenerator setColumns(List<GaussDBPGColumn> columns) {
        this.columns = columns;
        return this;
    }

    public GaussDBPGExpressionGenerator setRowValue(GaussDBPGRowValue rowValue) {
        this.rowValue = rowValue;
        return this;
    }

    public GaussDBPGExpression generateExpressionWithExpectedResult(GaussDBPGDataType type) {
        GaussDBPGExpression expr = generateExpression(0);
        // The expected value is computed by the expression's getExpectedValue() method
        return expr;
    }

    // Implement ExpressionGenerator interface
    public GaussDBPGExpression generatePredicate() {
        return generateBooleanExpression();
    }

    private enum Action {
        COLUMN, LITERAL, BINARY_LOGICAL_OPERATOR, BINARY_COMPARISON_OPERATION, BETWEEN_OPERATOR, IN_OPERATOR, UNARY_OPERATOR
    }

    public GaussDBPGExpression generateExpression(int depth) {
        if (depth >= state.getOptions().getMaxExpressionDepth()) {
            return generateLeafNode();
        }
        switch (Randomly.fromOptions(Action.values())) {
        case COLUMN:
            return generateColumn();
        case LITERAL:
            return generateConstant();
        case BINARY_LOGICAL_OPERATOR:
            return new GaussDBPGBinaryLogicalOperation(generateExpression(depth + 1), generateExpression(depth + 1),
                    GaussDBPGBinaryLogicalOperator.getRandom());
        case BINARY_COMPARISON_OPERATION:
            return new GaussDBPGBinaryComparisonOperation(generateExpression(depth + 1), generateExpression(depth + 1),
                    GaussDBPGBinaryComparisonOperator.getRandom());
        case BETWEEN_OPERATOR:
            GaussDBPGExpression a = generateExpression(depth + 1);
            GaussDBPGExpression x = generateLeafNode();
            GaussDBPGExpression y = generateLeafNode();
            return new GaussDBPGBetweenOperation(a, x, y, Randomly.getBoolean());
        case IN_OPERATOR:
            return GaussDBPGInOperation.create(generateLeafNode(), Randomly.smallNumber() + 1);
        case UNARY_OPERATOR:
            return new GaussDBPGUnaryPrefixOperation(generateExpression(depth + 1),
                    Randomly.getBoolean() ? UnaryPrefixOperator.NOT : Randomly.fromOptions(UnaryPrefixOperator.UNARY_PLUS, UnaryPrefixOperator.UNARY_MINUS));
        default:
            throw new AssertionError();
        }
    }

    private GaussDBPGExpression generateLeafNode() {
        List<GaussDBPGColumn> availableColumns = columns != null ? columns
                : (targetTables != null ? targetTables.getColumns() : null);
        if (Randomly.getBoolean() && availableColumns != null && !availableColumns.isEmpty()) {
            return generateColumn();
        }
        return generateConstant();
    }

    private GaussDBPGExpression generateColumn() {
        List<GaussDBPGColumn> availableColumns = columns != null ? columns
                : (targetTables != null ? targetTables.getColumns() : null);
        if (availableColumns == null || availableColumns.isEmpty()) {
            return generateConstant();
        }
        GaussDBPGColumn c = Randomly.fromList(availableColumns);
        if (rowValue != null && rowValue.getValues().containsKey(c)) {
            return GaussDBPGColumnValue.create(c, rowValue.getValues().get(c));
        }
        return GaussDBPGColumnReference.create(c, null);
    }

    public GaussDBPGConstant generateConstant() {
        return GaussDBPGConstant.createRandomConstant(state.getRandomly());
    }

    public GaussDBPGConstant generateConstant(GaussDBPGDataType type) {
        switch (type) {
        case INT:
            return GaussDBPGConstant.createIntConstant(state.getRandomly().getInteger());
        case BOOLEAN:
            return GaussDBPGConstant.createBooleanConstant(Randomly.getBoolean());
        case TEXT:
            return GaussDBPGConstant.createTextConstant(state.getRandomly().getString());
        case DECIMAL:
            return GaussDBPGConstant.createDecimalConstant(state.getRandomly().getRandomBigDecimal());
        case FLOAT:
            return GaussDBPGConstant.createFloatConstant(state.getRandomly().getDouble());
        default:
            return GaussDBPGConstant.createRandomConstant(state.getRandomly());
        }
    }

    @Override
    public GaussDBPGExpression generateBooleanExpression() {
        GaussDBPGExpression expr = generateExpression(0);
        if (expr instanceof GaussDBPGBinaryLogicalOperation || expr instanceof GaussDBPGBinaryComparisonOperation
                || expr instanceof GaussDBPGBetweenOperation || expr instanceof GaussDBPGInOperation
                || expr instanceof GaussDBPGUnaryPostfixOperation) {
            return expr;
        }
        // Coerce into boolean by comparison
        return new GaussDBPGBinaryComparisonOperation(expr, generateLeafNode(),
                GaussDBPGBinaryComparisonOperator.EQUALS);
    }

    @Override
    public GaussDBPGExpression negatePredicate(GaussDBPGExpression predicate) {
        return new GaussDBPGUnaryPrefixOperation(predicate, UnaryPrefixOperator.NOT);
    }

    @Override
    public GaussDBPGExpression isNull(GaussDBPGExpression expr) {
        return new GaussDBPGUnaryPostfixOperation(expr, UnaryPostfixOperator.IS_NULL);
    }

    @Override
    public GaussDBPGSelect generateSelect() {
        GaussDBPGSelect select = new GaussDBPGSelect();
        return select;
    }

    @Override
    public List<GaussDBPGJoin> getRandomJoinClauses() {
        List<GaussDBPGJoin> joinStatements = new ArrayList<>();
        for (int i = 1; i < tables.size(); i++) {
            GaussDBPGExpression joinClause = generateExpression(0);
            GaussDBPGTable table = Randomly.fromList(tables);
            tables.remove(table);
            GaussDBPGJoinType joinType = GaussDBPGJoinType.getRandom();
            GaussDBPGJoin j = new GaussDBPGJoin(GaussDBPGTableReference.create(table), joinClause, joinType);
            joinStatements.add(j);
        }
        return joinStatements;
    }

    @Override
    public List<GaussDBPGExpression> getTableRefs() {
        return tables.stream().map(GaussDBPGTableReference::create).collect(Collectors.toList());
    }

    @Override
    public List<GaussDBPGExpression> generateFetchColumns(boolean shouldCreateDummy) {
        if (shouldCreateDummy && Randomly.getBooleanWithSmallProbability()) {
            return List.of(GaussDBPGColumnReference.create(GaussDBPGColumn.createDummy("*"), null));
        }
        List<GaussDBPGExpression> fetchColumns = new ArrayList<>();
        List<GaussDBPGColumn> targetColumns = Randomly.nonEmptySubset(columns);
        for (GaussDBPGColumn c : targetColumns) {
            fetchColumns.add(new GaussDBPGColumnReference(c, null));
        }
        return fetchColumns;
    }

    @Override
    public List<GaussDBPGExpression> generateOrderBys() {
        return List.of();
    }

    @Override
    public GaussDBPGExpressionGenerator setTablesAndColumns(AbstractTables<GaussDBPGTable, GaussDBPGColumn> tables) {
        this.targetTables = tables;
        this.tables = tables.getTables();
        this.columns = tables.getColumns();
        return this;
    }

    // ==================== NoRECGenerator interface ====================

    @Override
    public String generateOptimizedQueryString(GaussDBPGSelect select, GaussDBPGExpression whereCondition,
            boolean shouldUseAggregate) {
        GaussDBPGColumnReference allColumns = GaussDBPGColumnReference.create(GaussDBPGColumn.createDummy("*"), null);
        if (shouldUseAggregate) {
            select.setFetchColumns(
                    List.of(new GaussDBPGAggregate(List.of(allColumns), GaussDBPGAggregateFunction.COUNT)));
        } else {
            select.setFetchColumns(List.of(allColumns));
        }
        select.setWhereClause(whereCondition);
        select.setSelectType(GaussDBPGSelect.GaussDBPGSelectType.ALL);
        return GaussDBPGToStringVisitor.asString(select);
    }

    @Override
    public String generateUnoptimizedQueryString(GaussDBPGSelect select, GaussDBPGExpression whereCondition) {
        // PG-style: CAST(condition AS INTEGER) gives 1 for true, 0 for false
        GaussDBPGCastOperation castToInt = new GaussDBPGCastOperation(whereCondition, GaussDBPGDataType.INT);
        select.setFetchColumns(List.of(castToInt));
        select.setWhereClause(null);
        select.setOrderByClauses(List.of());
        select.setSelectType(GaussDBPGSelect.GaussDBPGSelectType.ALL);
        return "SELECT SUM(count) FROM (" + GaussDBPGToStringVisitor.asString(select) + ") as res";
    }
}