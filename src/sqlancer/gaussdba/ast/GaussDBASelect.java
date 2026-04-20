package sqlancer.gaussdba.ast;

import java.util.List;
import java.util.stream.Collectors;

import sqlancer.common.ast.SelectBase;
import sqlancer.common.ast.newast.Select;
import sqlancer.gaussdba.GaussDBASchema.GaussDBAColumn;
import sqlancer.gaussdba.GaussDBASchema.GaussDBATable;
import sqlancer.gaussdba.GaussDBAToStringVisitor;

public class GaussDBASelect extends SelectBase<GaussDBAExpression>
        implements GaussDBAExpression, Select<GaussDBAJoin, GaussDBAExpression, GaussDBATable, GaussDBAColumn> {

    private GaussDBASelectType selectType = GaussDBASelectType.ALL;

    public enum GaussDBASelectType {
        ALL, DISTINCT
    }

    @Override
    public GaussDBAConstant getExpectedValue() {
        return null;
    }

    @Override
    public GaussDBADataType getExpressionType() {
        return null; // SELECT returns multiple columns
    }

    // Select interface methods for Join handling (J type, not E)
    @Override
    public void setJoinClauses(List<GaussDBAJoin> joinStatements) {
        List<GaussDBAExpression> expressions = joinStatements.stream().map(e -> (GaussDBAExpression) e)
                .collect(Collectors.toList());
        setJoinList(expressions);
    }

    @Override
    public List<GaussDBAJoin> getJoinClauses() {
        return getJoinList().stream().map(e -> (GaussDBAJoin) e).collect(Collectors.toList());
    }

    @Override
    public String asString() {
        return GaussDBAToStringVisitor.asString(this);
    }

    public GaussDBASelectType getSelectType() {
        return selectType;
    }

    public void setSelectType(GaussDBASelectType selectType) {
        this.selectType = selectType;
    }
}