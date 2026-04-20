package sqlancer.gaussdbpg.ast;

import sqlancer.common.ast.newast.ColumnReferenceNode;
import sqlancer.gaussdbpg.GaussDBPGSchema.GaussDBPGColumn;

public class GaussDBPGColumnReference extends ColumnReferenceNode<GaussDBPGExpression, GaussDBPGColumn>
        implements GaussDBPGExpression {

    private GaussDBPGConstant value;

    public GaussDBPGColumnReference(GaussDBPGColumn column, GaussDBPGConstant value) {
        super(column);
        this.value = value;
    }

    public static GaussDBPGColumnReference create(GaussDBPGColumn column, GaussDBPGConstant value) {
        return new GaussDBPGColumnReference(column, value);
    }

    @Override
    public GaussDBPGConstant getExpectedValue() {
        return value;
    }

    @Override
    public GaussDBPGDataType getExpressionType() {
        return getColumn().getType();
    }

    public GaussDBPGConstant getValue() {
        return value;
    }

    public void setValue(GaussDBPGConstant value) {
        this.value = value;
    }
}