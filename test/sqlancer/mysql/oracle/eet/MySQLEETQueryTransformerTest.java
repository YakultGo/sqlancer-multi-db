package sqlancer.mysql.oracle.eet;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import sqlancer.MainOptions;
import sqlancer.Randomly;
import sqlancer.mysql.MySQLGlobalState;
import sqlancer.mysql.MySQLOptions;
import sqlancer.mysql.MySQLSchema;
import sqlancer.mysql.MySQLVisitor;
import sqlancer.mysql.ast.MySQLCteDefinition;
import sqlancer.mysql.ast.MySQLCteTableReference;
import sqlancer.mysql.ast.MySQLColumnReference;
import sqlancer.mysql.ast.MySQLConstant;
import sqlancer.mysql.ast.MySQLDerivedTable;
import sqlancer.mysql.ast.MySQLExpression;
import sqlancer.mysql.ast.MySQLJoin;
import sqlancer.mysql.ast.MySQLSelect;
import sqlancer.mysql.ast.MySQLTableReference;
import sqlancer.mysql.ast.MySQLText;
import sqlancer.mysql.ast.MySQLUnionSelect;
import sqlancer.mysql.ast.MySQLWithSelect;
import sqlancer.mysql.gen.MySQLExpressionGenerator;

class MySQLEETQueryTransformerTest {

    private MySQLExpressionGenerator gen;
    private MySQLEETQueryTransformer qtf;

    @BeforeEach
    void setUp() {
        new Randomly(99L);
        MySQLGlobalState state = new MySQLGlobalState();
        state.setMainOptions(new MainOptions());
        state.setDbmsSpecificOptions(new MySQLOptions());
        state.setRandomly(new Randomly(99L));
        gen = new MySQLExpressionGenerator(state);
        MySQLSchema.MySQLColumn col = new MySQLSchema.MySQLColumn("c", MySQLSchema.MySQLDataType.INT, false, 0);
        gen.setColumns(List.of(col));
        qtf = new MySQLEETQueryTransformer(gen);
    }

    @Test
    void eqTransformQuery_changesWhereAndPreservesOriginal() {
        MySQLSchema.MySQLColumn col = new MySQLSchema.MySQLColumn("c", MySQLSchema.MySQLDataType.INT, false, 0);
        MySQLSchema.MySQLTable table = new MySQLSchema.MySQLTable("t", List.of(col), List.of(),
                MySQLSchema.MySQLTable.MySQLEngine.INNO_DB);
        MySQLSelect select = new MySQLSelect();
        select.setFetchColumns(List.of(new MySQLColumnReference(col, null)));
        select.setFromList(List.of(new MySQLTableReference(table)));
        select.setJoinList(List.of());
        select.setWhereClause(MySQLConstant.createTrue());
        select.setGroupByExpressions(List.of());
        select.setOrderByClauses(List.of());

        MySQLSelect copy = qtf.eqTransformQuery(select);
        assertNotSame(select, copy);
        assertNotEquals(MySQLVisitor.asString(select), MySQLVisitor.asString(copy));
        String origWhere = MySQLVisitor.asString(select.getWhereClause());
        assertTrue(copy.getWhereClause() != null);
        assertTrue(MySQLVisitor.asString(copy.getWhereClause()).length() > origWhere.length());
    }

    @Test
    void eqTransformQuery_transformsJoinOn() {
        MySQLSchema.MySQLColumn c1 = new MySQLSchema.MySQLColumn("c", MySQLSchema.MySQLDataType.INT, false, 0);
        MySQLSchema.MySQLTable t1 = new MySQLSchema.MySQLTable("t1", List.of(c1), List.of(),
                MySQLSchema.MySQLTable.MySQLEngine.INNO_DB);
        MySQLSchema.MySQLTable t2 = new MySQLSchema.MySQLTable("t2", List.of(c1), List.of(),
                MySQLSchema.MySQLTable.MySQLEngine.INNO_DB);
        MySQLJoin join = new MySQLJoin(t2, MySQLConstant.createTrue(), MySQLJoin.JoinType.INNER);
        MySQLSelect select = new MySQLSelect();
        select.setFetchColumns(List.of(new MySQLColumnReference(c1, null)));
        select.setFromList(List.of(new MySQLTableReference(t1)));
        select.setJoinList(List.of(join));
        select.setWhereClause(null);
        select.setGroupByExpressions(List.of());
        select.setOrderByClauses(List.of());

        MySQLSelect copy = qtf.eqTransformQuery(select);
        assertNotEquals(MySQLVisitor.asString(join.getOnClause()),
                MySQLVisitor.asString(((MySQLJoin) copy.getJoinList().get(0)).getOnClause()));
    }

