package sqlancer.gaussdbm.gen;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import sqlancer.Randomly;
import sqlancer.gaussdbm.GaussDBMGlobalState;
import sqlancer.gaussdbm.GaussDBMSchema.GaussDBTables;
import sqlancer.gaussdbm.ast.GaussDBConstant;
import sqlancer.gaussdbm.ast.GaussDBExpression;
import sqlancer.gaussdbm.ast.GaussDBSelect;
import sqlancer.gaussdbm.ast.GaussDBTableReference;

public final class GaussDBMRandomQuerySynthesizer {

    private GaussDBMRandomQuerySynthesizer() {
    }

    public static GaussDBSelect generate(GaussDBMGlobalState globalState, int nrColumns) {
        GaussDBTables tables = globalState.getSchema().getRandomTableNonEmptyTables();
        GaussDBMExpressionGenerator gen = new GaussDBMExpressionGenerator(globalState).setColumns(tables.getColumns());
        GaussDBSelect select = new GaussDBSelect();

        List<GaussDBExpression> allColumns = new ArrayList<>();
        List<GaussDBExpression> columnsWithoutAggregations = new ArrayList<>();

        boolean hasGeneratedAggregate = false;

        select.setSelectType(Randomly.fromOptions(GaussDBSelect.SelectType.values()));
        for (int i = 0; i < nrColumns; i++) {
            if (Randomly.getBoolean()) {
                GaussDBExpression expression = gen.generateExpression();
                allColumns.add(expression);
                columnsWithoutAggregations.add(expression);
            } else {
                allColumns.add(gen.generateAggregate());
                hasGeneratedAggregate = true;
            }
        }
        select.setFetchColumns(allColumns);

        List<GaussDBExpression> tableList = tables.getTables().stream().map(GaussDBTableReference::create)
                .collect(Collectors.toList());
        select.setFromList(tableList);
        if (Randomly.getBoolean()) {
            select.setWhereClause(gen.generateExpression());
        }
        if (Randomly.getBooleanWithRatherLowProbability()) {
            select.setOrderByClauses(gen.generateOrderBys());
        }
        if (hasGeneratedAggregate || Randomly.getBoolean()) {
            select.setGroupByExpressions(columnsWithoutAggregations);
            if (Randomly.getBoolean()) {
                select.setHavingClause(gen.generateHavingClause());
            }
        }
        if (Randomly.getBoolean()) {
            select.setLimitClause(GaussDBConstant.createIntConstant(Randomly.getPositiveOrZeroNonCachedInteger()));
            if (Randomly.getBoolean()) {
                select.setOffsetClause(GaussDBConstant.createIntConstant(Randomly.getPositiveOrZeroNonCachedInteger()));
            }
        }
        return select;
    }
}
