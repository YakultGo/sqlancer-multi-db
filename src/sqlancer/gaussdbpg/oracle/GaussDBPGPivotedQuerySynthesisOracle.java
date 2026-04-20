package sqlancer.gaussdbpg.oracle;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import sqlancer.Randomly;
import sqlancer.SQLConnection;
import sqlancer.common.oracle.PivotedQuerySynthesisBase;
import sqlancer.common.query.Query;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.gaussdbpg.GaussDBPGExpectedValueVisitor;
import sqlancer.gaussdbpg.GaussDBPGGlobalState;
import sqlancer.gaussdbpg.GaussDBPGErrors;
import sqlancer.gaussdbpg.GaussDBPGSchema.GaussDBPGColumn;
import sqlancer.gaussdbpg.GaussDBPGSchema.GaussDBPGRowValue;
import sqlancer.gaussdbpg.GaussDBPGSchema.GaussDBPGTables;
import sqlancer.gaussdbpg.GaussDBPGToStringVisitor;
import sqlancer.gaussdbpg.ast.GaussDBPGColumnValue;
import sqlancer.gaussdbpg.ast.GaussDBPGConstant;
import sqlancer.gaussdbpg.ast.GaussDBPGDataType;
import sqlancer.gaussdbpg.ast.GaussDBPGExpression;
import sqlancer.gaussdbpg.ast.GaussDBPGSelect;
import sqlancer.gaussdbpg.ast.GaussDBPGTableReference;
import sqlancer.gaussdbpg.ast.GaussDBPGUnaryPostfixOperation;
import sqlancer.gaussdbpg.ast.GaussDBPGUnaryPostfixOperation.UnaryPostfixOperator;
import sqlancer.gaussdbpg.gen.GaussDBPGExpressionGenerator;

public class GaussDBPGPivotedQuerySynthesisOracle
        extends PivotedQuerySynthesisBase<GaussDBPGGlobalState, GaussDBPGRowValue, GaussDBPGExpression, SQLConnection> {

    private List<GaussDBPGColumn> fetchColumns;

    public GaussDBPGPivotedQuerySynthesisOracle(GaussDBPGGlobalState globalState) throws SQLException {
        super(globalState);
        GaussDBPGErrors.addExpressionErrors(errors);
    }

    @Override
    public SQLQueryAdapter getRectifiedQuery() throws SQLException {
        GaussDBPGTables randomFromTables = globalState.getSchema().getRandomTableNonEmptyTables();

        GaussDBPGSelect selectStatement = new GaussDBPGSelect();
        selectStatement.setSelectType(Randomly.fromOptions(GaussDBPGSelect.GaussDBPGSelectType.values()));
        List<GaussDBPGColumn> columns = randomFromTables.getColumns();
        pivotRow = randomFromTables.getRandomRowValue(globalState.getConnection(), globalState);

        fetchColumns = columns;
        selectStatement.setFromList(randomFromTables.getTables().stream().map(t -> GaussDBPGTableReference.create(t))
                .collect(Collectors.toList()));
        selectStatement.setFetchColumns(fetchColumns.stream()
                .map(c -> new GaussDBPGColumnValue(getFetchValueAliasedColumn(c), pivotRow.getValues().get(c)))
                .collect(Collectors.toList()));
        GaussDBPGExpression whereClause = generateRectifiedExpression(columns, pivotRow);
        selectStatement.setWhereClause(whereClause);
        List<GaussDBPGExpression> groupByClause = generateGroupByClause(columns, pivotRow);
        selectStatement.setGroupByExpressions(groupByClause);
        GaussDBPGExpression limitClause = generateLimit();
        selectStatement.setLimitClause(limitClause);
        if (limitClause != null) {
            GaussDBPGExpression offsetClause = generateOffset();
            selectStatement.setOffsetClause(offsetClause);
        }
        List<GaussDBPGExpression> orderBy = new GaussDBPGExpressionGenerator(globalState).setColumns(columns)
                .generateOrderBys();
        selectStatement.setOrderByClauses(orderBy);
        return new SQLQueryAdapter(GaussDBPGToStringVisitor.asString(selectStatement));
    }

    private GaussDBPGColumn getFetchValueAliasedColumn(GaussDBPGColumn c) {
        GaussDBPGColumn aliasedColumn = new GaussDBPGColumn(c.getName() + " AS " + c.getTable().getName() + c.getName(),
                c.getType());
        aliasedColumn.setTable(c.getTable());
        return aliasedColumn;
    }

    private List<GaussDBPGExpression> generateGroupByClause(List<GaussDBPGColumn> columns, GaussDBPGRowValue rw) {
        if (Randomly.getBoolean()) {
            return columns.stream().map(c -> GaussDBPGColumnValue.create(c, rw.getValues().get(c)))
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    private GaussDBPGConstant generateLimit() {
        if (Randomly.getBoolean()) {
            return GaussDBPGConstant.createIntConstant(Integer.MAX_VALUE);
        } else {
            return null;
        }
    }

    private GaussDBPGExpression generateOffset() {
        if (Randomly.getBoolean()) {
            return GaussDBPGConstant.createIntConstant(0);
        } else {
            return null;
        }
    }

    private GaussDBPGExpression generateRectifiedExpression(List<GaussDBPGColumn> columns, GaussDBPGRowValue rw) {
        GaussDBPGExpression expr = new GaussDBPGExpressionGenerator(globalState).setColumns(columns).setRowValue(rw)
                .generateExpressionWithExpectedResult(GaussDBPGDataType.BOOLEAN);
        GaussDBPGExpression result;
        GaussDBPGConstant expectedVal = expr.getExpectedValue();
        if (expectedVal == null || expectedVal.isNull()) {
            result = GaussDBPGUnaryPostfixOperation.create(expr, UnaryPostfixOperator.IS_NULL);
        } else {
            GaussDBPGConstant castedVal = expectedVal.cast(GaussDBPGDataType.BOOLEAN);
            if (castedVal == null) {
                // Cannot cast to boolean, treat as IS_NULL
                result = GaussDBPGUnaryPostfixOperation.create(expr, UnaryPostfixOperator.IS_NULL);
            } else {
                result = GaussDBPGUnaryPostfixOperation.create(expr,
                        castedVal.asBoolean() ? UnaryPostfixOperator.IS_TRUE : UnaryPostfixOperator.IS_FALSE);
            }
        }
        rectifiedPredicates.add(result);
        return result;
    }

    @Override
    protected Query<SQLConnection> getContainmentCheckQuery(Query<?> query) throws SQLException {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM ("); // ANOTHER SELECT TO USE ORDER BY without restrictions
        sb.append(query.getUnterminatedQueryString());
        sb.append(") as result WHERE ");
        int i = 0;
        for (GaussDBPGColumn c : fetchColumns) {
            if (i++ != 0) {
                sb.append(" AND ");
            }
            sb.append("result.");
            sb.append(c.getTable().getName());
            sb.append(c.getName());
            if (pivotRow.getValues().get(c).isNull()) {
                sb.append(" IS NULL");
            } else {
                sb.append(" = ");
                sb.append(pivotRow.getValues().get(c).getTextRepresentation());
            }
        }
        String resultingQueryString = sb.toString();
        return new SQLQueryAdapter(resultingQueryString, errors);
    }

    @Override
    protected String getExpectedValues(GaussDBPGExpression expr) {
        return GaussDBPGExpectedValueVisitor.asExpectedValues(expr);
    }
}