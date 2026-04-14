package sqlancer.gaussdb.ast;

import sqlancer.gaussdb.GaussDBSchema.GaussDBColumn;

public class GaussDBColumnReference implements GaussDBExpression {

    private final GaussDBColumn column;
    private final GaussDBConstant value;

    public GaussDBColumnReference(GaussDBColumn column, GaussDBConstant value) {
        this.column = column;
        this.value = value;
    }

    public static GaussDBColumnReference create(GaussDBColumn column, GaussDBConstant value) {
        return new GaussDBColumnReference(column, value);
    }

    public GaussDBColumn getColumn() {
        return column;
    }

    public GaussDBConstant getValue() {
        return value;
    }

    @Override
    public GaussDBConstant getExpectedValue() {
        return value;
    }
}