    @Test
    void eqTransformRoot_transformsUnionBranches() {
        MySQLSchema.MySQLColumn col = new MySQLSchema.MySQLColumn("c", MySQLSchema.MySQLDataType.INT, false, 0);
        MySQLSchema.MySQLTable table = new MySQLSchema.MySQLTable("t", List.of(col), List.of(),
                MySQLSchema.MySQLTable.MySQLEngine.INNO_DB);
        MySQLSelect left = new MySQLSelect();
        left.setFetchColumns(List.of(new MySQLColumnReference(col, null)));
        left.setFromList(List.of(new MySQLTableReference(table)));
        left.setJoinList(List.of());
        left.setWhereClause(MySQLConstant.createTrue());
        left.setGroupByExpressions(List.of());
        left.setOrderByClauses(List.of());
        MySQLSelect right = new MySQLSelect();
        right.setFetchColumns(List.of(new MySQLColumnReference(col, null)));
        right.setFromList(List.of(new MySQLTableReference(table)));
        right.setJoinList(List.of());
        right.setWhereClause(MySQLConstant.createTrue());
        right.setGroupByExpressions(List.of());
        right.setOrderByClauses(List.of());
        MySQLUnionSelect u = new MySQLUnionSelect(List.of(left, right), true);
        MySQLExpression out = qtf.eqTransformRoot(u);
        assertNotSame(u, out);
        assertTrue(out instanceof MySQLUnionSelect);
        MySQLUnionSelect u2 = (MySQLUnionSelect) out;
        assertNotEquals(MySQLVisitor.asString(u.getBranches().get(0)), MySQLVisitor.asString(u2.getBranches().get(0)));
    }

    @Test
    void eqTransformRoot_transformsWithCteAndMain() {
        MySQLSchema.MySQLColumn col = new MySQLSchema.MySQLColumn("c", MySQLSchema.MySQLDataType.INT, false, 0);
        MySQLSchema.MySQLTable table = new MySQLSchema.MySQLTable("t", List.of(col), List.of(),
                MySQLSchema.MySQLTable.MySQLEngine.INNO_DB);
        MySQLSelect cteBody = new MySQLSelect();
        cteBody.setFetchColumns(List.of(new MySQLColumnReference(col, null)));
        cteBody.setFromList(List.of(new MySQLTableReference(table)));
        cteBody.setJoinList(List.of());
        cteBody.setWhereClause(MySQLConstant.createTrue());
        cteBody.setGroupByExpressions(List.of());
        cteBody.setOrderByClauses(List.of());
        MySQLSelect main = new MySQLSelect();
        main.setFetchColumns(List.of(new MySQLText("eet_cte.c")));
        main.setFromList(List.of(new MySQLCteTableReference("eet_cte")));
        main.setJoinList(List.of());
        // Raw predicate: second WHERE TRUE in same eqTransformQuery can trigger IgnoreMe in tryConstBoolTransform
        main.setWhereClause(new MySQLText("(eet_cte.c IS NOT NULL)"));
        main.setGroupByExpressions(List.of());
        main.setOrderByClauses(List.of());
        MySQLWithSelect w = new MySQLWithSelect(List.of(new MySQLCteDefinition("eet_cte", cteBody)), main);
        MySQLExpression out = qtf.eqTransformRoot(w);
        assertNotSame(w, out);
        assertTrue(out instanceof MySQLWithSelect);
    }

    @Test
    void eqTransformQuery_transformsDerivedTableSubquery() {
        MySQLSchema.MySQLColumn col = new MySQLSchema.MySQLColumn("c", MySQLSchema.MySQLDataType.INT, false, 0);
        MySQLSchema.MySQLTable table = new MySQLSchema.MySQLTable("t", List.of(col), List.of(),
                MySQLSchema.MySQLTable.MySQLEngine.INNO_DB);
        MySQLSelect inner = new MySQLSelect();
        inner.setFetchColumns(List.of(new MySQLColumnReference(col, null)));
        inner.setFromList(List.of(new MySQLTableReference(table)));
        inner.setJoinList(List.of());
        inner.setWhereClause(MySQLConstant.createTrue());
        inner.setGroupByExpressions(List.of());
        inner.setOrderByClauses(List.of());
        MySQLDerivedTable derived = new MySQLDerivedTable(inner, "eet_sub");
        MySQLSelect outer = new MySQLSelect();
        outer.setFetchColumns(List.of(new MySQLText("eet_sub.c")));
        outer.setFromList(List.of(derived));
        outer.setJoinList(List.of());
        outer.setWhereClause(new MySQLText("(eet_sub.c IS NOT NULL)"));
        outer.setGroupByExpressions(List.of());
        outer.setOrderByClauses(List.of());
        MySQLSelect copy = qtf.eqTransformQuery(outer);
        MySQLDerivedTable d2 = (MySQLDerivedTable) copy.getFromList().get(0);
        assertNotEquals(MySQLVisitor.asString(inner.getWhereClause()), MySQLVisitor.asString(d2.getSubquery().getWhereClause()));
    }
}
