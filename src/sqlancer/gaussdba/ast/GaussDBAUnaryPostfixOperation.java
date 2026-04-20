package sqlancer.gaussdba.ast;

import sqlancer.Randomly;

public class GaussDBAUnaryPostfixOperation implements GaussDBAExpression {

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
            return Randomly.fromOptions(values());
        }
    }

    private final GaussDBAExpression expr;
    private final UnaryPostfixOperator op;

    public GaussDBAUnaryPostfixOperation(GaussDBAExpression expr, UnaryPostfixOperator op) {
        this.expr = expr;
        this.op = op;
    }

    @Override
    public GaussDBAConstant getExpectedValue() {
        GaussDBAConstant expected = expr.getExpectedValue();
        if (expected == null) {
            return null;
        }

        // Oracle语义关键差异：空字符串被视为NULL
        switch (op) {
        case IS_NULL:
            // Oracle语义：空字符串 IS NULL = TRUE
            if (expected.isString() && expected.asString().isEmpty()) {
                return GaussDBAConstant.createNumberConstant(1);  // TRUE
            }
            return GaussDBAConstant.createNumberConstant(expected.isNull() ? 1 : 0);
        case IS_NOT_NULL:
            // Oracle语义：空字符串 IS NOT NULL = FALSE
            if (expected.isString() && expected.asString().isEmpty()) {
                return GaussDBAConstant.createNumberConstant(0);  // FALSE
            }
            return GaussDBAConstant.createNumberConstant(!expected.isNull() ? 1 : 0);
        case IS_TRUE:
            if (expected.isNull()) {
                return GaussDBAConstant.createNumberConstant(0);  // NULL IS TRUE = FALSE
            }
            // 空字符串被视为NULL，所以空串 IS TRUE = FALSE
            if (expected.isString() && expected.asString().isEmpty()) {
                return GaussDBAConstant.createNumberConstant(0);
            }
            if (expected.isNumber()) {
                return GaussDBAConstant.createNumberConstant(expected.asNumber() == 1 ? 1 : 0);
            }
            return GaussDBAConstant.createNumberConstant(0);
        case IS_FALSE:
            if (expected.isNull()) {
                return GaussDBAConstant.createNumberConstant(0);  // NULL IS FALSE = FALSE
            }
            // 空字符串被视为NULL，所以空串 IS FALSE = FALSE
            if (expected.isString() && expected.asString().isEmpty()) {
                return GaussDBAConstant.createNumberConstant(0);
            }
            if (expected.isNumber()) {
                return GaussDBAConstant.createNumberConstant(expected.asNumber() == 0 ? 1 : 0);
            }
            return GaussDBAConstant.createNumberConstant(0);
        default:
            throw new AssertionError(op);
        }
    }

    @Override
    public GaussDBADataType getExpressionType() {
        return GaussDBADataType.NUMBER;
    }

    public GaussDBAExpression getExpr() {
        return expr;
    }

    public UnaryPostfixOperator getOp() {
        return op;
    }

    public static GaussDBAUnaryPostfixOperation create(GaussDBAExpression expr, UnaryPostfixOperator op) {
        return new GaussDBAUnaryPostfixOperation(expr, op);
    }
}