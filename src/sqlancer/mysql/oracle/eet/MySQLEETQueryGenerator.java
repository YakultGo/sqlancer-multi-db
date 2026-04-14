package sqlancer.mysql.oracle.eet;

import java.util.ArrayList;
import java.util.List;

import sqlancer.Randomly;
import sqlancer.common.schema.AbstractTables;
import sqlancer.mysql.MySQLGlobalState;
import sqlancer.mysql.MySQLSchema.MySQLColumn;
import sqlancer.mysql.MySQLSchema.MySQLTable;
import sqlancer.mysql.ast.MySQLCteDefinition;
import sqlancer.mysql.ast.MySQLCteTableReference;
import sqlancer.mysql.ast.MySQLDerivedTable;
import sqlancer.mysql.ast.MySQLConstant;
import sqlancer.mysql.ast.MySQLExpression;
import sqlancer.mysql.ast.MySQLSelect;
import sqlancer.mysql.ast.MySQLText;
import sqlancer.mysql.ast.MySQLUnionSelect;
import sqlancer.mysql.ast.MySQLWithSelect;
import sqlancer.mysql.gen.MySQLExpressionGenerator;

/**
 * Builds random SELECT shapes for EET: plain SELECT, UNION, WITH (CTE), and derived table (subquery in FROM).
 */
public final class MySQLEETQueryGenerator {

    private static final String CTE_NAME = "eet_cte";
    private static final String DERIVED_ALIAS = "eet_sub";

    private MySQLEETQueryGenerator() {
    }

    /**
     * WHERE for an outer SELECT whose FROM is only a CTE or derived table: must not reference base tables
     * (e.g. {@code t2.c0}), only {@code alias.ref0} … columns produced by the inner subquery.
     */
    private static MySQLExpression outerScopeWhereClause(String aliasPrefix, int refColumnCount) {
        if (refColumnCount <= 0) {
            return MySQLConstant.createTrue();
        }
        // getNextLong(lower, upper) uses an exclusive upper bound
        int refIdx = (int) Randomly.getNotCachedInteger(0, refColumnCount);
        String col = aliasPrefix + ".ref" + refIdx;
        int kind = (int) Randomly.getNotCachedInteger(0, 3);
        switch (kind) {
        case 0:
            return new MySQLText("(" + col + " IS NOT NULL)");
        case 1:
            return new MySQLText("(" + col + " = " + col + ")");
        default:
            return new MySQLText("((" + col + " IS NOT NULL) OR (" + col + " IS NULL))");
        }
    }

    /**
     * @param choiceMode 0 = plain, 1 = UNION, 2 = WITH/CTE, 3 = derived table in FROM
     */
    public static MySQLExpression generateEETQuery(MySQLGlobalState state, MySQLExpressionGenerator gen,
            AbstractTables<MySQLTable, MySQLColumn> tables, int choiceMode) {
        switch (choiceMode) {
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

    /**
     * Random shape in {@code [0, 3]}: plain, UNION, WITH, derived.
     */
    public static MySQLExpression generateEETQueryRandomShape(MySQLGlobalState state, MySQLExpressionGenerator gen,
            AbstractTables<MySQLTable, MySQLColumn> tables) {
        int mode = (int) Randomly.getNotCachedInteger(0, 4);
        return generateEETQuery(state, gen, tables, mode);
    }

    public static MySQLSelect buildBaseSelect(MySQLExpressionGenerator gen) {
        MySQLSelect select = gen.generateSelect();
        select.setFetchColumns(gen.generateFetchColumns(true));
        select.setJoinClauses(gen.getRandomJoinClauses());
        select.setFromList(gen.getTableRefs());
        select.setWhereClause(gen.generateBooleanExpression());
        select.setGroupByExpressions(List.of());
        select.setOrderByClauses(List.of());
        return select;
    }

    private static MySQLUnionSelect buildUnionSelect(MySQLExpressionGenerator gen) {
        MySQLSelect left = buildBaseSelect(gen);
        List<MySQLExpression> sharedCols = new ArrayList<>(left.getFetchColumns());
        MySQLSelect right = gen.generateSelect();
        right.setFetchColumns(new ArrayList<>(sharedCols));
        right.setJoinClauses(gen.getRandomJoinClauses());
        right.setFromList(gen.getTableRefs());
        right.setWhereClause(gen.generateBooleanExpression());
        right.setGroupByExpressions(List.of());
        right.setOrderByClauses(List.of());
        return new MySQLUnionSelect(List.of(left, right), Randomly.getBoolean());
    }

    private static MySQLWithSelect buildWithSelect(MySQLExpressionGenerator gen) {
        MySQLSelect cteBody = buildBaseSelect(gen);
        MySQLCteDefinition cte = new MySQLCteDefinition(CTE_NAME, cteBody);
        MySQLSelect main = gen.generateSelect();
        List<MySQLExpression> outerFetch = new ArrayList<>();
        for (int i = 0; i < cteBody.getFetchColumns().size(); i++) {
            outerFetch.add(new MySQLText(CTE_NAME + ".ref" + i));
        }
        main.setFetchColumns(outerFetch);
        main.setFromList(List.of(new MySQLCteTableReference(CTE_NAME)));
        main.setJoinList(List.of());
        main.setWhereClause(outerScopeWhereClause(CTE_NAME, cteBody.getFetchColumns().size()));
        main.setGroupByExpressions(List.of());
        main.setOrderByClauses(List.of());
        return new MySQLWithSelect(List.of(cte), main);
    }

    private static MySQLSelect buildDerivedSelect(MySQLExpressionGenerator gen) {
        MySQLSelect inner = buildBaseSelect(gen);
        MySQLDerivedTable derived = new MySQLDerivedTable(inner, DERIVED_ALIAS);
        List<MySQLExpression> outerFetch = new ArrayList<>();
        for (int i = 0; i < inner.getFetchColumns().size(); i++) {
            outerFetch.add(new MySQLText(DERIVED_ALIAS + ".ref" + i));
        }
        MySQLSelect outer = gen.generateSelect();
        outer.setFetchColumns(outerFetch);
        outer.setFromList(List.of(derived));
        outer.setJoinList(List.of());
        outer.setWhereClause(outerScopeWhereClause(DERIVED_ALIAS, inner.getFetchColumns().size()));
        outer.setGroupByExpressions(List.of());
        outer.setOrderByClauses(List.of());
        return outer;
    }
}
