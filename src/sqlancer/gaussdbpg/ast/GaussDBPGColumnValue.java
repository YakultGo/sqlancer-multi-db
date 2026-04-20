package sqlancer.gaussdbpg.ast;

import sqlancer.gaussdbpg.GaussDBPGSchema.GaussDBPGColumn;

public class GaussDBPGColumnValue implements GaussDBPGExpression {

    private final GaussDBPGColumn c;
    private final GaussDBPGConstant expectedValue;

    public GaussDBPGColumnValue(GaussDBPGColumn c, GaussDBPGConstant expectedValue) {
        this.c = c;
        this.expectedValue = expectedValue;
    }

    @Override
    public GaussDBPGDataType getExpressionType() {
        return c.getType();
    }

    @Override
    public GaussDBPGConstant getExpectedValue() {
        return expectedValue;
    }

    public static GaussDBPGColumnValue create(GaussDBPGColumn c, GaussDBPGConstant expected) {
        return new GaussDBPGColumnValue(c, expected);
    }

    public GaussDBPGColumn getColumn() {
        return c;
    }
}