package sqlancer.gaussdba.ast;

public class GaussDBACastOperation implements GaussDBAExpression {

    private final GaussDBAExpression expr;
    private final GaussDBADataType type;

    public GaussDBACastOperation(GaussDBAExpression expr, GaussDBADataType type) {
        this.expr = expr;
        this.type = type;
    }

    public GaussDBAExpression getExpr() {
        return expr;
    }

    public GaussDBADataType getType() {
        return type;
    }

    @Override
    public GaussDBAConstant getExpectedValue() {
        GaussDBAConstant expected = expr.getExpectedValue();
        if (expected == null) {
            return null;
        }

        // Oracle语义：空串被视为NULL，CAST(NULL AS type) = NULL
        if (expected.isNull()) {
            return GaussDBAConstant.createNullConstant();
        }

        // 空字符串转任何类型都是NULL
        if (expected.isString() && expected.asString().isEmpty()) {
            return GaussDBAConstant.createNullConstant();
        }

        return expected.cast(type);
    }

    @Override
    public GaussDBADataType getExpressionType() {
        return type;
    }

    public static GaussDBACastOperation create(GaussDBAExpression expr, GaussDBADataType type) {
        return new GaussDBACastOperation(expr, type);
    }
}