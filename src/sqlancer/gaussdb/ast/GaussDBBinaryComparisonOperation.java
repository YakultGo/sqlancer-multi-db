package sqlancer.gaussdb.ast;

public class GaussDBBinaryComparisonOperation implements GaussDBExpression {

    public enum BinaryComparisonOperator {
        EQUALS("="), NOT_EQUALS("<>"), GREATER(">"), GREATER_EQUALS(">="), SMALLER("<"), SMALLER_EQUALS("<=");

        private final String textRepr;

        BinaryComparisonOperator(String textRepr) {
            this.textRepr = textRepr;
        }

        public String getTextRepr() {
            return textRepr;
        }
    }

    private final GaussDBExpression left;
    private final GaussDBExpression right;
    private final BinaryComparisonOperator op;

    public GaussDBBinaryComparisonOperation(GaussDBExpression left, GaussDBExpression right,
            BinaryComparisonOperator op) {
        this.left = left;
        this.right = right;
        this.op = op;
    }

    public GaussDBExpression getLeft() {
        return left;
    }

    public GaussDBExpression getRight() {
        return right;
    }

    public BinaryComparisonOperator getOp() {
        return op;
    }
}

