package sqlancer.gaussdbpg.ast;

public class GaussDBPGCastOperation implements GaussDBPGExpression {

    private final GaussDBPGExpression expr;
    private final GaussDBPGDataType type;

    public GaussDBPGCastOperation(GaussDBPGExpression expr, GaussDBPGDataType type) {
        this.expr = expr;
        this.type = type;
    }

    @Override
    public GaussDBPGConstant getExpectedValue() {
        GaussDBPGConstant expected = expr.getExpectedValue();
        if (expected == null) {
            return null;
        }
        return expected.cast(type);
    }

    @Override
    public GaussDBPGDataType getExpressionType() {
        return type;
    }

    public GaussDBPGExpression getExpr() {
        return expr;
    }

    public GaussDBPGDataType getType() {
        return type;
    }
}