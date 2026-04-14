package sqlancer.gaussdb.ast;

import sqlancer.common.ast.newast.Join;
import sqlancer.gaussdb.GaussDBSchema.GaussDBColumn;
import sqlancer.gaussdb.GaussDBSchema.GaussDBTable;

public class GaussDBJoin implements GaussDBExpression, Join<GaussDBExpression, GaussDBTable, GaussDBColumn> {

    public enum JoinType {
        INNER, LEFT, RIGHT, CROSS;
    }

    private final GaussDBExpression tableReference;
    private GaussDBExpression onCondition;
    private final JoinType joinType;

    public GaussDBJoin(GaussDBExpression tableReference, GaussDBExpression onCondition, JoinType joinType) {
        this.tableReference = tableReference;
        this.onCondition = onCondition;
        this.joinType = joinType;
    }

    public GaussDBExpression getTableReference() {
        return tableReference;
    }

    public GaussDBExpression getOnCondition() {
        return onCondition;
    }

    public JoinType getJoinType() {
        return joinType;
    }

    @Override
    public void setOnClause(GaussDBExpression onClause) {
        this.onCondition = onClause;
    }
}

