package sqlancer.gaussdbpg.ast;

import sqlancer.common.ast.SelectBase;
import sqlancer.common.ast.newast.Select;
import sqlancer.gaussdbpg.GaussDBPGSchema.GaussDBPGColumn;
import sqlancer.gaussdbpg.GaussDBPGSchema.GaussDBPGTable;
import sqlancer.gaussdbpg.GaussDBPGToStringVisitor;

public class GaussDBPGSelect extends SelectBase<GaussDBPGExpression>
        implements GaussDBPGExpression, Select<GaussDBPGJoin, GaussDBPGExpression, GaussDBPGTable, GaussDBPGColumn> {

    private GaussDBPGSelectType selectType = GaussDBPGSelectType.ALL;

    public enum GaussDBPGSelectType {
        ALL, DISTINCT
    }

    @Override
    public GaussDBPGConstant getExpectedValue() {
        return null;
    }

    @Override
    public GaussDBPGDataType getExpressionType() {
        return null; // SELECT returns multiple columns
    }

    @Override
    public void setJoinClauses(java.util.List<GaussDBPGJoin> joinStatements) {
        java.util.List<GaussDBPGExpression> expressions = joinStatements.stream().map(e -> (GaussDBPGExpression) e)
                .collect(java.util.stream.Collectors.toList());
        setJoinList(expressions);
    }

    @Override
    public java.util.List<GaussDBPGJoin> getJoinClauses() {
        return getJoinList().stream().map(e -> (GaussDBPGJoin) e).collect(java.util.stream.Collectors.toList());
    }

    @Override
    public String asString() {
        return GaussDBPGToStringVisitor.asString(this);
    }

    public GaussDBPGSelectType getSelectType() {
        return selectType;
    }

    public void setSelectType(GaussDBPGSelectType selectType) {
        this.selectType = selectType;
    }
}