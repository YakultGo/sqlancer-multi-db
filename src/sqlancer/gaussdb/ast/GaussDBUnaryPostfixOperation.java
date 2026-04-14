package sqlancer.gaussdb.ast;

public class GaussDBUnaryPostfixOperation implements GaussDBExpression {

    public enum UnaryPostfixOperator {
        IS_NULL("IS NULL"), IS_NOT_NULL("IS NOT NULL");

        private final String text;

        UnaryPostfixOperator(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }
    }

    private final GaussDBExpression expr;
    private final UnaryPostfixOperator op;

    public GaussDBUnaryPostfixOperation(GaussDBExpression expr, UnaryPostfixOperator op) {
        this.expr = expr;
        this.op = op;
    }

    public GaussDBExpression getExpr() {
        return expr;
    }

    public UnaryPostfixOperator getOp() {
        return op;
    }
}

