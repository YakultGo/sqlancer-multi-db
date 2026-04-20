package sqlancer.gaussdba.ast;

import sqlancer.gaussdba.GaussDBASchema.GaussDBAColumn;

public class GaussDBAColumnReference implements GaussDBAExpression {

    private final GaussDBAColumn column;
    private final GaussDBAConstant expectedValue;

    public GaussDBAColumnReference(GaussDBAColumn column, GaussDBAConstant expectedValue) {
        this.column = column;
        this.expectedValue = expectedValue;
    }

    public static GaussDBAColumnReference create(GaussDBAColumn column, GaussDBAConstant expectedValue) {
        return new GaussDBAColumnReference(column, expectedValue);
    }

    public GaussDBAColumn getColumn() {
        return column;
    }

    @Override
    public GaussDBAConstant getExpectedValue() {
        return expectedValue;
    }

    @Override
    public GaussDBADataType getExpressionType() {
        return column.getType();
    }
}