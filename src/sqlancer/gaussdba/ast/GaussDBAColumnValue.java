package sqlancer.gaussdba.ast;

import sqlancer.gaussdba.GaussDBASchema.GaussDBAColumn;

public class GaussDBAColumnValue implements GaussDBAExpression {

    private final GaussDBAColumn c;
    private final GaussDBAConstant expectedValue;

    public GaussDBAColumnValue(GaussDBAColumn c, GaussDBAConstant expectedValue) {
        this.c = c;
        this.expectedValue = expectedValue;
    }

    @Override
    public GaussDBADataType getExpressionType() {
        return c.getType();
    }

    @Override
    public GaussDBAConstant getExpectedValue() {
        return expectedValue;
    }

    public static GaussDBAColumnValue create(GaussDBAColumn c, GaussDBAConstant expected) {
        return new GaussDBAColumnValue(c, expected);
    }

    public GaussDBAColumn getColumn() {
        return c;
    }
}