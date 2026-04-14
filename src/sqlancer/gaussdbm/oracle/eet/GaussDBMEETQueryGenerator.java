package sqlancer.gaussdbm.oracle.eet;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import sqlancer.Randomly;
import sqlancer.common.schema.AbstractTables;
import sqlancer.gaussdbm.GaussDBMGlobalState;
import sqlancer.gaussdbm.GaussDBMSchema.GaussDBColumn;
import sqlancer.gaussdbm.GaussDBMSchema.GaussDBTable;
import sqlancer.gaussdbm.ast.GaussDBCteDefinition;
import sqlancer.gaussdbm.ast.GaussDBCteTableReference;
import sqlancer.gaussdbm.ast.GaussDBDerivedTable;
import sqlancer.gaussdbm.ast.GaussDBConstant;
import sqlancer.gaussdbm.ast.GaussDBExpression;
import sqlancer.gaussdbm.ast.GaussDBSelect;
import sqlancer.gaussdbm.ast.GaussDBText;
import sqlancer.gaussdbm.ast.GaussDBUnionSelect;
import sqlancer.gaussdbm.ast.GaussDBWithSelect;
import sqlancer.gaussdbm.gen.GaussDBMExpressionGenerator;

/**
 * Builds random SELECT shapes for EET: plain SELECT, UNION, WITH (CTE), and derived table (subquery in FROM).
 */
public final class GaussDBMEETQueryGenerator {

    private static final String CTE_NAME = "eet_cte";
    private static final String DERIVED_ALIAS = "eet_sub";

    private GaussDBMEETQueryGenerator() {
    }

    private static GaussDBExpression outerScopeWhereClause(String aliasPrefix, int refColumnCount) {
        if (refColumnCount <= 0) {
            return GaussDBConstant.createBooleanConstant(true);
        }
        int refIdx = (int) Randomly.getNotCachedInteger(0, refColumnCount);
        String col = aliasPrefix + ".ref" + refIdx;
        int kind = (int) Randomly.getNotCachedInteger(0, 3);
        switch (kind) {
        case 0:
            return new GaussDBText("(" + col + " IS NOT NULL)");
        case 1:
            return new GaussDBText("(" + col + " = " + col + ")");
        default:
            return new GaussDBText("((" + col + " IS NOT NULL) OR (" + col + " IS NULL))");
        }
    }

    public static GaussDBExpression generateEETQueryRandomShape(GaussDBMGlobalState state,
            GaussDBMExpressionGenerator gen, AbstractTables<GaussDBTable, GaussDBColumn> tables) {
        Objects.requireNonNull(state);
        Objects.requireNonNull(tables);
        int mode = (int) Randomly.getNotCachedInteger(0, 4);
        switch (mode) {
        case 1:
            return buildUnionSelect(gen);
        case 2:
            return buildWithSelect(gen);
        case 3:
            return buildDerivedSelect(gen);
        case 0:
        default:
            return buildBaseSelect(gen);
        }
    }

    public static GaussDBSelect buildBaseSelect(GaussDBMExpressionGenerator gen) {
        GaussDBSelect select = gen.generateSelect();
        select.setFetchColumns(gen.generateFetchColumns(true));
        select.setJoinClauses(gen.getRandomJoinClauses());
        select.setFromList(gen.getTableRefs());
        select.setWhereClause(gen.generateBooleanExpression());
        select.setGroupByExpressions(List.of());
        select.setOrderByClauses(List.of());
        return select;
    }

    private static GaussDBUnionSelect buildUnionSelect(GaussDBMExpressionGenerator gen) {
        GaussDBSelect left = buildBaseSelect(gen);
        List<GaussDBExpression> sharedCols = new ArrayList<>(left.getFetchColumns());
        GaussDBSelect right = gen.generateSelect();
        right.setFetchColumns(new ArrayList<>(sharedCols));
        right.setJoinClauses(gen.getRandomJoinClauses());
        right.setFromList(gen.getTableRefs());
        right.setWhereClause(gen.generateBooleanExpression());
        right.setGroupByExpressions(List.of());
        right.setOrderByClauses(List.of());
        return new GaussDBUnionSelect(List.of(left, right), Randomly.getBoolean());
    }

    private static GaussDBWithSelect buildWithSelect(GaussDBMExpressionGenerator gen) {
        GaussDBSelect cteBody = buildBaseSelect(gen);
        GaussDBCteDefinition cte = new GaussDBCteDefinition(CTE_NAME, cteBody);
        GaussDBSelect main = gen.generateSelect();
        List<GaussDBExpression> outerFetch = new ArrayList<>();
        for (int i = 0; i < cteBody.getFetchColumns().size(); i++) {
            outerFetch.add(new GaussDBText(CTE_NAME + ".ref" + i));
        }
        main.setFetchColumns(outerFetch);
        main.setFromList(List.of(new GaussDBCteTableReference(CTE_NAME)));
        main.setJoinList(List.of());
        main.setWhereClause(outerScopeWhereClause(CTE_NAME, cteBody.getFetchColumns().size()));
        main.setGroupByExpressions(List.of());
        main.setOrderByClauses(List.of());
        return new GaussDBWithSelect(List.of(cte), main);
    }

    private static GaussDBSelect buildDerivedSelect(GaussDBMExpressionGenerator gen) {
        GaussDBSelect inner = buildBaseSelect(gen);
        GaussDBDerivedTable derived = new GaussDBDerivedTable(inner, DERIVED_ALIAS);
        List<GaussDBExpression> outerFetch = new ArrayList<>();
        for (int i = 0; i < inner.getFetchColumns().size(); i++) {
            outerFetch.add(new GaussDBText(DERIVED_ALIAS + ".ref" + i));
        }
        GaussDBSelect outer = gen.generateSelect();
        outer.setFetchColumns(outerFetch);
        outer.setFromList(List.of(derived));
        outer.setJoinList(List.of());
        outer.setWhereClause(outerScopeWhereClause(DERIVED_ALIAS, inner.getFetchColumns().size()));
        outer.setGroupByExpressions(List.of());
        outer.setOrderByClauses(List.of());
        return outer;
    }
}
