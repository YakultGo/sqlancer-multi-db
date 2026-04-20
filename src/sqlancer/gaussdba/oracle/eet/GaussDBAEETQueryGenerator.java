package sqlancer.gaussdba.oracle.eet;

import java.util.List;
import java.util.Objects;

import sqlancer.Randomly;
import sqlancer.common.schema.AbstractTables;
import sqlancer.gaussdba.GaussDBAGlobalState;
import sqlancer.gaussdba.GaussDBASchema.GaussDBAColumn;
import sqlancer.gaussdba.GaussDBASchema.GaussDBATable;
import sqlancer.gaussdba.ast.GaussDBAExpression;
import sqlancer.gaussdba.ast.GaussDBASelect;
import sqlancer.gaussdba.gen.GaussDBAExpressionGenerator;

/**
 * Builds random SELECT shapes for EET.
 */
public final class GaussDBAEETQueryGenerator {

    private GaussDBAEETQueryGenerator() {
    }

    public static GaussDBAExpression generateEETQueryRandomShape(GaussDBAGlobalState state,
            GaussDBAExpressionGenerator gen, AbstractTables<GaussDBATable, GaussDBAColumn> tables) {
        Objects.requireNonNull(state);
        Objects.requireNonNull(tables);
        int mode = (int) Randomly.getNotCachedInteger(0, 4);
        switch (mode) {
        case 1:
            // UNION - 暂不支持，返回基础SELECT
            return buildBaseSelect(gen);
        case 2:
            // WITH (CTE) - 暂不支持，返回基础SELECT
            return buildBaseSelect(gen);
        case 3:
            // Derived table - 暂不支持，返回基础SELECT
            return buildBaseSelect(gen);
        case 0:
        default:
            return buildBaseSelect(gen);
        }
    }

    public static GaussDBASelect buildBaseSelect(GaussDBAExpressionGenerator gen) {
        GaussDBASelect select = gen.generateSelect();
        select.setFetchColumns(gen.generateFetchColumns(true));
        select.setJoinClauses(gen.getRandomJoinClauses());
        select.setFromList(gen.getTableRefs());
        select.setWhereClause(gen.generateBooleanExpression());
        select.setGroupByClause(List.of());
        select.setOrderByClauses(List.of());
        return select;
    }
}