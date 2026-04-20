package sqlancer.gaussdbpg.ast;

import sqlancer.IgnoreMeException;

public class GaussDBPGUnaryPrefixOperation implements GaussDBPGExpression {

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
            return sqlancer.Randomly.fromOptions(values());
        }
    }

    private final GaussDBPGExpression expr;
    private final UnaryPrefixOperator op;

    public GaussDBPGUnaryPrefixOperation(GaussDBPGExpression expr, UnaryPrefixOperator op) {
        this.expr = expr;
        this.op = op;
    }

    @Override
    public GaussDBPGConstant getExpectedValue() {
        GaussDBPGConstant expected = expr.getExpectedValue();
        if (expected == null) {
            return null;
        }

        switch (op) {
        case NOT:
            if (expected.isNull()) {
                return GaussDBPGConstant.createNullConstant();
            }
            // Need to cast to BOOLEAN before calling asBoolean()
            GaussDBPGConstant casted = expected.cast(GaussDBPGDataType.BOOLEAN);
            if (casted == null) {
                throw new IgnoreMeException();
            }
            return GaussDBPGConstant.createBooleanConstant(!casted.asBoolean());
        case UNARY_PLUS:
            if (expected.isNull()) {
                return GaussDBPGConstant.createNullConstant();
            }
            if (expected.isInt()) {
                return expected;
            }
            if (expected.isFloat()) {
                return expected;
            }
            return null;
        case UNARY_MINUS:
            if (expected.isNull()) {
                return GaussDBPGConstant.createNullConstant();
            }
            if (expected.isInt()) {
                return GaussDBPGConstant.createIntConstant(-expected.asInt());
            }
            if (expected.isFloat()) {
                return GaussDBPGConstant.createFloatConstant(-expected.asFloat());
            }
            return null;
        default:
            throw new AssertionError(op);
        }
    }

    @Override
    public GaussDBPGDataType getExpressionType() {
        switch (op) {
        case NOT:
            return GaussDBPGDataType.BOOLEAN;
        case UNARY_PLUS:
        case UNARY_MINUS:
            return expr.getExpressionType();
        default:
            throw new AssertionError(op);
        }
    }

    public GaussDBPGExpression getExpr() {
        return expr;
    }

    public UnaryPrefixOperator getOp() {
        return op;
    }
}