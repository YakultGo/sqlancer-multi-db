package sqlancer.gaussdba.oracle.tlp;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import sqlancer.Randomly;
import sqlancer.common.gen.ExpressionGenerator;
import sqlancer.common.oracle.TernaryLogicPartitioningOracleBase;
import sqlancer.common.oracle.TestOracle;
import sqlancer.gaussdba.GaussDBAGlobalState;
import sqlancer.gaussdba.GaussDBASchema;
import sqlancer.gaussdba.GaussDBASchema.GaussDBAColumn;
import sqlancer.gaussdba.GaussDBASchema.GaussDBATable;
import sqlancer.gaussdba.GaussDBASchema.GaussDBATables;
import sqlancer.gaussdba.ast.GaussDBAColumnReference;
import sqlancer.gaussdba.ast.GaussDBAExpression;
import sqlancer.gaussdba.ast.GaussDBAJoin;
import sqlancer.gaussdba.ast.GaussDBAJoin.GaussDBAJoinType;
import sqlancer.gaussdba.ast.GaussDBASelect;
import sqlancer.gaussdba.ast.GaussDBATableReference;
import sqlancer.gaussdba.gen.GaussDBAExpressionGenerator;

public class GaussDBATLPBase extends TernaryLogicPartitioningOracleBase<GaussDBAExpression, GaussDBAGlobalState>
        implements TestOracle<GaussDBAGlobalState> {

    protected GaussDBASchema s;
    protected GaussDBATables targetTables;
    protected GaussDBAExpressionGenerator gen;
    protected GaussDBASelect select;

    public GaussDBATLPBase(GaussDBAGlobalState state) {
        super(state);
    }

    @Override
    public void check() throws SQLException {
        s = state.getSchema();
        targetTables = s.getRandomTableNonEmptyTables();
        List<GaussDBATable> tables = targetTables.getTables();
        List<GaussDBAJoin> joins = getJoinStatements(state, targetTables.getColumns(), tables);
        generateSelectBase(tables, joins);
    }

    protected List<GaussDBAJoin> getJoinStatements(GaussDBAGlobalState globalState, List<GaussDBAColumn> columns,
            List<GaussDBATable> tables) {
        List<GaussDBAJoin> joinStatements = new ArrayList<>();
        GaussDBAExpressionGenerator gen = new GaussDBAExpressionGenerator(globalState).setColumns(columns);
        for (int i = 1; i < tables.size(); i++) {
            GaussDBAExpression joinClause = gen.generateBooleanExpression();
            GaussDBATable table = Randomly.fromList(tables);
            tables.remove(table);
            GaussDBAJoinType options = GaussDBAJoinType.getRandom();
            GaussDBAJoin j = new GaussDBAJoin(GaussDBATableReference.create(table), joinClause, options);
            joinStatements.add(j);
        }
        return joinStatements;
    }

    protected void generateSelectBase(List<GaussDBATable> tables, List<GaussDBAJoin> joins) {
        List<GaussDBAExpression> tableList = tables.stream().map(GaussDBATableReference::create)
                .collect(Collectors.toList());
        gen = new GaussDBAExpressionGenerator(state).setColumns(targetTables.getColumns());
        initializeTernaryPredicateVariants();
        select = new GaussDBASelect();
        select.setFetchColumns(generateFetchColumns());
        select.setFromList(tableList);
        select.setWhereClause(null);
        select.setJoinClauses(joins);
    }

    List<GaussDBAExpression> generateFetchColumns() {
        if (Randomly.getBooleanWithRatherLowProbability()) {
            return Arrays.asList(GaussDBAColumnReference.create(GaussDBAColumn.createDummy("*"), null));
        }
        List<GaussDBAExpression> fetchColumns = new ArrayList<>();
        List<GaussDBAColumn> targetColumns = Randomly.nonEmptySubset(targetTables.getColumns());
        for (GaussDBAColumn c : targetColumns) {
            fetchColumns.add(new GaussDBAColumnReference(c, null));
        }
        return fetchColumns;
    }

    @Override
    protected ExpressionGenerator<GaussDBAExpression> getGen() {
        return gen;
    }
}