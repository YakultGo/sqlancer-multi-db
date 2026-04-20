package sqlancer.gaussdba.ast;

import sqlancer.IgnoreMeException;
import sqlancer.Randomly;

public class GaussDBAUnaryPrefixOperation implements GaussDBAExpression {

    private final GaussDBAExpression expr;
    private final UnaryPrefixOperator op;

    public enum UnaryPrefixOperator {
        NOT("NOT"), UNARY_PLUS("+"), UNARY_MINUS("-");

        private final String text;

        UnaryPrefixOperator(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        public static UnaryPrefixOperator getRandom() {
            return Randomly.fromOptions(values());
        }
    }

    public GaussDBAUnaryPrefixOperation(GaussDBAExpression expr, UnaryPrefixOperator op) {
        this.expr = expr;
        this.op = op;
    }

    public GaussDBAExpression getExpr() {
        return expr;
    }

    public UnaryPrefixOperator getOp() {
        return op;
    }

    @Override
    public GaussDBAConstant getExpectedValue() {
        GaussDBAConstant expected = expr.getExpectedValue();
        if (expected == null) {
            return null;
        }

        switch (op) {
        case NOT:
            // Oracle语义：NOT NULL = NULL
            if (expected.isNull()) {
                return GaussDBAConstant.createNullConstant();
            }
            if (expected.isNumber()) {
                // NOT 1 = 0, NOT 0 = 1
                return GaussDBAConstant.createNumberConstant(expected.asNumber() == 1 ? 0 : 1);
            }
            // NOT on non-number types (VARCHAR2, DATE, TIMESTAMP) is not supported
            throw new IgnoreMeException();
        case UNARY_PLUS:
            if (expected.isNull()) {
                return GaussDBAConstant.createNullConstant();
            }
            if (expected.isNumber()) {
                return expected;
            }
            // + on non-number types is not supported
            throw new IgnoreMeException();
        case UNARY_MINUS:
            if (expected.isNull()) {
                return GaussDBAConstant.createNullConstant();
            }
            if (expected.isNumber()) {
                return GaussDBAConstant.createNumberConstant(-expected.asNumber());
            }
            // - on non-number types is not supported
            throw new IgnoreMeException();
        default:
            throw new AssertionError(op);
        }
    }

    @Override
    public GaussDBADataType getExpressionType() {
        return expr.getExpressionType();
    }
}