package sqlancer.gaussdb.ast;

public class GaussDBBetweenOperation implements GaussDBExpression {

    private final GaussDBExpression expr;
    private final GaussDBExpression left;
    private final GaussDBExpression right;
    private final boolean isNegated;

    public GaussDBBetweenOperation(GaussDBExpression expr, GaussDBExpression left, GaussDBExpression right,
            boolean isNegated) {
        this.expr = expr;
        this.left = left;
        this.right = right;
        this.isNegated = isNegated;
    }

    public GaussDBExpression getExpr() {
        return expr;
    }

    public GaussDBExpression getLeft() {
        return left;
    }

    public GaussDBExpression getRight() {
        return right;
    }

    public boolean isNegated() {
        return isNegated;
    }
}

