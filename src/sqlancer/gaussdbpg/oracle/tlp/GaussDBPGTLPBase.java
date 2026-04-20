package sqlancer.gaussdbpg.oracle.tlp;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import sqlancer.Randomly;
import sqlancer.common.gen.ExpressionGenerator;
import sqlancer.common.oracle.TernaryLogicPartitioningOracleBase;
import sqlancer.common.oracle.TestOracle;
import sqlancer.gaussdbpg.GaussDBPGGlobalState;
import sqlancer.gaussdbpg.GaussDBPGSchema;
import sqlancer.gaussdbpg.GaussDBPGSchema.GaussDBPGColumn;
import sqlancer.gaussdbpg.GaussDBPGSchema.GaussDBPGTable;
import sqlancer.gaussdbpg.GaussDBPGSchema.GaussDBPGTables;
import sqlancer.gaussdbpg.ast.GaussDBPGColumnReference;
import sqlancer.gaussdbpg.ast.GaussDBPGExpression;
import sqlancer.gaussdbpg.ast.GaussDBPGJoin;
import sqlancer.gaussdbpg.ast.GaussDBPGJoin.GaussDBPGJoinType;
import sqlancer.gaussdbpg.ast.GaussDBPGSelect;
import sqlancer.gaussdbpg.ast.GaussDBPGTableReference;
import sqlancer.gaussdbpg.gen.GaussDBPGExpressionGenerator;

public class GaussDBPGTLPBase extends TernaryLogicPartitioningOracleBase<GaussDBPGExpression, GaussDBPGGlobalState>
        implements TestOracle<GaussDBPGGlobalState> {

    protected GaussDBPGSchema s;
    protected GaussDBPGTables targetTables;
    protected GaussDBPGExpressionGenerator gen;
    protected GaussDBPGSelect select;

    public GaussDBPGTLPBase(GaussDBPGGlobalState state) {
        super(state);
    }

    @Override
    public void check() throws SQLException {
        s = state.getSchema();
        targetTables = s.getRandomTableNonEmptyTables();
        List<GaussDBPGTable> tables = targetTables.getTables();
        List<GaussDBPGJoin> joins = getJoinStatements(state, targetTables.getColumns(), tables);
        generateSelectBase(tables, joins);
    }

    protected List<GaussDBPGJoin> getJoinStatements(GaussDBPGGlobalState globalState, List<GaussDBPGColumn> columns,
            List<GaussDBPGTable> tables) {
        List<GaussDBPGJoin> joinStatements = new ArrayList<>();
        GaussDBPGExpressionGenerator gen = new GaussDBPGExpressionGenerator(globalState).setColumns(columns);
        for (int i = 1; i < tables.size(); i++) {
            GaussDBPGExpression joinClause = gen.generateBooleanExpression();
            GaussDBPGTable table = Randomly.fromList(tables);
            tables.remove(table);
            GaussDBPGJoinType options = GaussDBPGJoinType.getRandom();
            GaussDBPGJoin j = new GaussDBPGJoin(GaussDBPGTableReference.create(table), joinClause, options);
            joinStatements.add(j);
        }
        return joinStatements;
    }

    protected void generateSelectBase(List<GaussDBPGTable> tables, List<GaussDBPGJoin> joins) {
        List<GaussDBPGExpression> tableList = tables.stream().map(GaussDBPGTableReference::create)
                .collect(Collectors.toList());
        gen = new GaussDBPGExpressionGenerator(state).setColumns(targetTables.getColumns());
        initializeTernaryPredicateVariants();
        select = new GaussDBPGSelect();
        select.setFetchColumns(generateFetchColumns());
        select.setFromList(tableList);
        select.setWhereClause(null);
        select.setJoinClauses(joins);
    }

    List<GaussDBPGExpression> generateFetchColumns() {
        if (Randomly.getBooleanWithRatherLowProbability()) {
            return Arrays.asList(GaussDBPGColumnReference.create(GaussDBPGColumn.createDummy("*"), null));
        }
        List<GaussDBPGExpression> fetchColumns = new ArrayList<>();
        List<GaussDBPGColumn> targetColumns = Randomly.nonEmptySubset(targetTables.getColumns());
        for (GaussDBPGColumn c : targetColumns) {
            fetchColumns.add(new GaussDBPGColumnReference(c, null));
        }
        return fetchColumns;
    }

    @Override
    protected ExpressionGenerator<GaussDBPGExpression> getGen() {
        return gen;
    }
}