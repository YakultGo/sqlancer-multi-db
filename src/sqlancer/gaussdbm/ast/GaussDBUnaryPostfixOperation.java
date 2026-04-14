package sqlancer.gaussdbm.ast;

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

    @Override
    public GaussDBConstant getExpectedValue() {
        GaussDBConstant v = expr.getExpectedValue();
        boolean val;
        switch (op) {
        case IS_NULL:
            val = v.isNull();
            break;
        case IS_NOT_NULL:
            val = !v.isNull();
            break;
        default:
            throw new AssertionError(op);
        }
        return GaussDBConstant.createBooleanConstant(val);
    }
}

