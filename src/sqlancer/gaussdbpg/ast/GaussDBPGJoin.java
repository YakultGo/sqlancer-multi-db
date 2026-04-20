package sqlancer.gaussdbpg.ast;

import sqlancer.Randomly;
import sqlancer.common.ast.newast.Join;
import sqlancer.gaussdbpg.GaussDBPGSchema.GaussDBPGColumn;
import sqlancer.gaussdbpg.GaussDBPGSchema.GaussDBPGTable;

public class GaussDBPGJoin implements GaussDBPGExpression, Join<GaussDBPGExpression, GaussDBPGTable, GaussDBPGColumn> {

    public enum GaussDBPGJoinType {
        INNER, LEFT, RIGHT, CROSS;

        public static GaussDBPGJoinType getRandom() {
            return Randomly.fromOptions(values());
        }
    }

    private final GaussDBPGExpression tableReference;
    private GaussDBPGExpression onCondition;
    private GaussDBPGJoinType joinType;

    public GaussDBPGJoin(GaussDBPGExpression tableReference, GaussDBPGExpression onCondition,
            GaussDBPGJoinType joinType) {
        this.tableReference = tableReference;
        this.onCondition = onCondition;
        this.joinType = joinType;
    }

    @Override
    public GaussDBPGConstant getExpectedValue() {
        return null;
    }

    @Override
    public GaussDBPGDataType getExpressionType() {
        return null;
    }

    public GaussDBPGExpression getTableReference() {
        return tableReference;
    }

    public GaussDBPGExpression getOnCondition() {
        return onCondition;
    }

    public GaussDBPGJoinType getJoinType() {
        return joinType;
    }

    @Override
    public void setOnClause(GaussDBPGExpression onCondition) {
        this.onCondition = onCondition;
    }

    public void setJoinType(GaussDBPGJoinType joinType) {
        this.joinType = joinType;
    }
}