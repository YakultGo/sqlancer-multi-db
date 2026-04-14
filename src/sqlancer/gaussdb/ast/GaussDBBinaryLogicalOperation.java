package sqlancer.gaussdb.ast;

import sqlancer.Randomly;

public class GaussDBBinaryLogicalOperation implements GaussDBExpression {

    public enum GaussDBBinaryLogicalOperator {
        AND("AND"), OR("OR"), XOR("XOR");

        private final String textRepresentation;

        GaussDBBinaryLogicalOperator(String textRepresentation) {
            this.textRepresentation = textRepresentation;
        }

        public static GaussDBBinaryLogicalOperator getRandom() {
            return Randomly.fromOptions(values());
        }

        public String getTextRepresentation() {
            return textRepresentation;
        }
    }

    private final GaussDBExpression left;
    private final GaussDBExpression right;
    private final GaussDBBinaryLogicalOperator op;

    public GaussDBBinaryLogicalOperation(GaussDBExpression left, GaussDBExpression right,
            GaussDBBinaryLogicalOperator op) {
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

    public GaussDBBinaryLogicalOperator getOp() {
        return op;
    }
}

