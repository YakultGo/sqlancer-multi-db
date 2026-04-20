package sqlancer.gaussdba.ast;

import sqlancer.Randomly;

public class GaussDBABinaryLogicalOperation implements GaussDBAExpression {

    private final GaussDBAExpression left;
    private final GaussDBAExpression right;
    private GaussDBABinaryLogicalOperator op;

    public enum GaussDBABinaryLogicalOperator {
        AND("AND"), OR("OR");

        private final String textRepresentation;

        GaussDBABinaryLogicalOperator(String textRepresentation) {
            this.textRepresentation = textRepresentation;
        }

        public String getTextRepresentation() {
            return textRepresentation;
        }

        public static GaussDBABinaryLogicalOperator getRandom() {
            return Randomly.fromOptions(values());
        }
    }

    public GaussDBABinaryLogicalOperation(GaussDBAExpression left, GaussDBAExpression right,
            GaussDBABinaryLogicalOperator op) {
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

    public GaussDBABinaryLogicalOperator getOperator() {
        return op;
    }

    public void setOperator(GaussDBABinaryLogicalOperator op) {
        this.op = op;
    }

    public GaussDBABinaryLogicalOperator getOp() {
        return op;
    }

    @Override
    public GaussDBAConstant getExpectedValue() {
        GaussDBAConstant leftExpected = left.getExpectedValue();
        GaussDBAConstant rightExpected = right.getExpectedValue();

        if (leftExpected == null || rightExpected == null) {
            return null;
        }

        // Oracle语义三值逻辑
        // AND: FALSE AND anything = FALSE, TRUE AND NULL = NULL, NULL AND NULL = NULL
        // OR: TRUE OR anything = TRUE, FALSE OR NULL = NULL, NULL OR NULL = NULL

        switch (op) {
        case AND:
            return evaluateAnd(leftExpected, rightExpected);
        case OR:
            return evaluateOr(leftExpected, rightExpected);
        default:
            throw new AssertionError(op);
        }
    }

    private GaussDBAConstant evaluateAnd(GaussDBAConstant left, GaussDBAConstant right) {
        // Oracle语义：AND操作
        // 如果任一值为FALSE(0)，结果为FALSE
        // 如果任一值为NULL，且另一值不为FALSE，结果为NULL
        // 如果两个值都是TRUE(1)，结果为TRUE

        if (left.isNumber()) {
            long leftVal = left.asNumber();
            if (leftVal == 0) {
                // FALSE AND anything = FALSE
                return GaussDBAConstant.createNumberConstant(0);
            }
        }

        if (right.isNumber()) {
            long rightVal = right.asNumber();
            if (rightVal == 0) {
                // anything AND FALSE = FALSE
                return GaussDBAConstant.createNumberConstant(0);
            }
        }

        if (left.isNull() || right.isNull()) {
            // TRUE AND NULL = NULL, NULL AND TRUE = NULL, NULL AND NULL = NULL
            return GaussDBAConstant.createNullConstant();
        }

        if (left.isNumber() && right.isNumber()) {
            // TRUE AND TRUE = TRUE
            return GaussDBAConstant.createNumberConstant(1);
        }

        return GaussDBAConstant.createNullConstant();
    }

    private GaussDBAConstant evaluateOr(GaussDBAConstant left, GaussDBAConstant right) {
        // Oracle语义：OR操作
        // 如果任一值为TRUE(1)，结果为TRUE
        // 如果任一值为NULL，且另一值不为TRUE，结果为NULL
        // 如果两个值都是FALSE(0)，结果为FALSE

        if (left.isNumber()) {
            long leftVal = left.asNumber();
            if (leftVal == 1) {
                // TRUE OR anything = TRUE
                return GaussDBAConstant.createNumberConstant(1);
            }
        }

        if (right.isNumber()) {
            long rightVal = right.asNumber();
            if (rightVal == 1) {
                // anything OR TRUE = TRUE
                return GaussDBAConstant.createNumberConstant(1);
            }
        }

        if (left.isNull() || right.isNull()) {
            // FALSE OR NULL = NULL, NULL OR FALSE = NULL, NULL OR NULL = NULL
            return GaussDBAConstant.createNullConstant();
        }

        if (left.isNumber() && right.isNumber()) {
            // FALSE OR FALSE = FALSE
            return GaussDBAConstant.createNumberConstant(0);
        }

        return GaussDBAConstant.createNullConstant();
    }

    @Override
    public GaussDBADataType getExpressionType() {
        return GaussDBADataType.NUMBER;
    }
}