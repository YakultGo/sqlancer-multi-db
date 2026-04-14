package sqlancer.gaussdb.ast;

public class GaussDBUnaryPrefixOperation implements GaussDBExpression {

    public enum UnaryPrefixOperator {
        NOT("NOT");

        private final String text;

        UnaryPrefixOperator(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }
    }

    private final GaussDBExpression expr;
    private final UnaryPrefixOperator op;

    public GaussDBUnaryPrefixOperation(GaussDBExpression expr, UnaryPrefixOperator op) {
        this.expr = expr;
        this.op = op;
    }

    public GaussDBExpression getExpr() {
        return expr;
    }

    public UnaryPrefixOperator getOp() {
        return op;
    }
}

