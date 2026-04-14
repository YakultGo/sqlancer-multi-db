package sqlancer.gaussdb.gen;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import sqlancer.Randomly;
import sqlancer.common.gen.TLPWhereGenerator;
import sqlancer.common.schema.AbstractTables;
import sqlancer.gaussdb.GaussDBGlobalState;
import sqlancer.gaussdb.GaussDBSchema.GaussDBColumn;
import sqlancer.gaussdb.GaussDBSchema.GaussDBTable;
import sqlancer.gaussdb.ast.GaussDBBetweenOperation;
import sqlancer.gaussdb.ast.GaussDBBinaryComparisonOperation;
import sqlancer.gaussdb.ast.GaussDBBinaryComparisonOperation.BinaryComparisonOperator;
import sqlancer.gaussdb.ast.GaussDBBinaryLogicalOperation;
import sqlancer.gaussdb.ast.GaussDBBinaryLogicalOperation.GaussDBBinaryLogicalOperator;
import sqlancer.gaussdb.ast.GaussDBColumnReference;
import sqlancer.gaussdb.ast.GaussDBConstant;
import sqlancer.gaussdb.ast.GaussDBExpression;
import sqlancer.gaussdb.ast.GaussDBJoin;
import sqlancer.gaussdb.ast.GaussDBSelect;
import sqlancer.gaussdb.ast.GaussDBTableReference;
import sqlancer.gaussdb.ast.GaussDBUnaryPostfixOperation;
import sqlancer.gaussdb.ast.GaussDBUnaryPrefixOperation;
import sqlancer.gaussdb.ast.GaussDBUnaryPrefixOperation.UnaryPrefixOperator;
import sqlancer.gaussdb.ast.GaussDBUnaryPostfixOperation.UnaryPostfixOperator;

public class GaussDBExpressionGenerator implements TLPWhereGenerator<GaussDBSelect, GaussDBJoin, GaussDBExpression, GaussDBTable, GaussDBColumn> {

    private final GaussDBGlobalState state;
    private List<GaussDBTable> tables = new ArrayList<>();
    private AbstractTables<GaussDBTable, GaussDBColumn> targetTables;

    public GaussDBExpressionGenerator(GaussDBGlobalState state) {
        this.state = state;
    }

    private enum Actions {
        COLUMN, LITERAL, BINARY_LOGICAL_OPERATOR, BINARY_COMPARISON_OPERATION, BETWEEN_OPERATOR;
    }

    public GaussDBExpression generateExpression(int depth) {
        if (depth >= state.getOptions().getMaxExpressionDepth()) {
            return generateLeafNode();
        }
        switch (Randomly.fromOptions(Actions.values())) {
        case COLUMN:
            return generateColumn();
        case LITERAL:
            return generateConstant();
        case BINARY_LOGICAL_OPERATOR:
            return new GaussDBBinaryLogicalOperation(generateExpression(depth + 1), generateExpression(depth + 1),
                    GaussDBBinaryLogicalOperator.getRandom());
        case BINARY_COMPARISON_OPERATION:
            return new GaussDBBinaryComparisonOperation(generateExpression(depth + 1), generateExpression(depth + 1),
                    Randomly.fromOptions(BinaryComparisonOperator.values()));
        case BETWEEN_OPERATOR: {
            // Restriction (pdf 4.8.1): x and y must not be expressions like (1 < 1).
            // Enforce leaf bounds to avoid generating invalid BETWEEN syntax.
            GaussDBExpression a = generateExpression(depth + 1);
            GaussDBExpression x = generateLeafNode();
            GaussDBExpression y = generateLeafNode();
            return new GaussDBBetweenOperation(a, x, y, Randomly.getBoolean());
        }
        default:
            throw new AssertionError();
        }
    }

    private GaussDBExpression generateLeafNode() {
        if (Randomly.getBoolean() && !targetTables.getColumns().isEmpty()) {
            return generateColumn();
        }
        return generateConstant();
    }

    private GaussDBExpression generateColumn() {
        GaussDBColumn c = Randomly.fromList(targetTables.getColumns());
        return GaussDBColumnReference.create(c, null);
    }

    public GaussDBConstant generateConstant() {
        return GaussDBConstant.createRandomConstant();
    }

    @Override
    public GaussDBExpression generateBooleanExpression() {
        GaussDBExpression expr = generateExpression(0);
        if (expr instanceof GaussDBBinaryLogicalOperation || expr instanceof GaussDBBinaryComparisonOperation
                || expr instanceof GaussDBBetweenOperation) {
            return expr;
        }
        // Coerce into boolean by comparison
        return new GaussDBBinaryComparisonOperation(expr, generateLeafNode(), BinaryComparisonOperator.EQUALS);
    }

    @Override
    public GaussDBExpression negatePredicate(GaussDBExpression predicate) {
        return new GaussDBUnaryPrefixOperation(predicate, UnaryPrefixOperator.NOT);
    }

    @Override
    public GaussDBExpression isNull(GaussDBExpression expr) {
        return new GaussDBUnaryPostfixOperation(expr, UnaryPostfixOperator.IS_NULL);
    }

    @Override
    public GaussDBSelect generateSelect() {
        GaussDBSelect select = new GaussDBSelect();
        return select;
    }

    @Override
    public List<GaussDBJoin> getRandomJoinClauses() {
        return List.of();
    }

    @Override
    public List<GaussDBExpression> getTableRefs() {
        return tables.stream().map(GaussDBTableReference::create).collect(Collectors.toList());
    }

    @Override
    public List<GaussDBExpression> generateFetchColumns(boolean shouldCreateDummy) {
        // Use first column for stable result sets.
        GaussDBColumn c = Randomly.fromList(targetTables.getColumns());
        return List.of(GaussDBColumnReference.create(c, null));
    }

    @Override
    public List<GaussDBExpression> generateOrderBys() {
        return List.of();
    }

    @Override
    public GaussDBExpressionGenerator setTablesAndColumns(AbstractTables<GaussDBTable, GaussDBColumn> tables) {
        this.targetTables = tables;
        this.tables = tables.getTables();
        return this;
    }
}

