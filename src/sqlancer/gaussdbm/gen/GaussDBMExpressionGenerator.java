package sqlancer.gaussdbm.gen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import sqlancer.Randomly;
import sqlancer.common.gen.CERTGenerator;
import sqlancer.common.gen.NoRECGenerator;
import sqlancer.common.gen.TLPWhereGenerator;
import sqlancer.common.gen.UntypedExpressionGenerator;
import sqlancer.common.schema.AbstractTables;
import sqlancer.gaussdbm.GaussDBMGlobalState;
import sqlancer.gaussdbm.GaussDBMSchema.GaussDBColumn;
import sqlancer.gaussdbm.GaussDBMSchema.GaussDBRowValue;
import sqlancer.gaussdbm.GaussDBMSchema.GaussDBTable;
import sqlancer.gaussdbm.ast.GaussDBAggregate;
import sqlancer.gaussdbm.ast.GaussDBAggregate.GaussDBAggregateFunction;
import sqlancer.gaussdbm.ast.GaussDBBetweenOperation;
import sqlancer.gaussdbm.ast.GaussDBBinaryComparisonOperation;
import sqlancer.gaussdbm.ast.GaussDBBinaryComparisonOperation.BinaryComparisonOperator;
import sqlancer.gaussdbm.ast.GaussDBBinaryLogicalOperation;
import sqlancer.gaussdbm.ast.GaussDBBinaryLogicalOperation.GaussDBBinaryLogicalOperator;
import sqlancer.gaussdbm.ast.GaussDBColumnReference;
import sqlancer.gaussdbm.ast.GaussDBConstant;
import sqlancer.gaussdbm.ast.GaussDBExpression;
import sqlancer.gaussdbm.ast.GaussDBIfFunction;
import sqlancer.gaussdbm.ast.GaussDBJoin;
import sqlancer.gaussdbm.ast.GaussDBSelect;
import sqlancer.gaussdbm.ast.GaussDBSelect.SelectType;
import sqlancer.gaussdbm.ast.GaussDBTableReference;
import sqlancer.gaussdbm.ast.GaussDBUnaryPostfixOperation;
import sqlancer.gaussdbm.ast.GaussDBUnaryPostfixOperation.UnaryPostfixOperator;
import sqlancer.gaussdbm.ast.GaussDBUnaryPrefixOperation;
import sqlancer.gaussdbm.ast.GaussDBUnaryPrefixOperation.UnaryPrefixOperator;

