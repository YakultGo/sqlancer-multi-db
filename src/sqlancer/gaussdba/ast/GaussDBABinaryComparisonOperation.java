package sqlancer.gaussdba.ast;

import sqlancer.Randomly;

public class GaussDBABinaryComparisonOperation implements GaussDBAExpression {

    private final GaussDBAExpression left;
    private final GaussDBAExpression right;
    private final GaussDBABinaryComparisonOperator op;

    public enum GaussDBABinaryComparisonOperator {
        EQUALS("="), NOT_EQUALS("<>"), LESS_THAN("<"), GREATER_THAN(">"), LESS_EQUALS("<="), GREATER_EQUALS(">=");

        private final String textRepr;

        GaussDBABinaryComparisonOperator(String textRepr) {
            this.textRepr = textRepr;
        }

        public String getTextRepr() {
            return textRepr;
        }

        public static GaussDBABinaryComparisonOperator getRandom() {
            return Randomly.fromOptions(values());
        }
    }

    public GaussDBABinaryComparisonOperation(GaussDBAExpression left, GaussDBAExpression right,
            GaussDBABinaryComparisonOperator op) {
        this.left = left;
        this.right = right;
        this.op = op;
    }

    public GaussDBAExpression getLeft() {
        return left;
    }

    public GaussDBAExpression getRight() {
        return right;
    }

    public GaussDBABinaryComparisonOperator getOp() {
        return op;
    }

    @Override
    public GaussDBAConstant getExpectedValue() {
        GaussDBAConstant leftExpected = left.getExpectedValue();
        GaussDBAConstant rightExpected = right.getExpectedValue();

        if (leftExpected == null || rightExpected == null) {
            return null;
        }

        // Oracle语义：空串被视为NULL
        if (leftExpected.isNull() || rightExpected.isNull()) {
            // Oracle语义：NULL与任何值比较都返回NULL
            return GaussDBAConstant.createNullConstant();
        }

        switch (op) {
        case EQUALS:
            return leftExpected.isEquals(rightExpected);
        case NOT_EQUALS:
            GaussDBAConstant eqResult = leftExpected.isEquals(rightExpected);
            if (eqResult.isNull()) {
                return GaussDBAConstant.createNullConstant();
            }
            return GaussDBAConstant.createBooleanConstant(!eqResult.asBoolean());
        case LESS_THAN:
            return leftExpected.isLessThan(rightExpected);
        case GREATER_THAN:
            return rightExpected.isLessThan(leftExpected);
        case LESS_EQUALS:
            GaussDBAConstant ltResult = leftExpected.isLessThan(rightExpected);
            GaussDBAConstant eqResult2 = leftExpected.isEquals(rightExpected);
            if (ltResult.isNull() || eqResult2.isNull()) {
                return GaussDBAConstant.createNullConstant();
            }
            return GaussDBAConstant.createBooleanConstant(ltResult.asBoolean() || eqResult2.asBoolean());
        case GREATER_EQUALS:
            GaussDBAConstant gtResult = rightExpected.isLessThan(leftExpected);
            GaussDBAConstant eqResult3 = leftExpected.isEquals(rightExpected);
            if (gtResult.isNull() || eqResult3.isNull()) {
                return GaussDBAConstant.createNullConstant();
            }
            return GaussDBAConstant.createBooleanConstant(gtResult.asBoolean() || eqResult3.asBoolean());
        default:
            throw new AssertionError(op);
        }
    }

    @Override
    public GaussDBADataType getExpressionType() {
        // A模式无BOOLEAN类型，比较结果用NUMBER(1)表示
        return GaussDBADataType.NUMBER;
    }
}