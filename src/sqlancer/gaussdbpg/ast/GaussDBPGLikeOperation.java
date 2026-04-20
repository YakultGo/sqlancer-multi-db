package sqlancer.gaussdbpg.ast;

public class GaussDBPGLikeOperation implements GaussDBPGExpression {

    private final GaussDBPGExpression left;
    private final GaussDBPGExpression right;
    private final boolean negated;

    public GaussDBPGLikeOperation(GaussDBPGExpression left, GaussDBPGExpression right) {
        this(left, right, false);
    }

    public GaussDBPGLikeOperation(GaussDBPGExpression left, GaussDBPGExpression right, boolean negated) {
        this.left = left;
        this.right = right;
        this.negated = negated;
    }

    @Override
    public GaussDBPGConstant getExpectedValue() {
        // LIKE operations are complex and depend on pattern matching
        // For PQS, we can't easily compute expected values for arbitrary patterns
        return null;
    }

    @Override
    public GaussDBPGDataType getExpressionType() {
        return GaussDBPGDataType.BOOLEAN;
    }

    public GaussDBPGExpression getLeft() {
        return left;
    }

    public GaussDBPGExpression getRight() {
        return right;
    }

    public boolean isNegated() {
        return negated;
    }
}