public class GaussDBMExpressionGenerator extends UntypedExpressionGenerator<GaussDBExpression, GaussDBColumn>
        implements NoRECGenerator<GaussDBSelect, GaussDBJoin, GaussDBExpression, GaussDBTable, GaussDBColumn>,
        TLPWhereGenerator<GaussDBSelect, GaussDBJoin, GaussDBExpression, GaussDBTable, GaussDBColumn>,
        CERTGenerator<GaussDBSelect, GaussDBJoin, GaussDBExpression, GaussDBTable, GaussDBColumn> {

    private final GaussDBMGlobalState state;
    private List<GaussDBTable> tables = new ArrayList<>();
    private GaussDBRowValue rowVal;

    public GaussDBMExpressionGenerator(GaussDBMGlobalState state) {
        this.state = state;
    }

    public GaussDBMExpressionGenerator setRowVal(GaussDBRowValue rowVal) {
        this.rowVal = rowVal;
        return this;
    }

    private enum Actions {
        COLUMN, LITERAL, BINARY_LOGICAL_OPERATOR, BINARY_COMPARISON_OPERATION, BETWEEN_OPERATOR, UNARY_PREFIX_OPERATION,
        UNARY_POSTFIX_OPERATION;
    }

    @Override
    protected GaussDBExpression generateExpression(int depth) {
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
            GaussDBExpression a = generateExpression(depth + 1);
            GaussDBExpression x = generateLeafNode();
            GaussDBExpression y = generateLeafNode();
            return new GaussDBBetweenOperation(a, x, y, Randomly.getBoolean());
        }
        case UNARY_PREFIX_OPERATION:
            return new GaussDBUnaryPrefixOperation(generateExpression(depth + 1), UnaryPrefixOperator.NOT);
        case UNARY_POSTFIX_OPERATION:
            return new GaussDBUnaryPostfixOperation(generateExpression(depth + 1), UnaryPostfixOperator.IS_NULL);
        default:
            throw new AssertionError();
        }
    }

    @Override
    protected GaussDBExpression generateColumn() {
        GaussDBColumn c = Randomly.fromList(columns);
        GaussDBConstant val = rowVal == null ? null : rowVal.getValues().get(c);
        return GaussDBColumnReference.create(c, val);
    }

    @Override
    public GaussDBConstant generateConstant() {
        return GaussDBConstant.createRandomConstant();
    }

    @Override
    public GaussDBExpression generateBooleanExpression() {
        return generateExpression();
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
        return new GaussDBSelect();
    }

    @Override
    public List<GaussDBJoin> getRandomJoinClauses() {
        if (tables == null || tables.size() <= 1) {
            return List.of();
        }
        List<GaussDBTable> tablesCopy = new ArrayList<>(tables);
        List<GaussDBJoin> joins = GaussDBJoin.getRandomJoinClauses(tablesCopy, state);
        this.tables = tablesCopy;
        return joins;
    }

    @Override
    public List<GaussDBExpression> getTableRefs() {
        return tables.stream().map(GaussDBTableReference::create).collect(Collectors.toList());
    }

    @Override
    public List<GaussDBExpression> generateFetchColumns(boolean shouldCreateDummy) {
        return columns.stream().map(c -> GaussDBColumnReference.create(c, null)).collect(Collectors.toList());
    }

    @Override
    public String generateOptimizedQueryString(GaussDBSelect select, GaussDBExpression whereCondition,
            boolean shouldUseAggregate) {
        if (shouldUseAggregate) {
            GaussDBAggregate countAgg = new GaussDBAggregate(List.of(GaussDBConstant.createIntConstant(1)),
                    GaussDBAggregateFunction.COUNT);
            select.setFetchColumns(List.of(countAgg));
        } else {
            select.setFetchColumns(generateFetchColumns(false));
        }
        select.setWhereClause(whereCondition);
        return select.asString();
    }

    @Override
    public String generateUnoptimizedQueryString(GaussDBSelect select, GaussDBExpression whereCondition) {
        GaussDBExpression countExpr = new GaussDBIfFunction(whereCondition, GaussDBConstant.createIntConstant(1),
                GaussDBConstant.createIntConstant(0));
        select.setFetchColumns(List.of(countExpr));
        select.setWhereClause(null);
        select.setOrderByClauses(List.of());
        return "SELECT SUM(ref0) FROM (" + select.asString() + ") AS res";
    }

    @Override
    public String generateExplainQuery(GaussDBSelect select) {
        return "EXPLAIN " + select.asString();
    }

    public GaussDBAggregate generateAggregate() {
        GaussDBAggregateFunction func = Randomly.fromOptions(GaussDBAggregateFunction.values());
        if (func.isVariadic()) {
            int nrExprs = Randomly.smallNumber() + 1;
            List<GaussDBExpression> exprs = IntStream.range(0, nrExprs).mapToObj(index -> generateExpression())
                    .collect(Collectors.toList());
            return new GaussDBAggregate(exprs, func);
        } else {
            return new GaussDBAggregate(List.of(generateExpression()), func);
        }
    }

    @Override
    public boolean mutate(GaussDBSelect select) {
        List<Function<GaussDBSelect, Boolean>> mutators = new ArrayList<>();
        mutators.add(this::mutateWhere);
        mutators.add(this::mutateGroupBy);
        mutators.add(this::mutateHaving);
        mutators.add(this::mutateAnd);
        mutators.add(this::mutateOr);
        mutators.add(this::mutateDistinct);
        return Randomly.fromList(mutators).apply(select);
    }

    boolean mutateDistinct(GaussDBSelect select) {
        if (select.getSelectType() != SelectType.ALL) {
            select.setSelectType(SelectType.ALL);
            return true;
        } else {
            select.setSelectType(SelectType.DISTINCT);
            return false;
        }
    }

    boolean mutateWhere(GaussDBSelect select) {
        boolean increase = select.getWhereClause() != null;
        if (increase) {
            select.setWhereClause(null);
        } else {
            select.setWhereClause(generateExpression());
        }
        return increase;
    }

    boolean mutateGroupBy(GaussDBSelect select) {
        boolean increase = !select.getGroupByExpressions().isEmpty();
        if (increase) {
            select.clearGroupByExpressions();
        } else {
            select.setGroupByExpressions(select.getFetchColumns());
        }
        return increase;
    }

    boolean mutateHaving(GaussDBSelect select) {
        if (select.getGroupByExpressions().isEmpty()) {
            select.setGroupByExpressions(select.getFetchColumns());
            select.setHavingClause(generateExpression());
            return false;
        } else {
            if (select.getHavingClause() == null) {
                select.setHavingClause(generateExpression());
                return false;
            } else {
                select.setHavingClause(null);
                return true;
            }
        }
    }

    boolean mutateAnd(GaussDBSelect select) {
        if (select.getWhereClause() == null) {
            select.setWhereClause(generateExpression());
        } else {
            GaussDBExpression newWhere = new GaussDBBinaryLogicalOperation(select.getWhereClause(), generateExpression(),
                    GaussDBBinaryLogicalOperator.AND);
            select.setWhereClause(newWhere);
        }
        return false;
    }

    boolean mutateOr(GaussDBSelect select) {
        if (select.getWhereClause() == null) {
            select.setWhereClause(generateExpression());
            return false;
        } else {
            GaussDBExpression newWhere = new GaussDBBinaryLogicalOperation(select.getWhereClause(), generateExpression(),
                    GaussDBBinaryLogicalOperator.OR);
            select.setWhereClause(newWhere);
            return true;
        }
    }

    @Override
    public List<GaussDBExpression> generateOrderBys() {
        return generateExpressions(Randomly.smallNumber() + 1);
    }

    @Override
    public GaussDBMExpressionGenerator setTablesAndColumns(AbstractTables<GaussDBTable, GaussDBColumn> tables) {
        this.tables = tables.getTables();
        this.columns = tables.getColumns();
        return this;
    }
}
