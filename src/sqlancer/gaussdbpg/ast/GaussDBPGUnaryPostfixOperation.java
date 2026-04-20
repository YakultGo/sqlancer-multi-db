package sqlancer.gaussdbpg.ast;

import sqlancer.IgnoreMeException;

public class GaussDBPGUnaryPostfixOperation implements GaussDBPGExpression {

    public enum UnaryPostfixOperator {
        IS_NULL("IS NULL"), IS_NOT_NULL("IS NOT NULL"), IS_TRUE("IS TRUE"), IS_FALSE("IS FALSE");

        private final String text;

        UnaryPostfixOperator(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        public static UnaryPostfixOperator getRandom() {
            return sqlancer.Randomly.fromOptions(values());
        }
    }

    private final GaussDBPGExpression expr;
    private final UnaryPostfixOperator op;

    public GaussDBPGUnaryPostfixOperation(GaussDBPGExpression expr, UnaryPostfixOperator op) {
        this.expr = expr;
        this.op = op;
    }

    // Helper method to safely get boolean value from a constant
    private Boolean getBooleanValue(GaussDBPGConstant constant) {
        if (constant == null || constant.isNull()) {
            return null;
        }
        GaussDBPGConstant casted = constant.cast(GaussDBPGDataType.BOOLEAN);
        if (casted == null) {
            throw new IgnoreMeException();
        }
        return casted.asBoolean();
    }

    @Override
    public GaussDBPGConstant getExpectedValue() {
        GaussDBPGConstant expected = expr.getExpectedValue();
        if (expected == null) {
            return null;
        }

        switch (op) {
        case IS_NULL:
            // PG semantics: empty string is NOT NULL
            return GaussDBPGConstant.createBooleanConstant(expected.isNull());
        case IS_NOT_NULL:
            return GaussDBPGConstant.createBooleanConstant(!expected.isNull());
        case IS_TRUE:
            if (expected.isNull()) {
                return GaussDBPGConstant.createBooleanConstant(false);
            }
            Boolean boolVal = getBooleanValue(expected);
            if (boolVal == null) {
                return GaussDBPGConstant.createBooleanConstant(false);
            }
            return GaussDBPGConstant.createBooleanConstant(boolVal);
        case IS_FALSE:
            if (expected.isNull()) {
                return GaussDBPGConstant.createBooleanConstant(false);
            }
            Boolean boolVal2 = getBooleanValue(expected);
            if (boolVal2 == null) {
                return GaussDBPGConstant.createBooleanConstant(false);
            }
            return GaussDBPGConstant.createBooleanConstant(!boolVal2);
        default:
            throw new AssertionError(op);
        }
    }

    public static GaussDBPGUnaryPostfixOperation create(GaussDBPGExpression expr, UnaryPostfixOperator op) {
        return new GaussDBPGUnaryPostfixOperation(expr, op);
    }

    @Override
    public GaussDBPGDataType getExpressionType() {
        return GaussDBPGDataType.BOOLEAN;
    }

    public GaussDBPGExpression getExpr() {
        return expr;
    }

    public UnaryPostfixOperator getOp() {
        return op;
    }
}