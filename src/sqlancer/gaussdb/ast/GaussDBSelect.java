package sqlancer.gaussdb.ast;

import sqlancer.common.ast.SelectBase;
import sqlancer.common.ast.newast.Select;
import sqlancer.gaussdb.GaussDBSchema.GaussDBColumn;
import sqlancer.gaussdb.GaussDBSchema.GaussDBTable;
import sqlancer.gaussdb.GaussDBToStringVisitor;

public class GaussDBSelect extends SelectBase<GaussDBExpression>
        implements GaussDBExpression, Select<GaussDBJoin, GaussDBExpression, GaussDBTable, GaussDBColumn> {

    @Override
    public GaussDBConstant getExpectedValue() {
        return null;
    }

    @Override
    public void setJoinClauses(java.util.List<GaussDBJoin> joinStatements) {
        java.util.List<GaussDBExpression> expressions = joinStatements.stream().map(e -> (GaussDBExpression) e)
                .collect(java.util.stream.Collectors.toList());
        setJoinList(expressions);
    }

    @Override
    public java.util.List<GaussDBJoin> getJoinClauses() {
        return getJoinList().stream().map(e -> (GaussDBJoin) e).collect(java.util.stream.Collectors.toList());
    }

    @Override
    public String asString() {
        return GaussDBToStringVisitor.asString(this);
    }
}

