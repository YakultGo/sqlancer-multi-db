package sqlancer.gaussdba.ast;

public class GaussDBABetweenOperation implements GaussDBAExpression {

    private final GaussDBAExpression expr;
    private final GaussDBAExpression left;
    private final GaussDBAExpression right;
    private final boolean negated;

    public GaussDBABetweenOperation(GaussDBAExpression expr, GaussDBAExpression left, GaussDBAExpression right,
            boolean negated) {
        this.expr = expr;
        this.left = left;
        this.right = right;
        this.negated = negated;
    }

    public GaussDBAExpression getExpr() {
        return expr;
    }

    public GaussDBAExpression getLeft() {
        return left;
    }

    public GaussDBAExpression getRight() {
        return right;
    }

    public boolean isNegated() {
        return negated;
    }

    @Override
    public GaussDBAConstant getExpectedValue() {
        GaussDBAConstant exprVal = expr.getExpectedValue();
        GaussDBAConstant leftVal = left.getExpectedValue();
        GaussDBAConstant rightVal = right.getExpectedValue();

        if (exprVal == null || leftVal == null || rightVal == null) {
            return null;
        }

        // Oracle语义：空字符串被视为NULL
        // 如果任一值为NULL（包括空字符串），整个BETWEEN返回NULL
        if (exprVal.isNull() || leftVal.isNull() || rightVal.isNull()) {
            return GaussDBAConstant.createNullConstant();
        }

        // 检查是否是空字符串（Oracle语义下被视为NULL）
        if (exprVal.isString() && exprVal.asString().isEmpty()) {
            return GaussDBAConstant.createNullConstant();
        }
        if (leftVal.isString() && leftVal.asString().isEmpty()) {
            return GaussDBAConstant.createNullConstant();
        }
        if (rightVal.isString() && rightVal.asString().isEmpty()) {
            return GaussDBAConstant.createNullConstant();
        }

        // 比较操作
        GaussDBAConstant greaterOrEqualLeft = exprVal.isEquals(leftVal);
        if (greaterOrEqualLeft == null || greaterOrEqualLeft.isNull()) {
            return GaussDBAConstant.createNullConstant();
        }

        boolean greaterOrEqual = false;
        if (greaterOrEqualLeft.isNumber() && greaterOrEqualLeft.asNumber() == 1) {
            greaterOrEqual = true;
        } else {
            GaussDBAConstant lessThanLeft = leftVal.isLessThan(exprVal);
            if (lessThanLeft == null || lessThanLeft.isNull()) {
                return GaussDBAConstant.createNullConstant();
            }
            if (lessThanLeft.isNumber() && lessThanLeft.asNumber() == 1) {
                greaterOrEqual = true;
            }
        }

        GaussDBAConstant lessOrEqualRight = exprVal.isEquals(rightVal);
        if (lessOrEqualRight == null || lessOrEqualRight.isNull()) {
            return GaussDBAConstant.createNullConstant();
        }

        boolean lessOrEqual = false;
        if (lessOrEqualRight.isNumber() && lessOrEqualRight.asNumber() == 1) {
            lessOrEqual = true;
        } else {
            GaussDBAConstant lessThanRight = exprVal.isLessThan(rightVal);
            if (lessThanRight == null || lessThanRight.isNull()) {
                return GaussDBAConstant.createNullConstant();
            }
            if (lessThanRight.isNumber() && lessThanRight.asNumber() == 1) {
                lessOrEqual = true;
            }
        }

        boolean result = greaterOrEqual && lessOrEqual;
        return GaussDBAConstant.createNumberConstant(negated ? (result ? 0 : 1) : (result ? 1 : 0));
    }

    @Override
    public GaussDBADataType getExpressionType() {
        return GaussDBADataType.NUMBER;
    }

    public static GaussDBABetweenOperation create(GaussDBAExpression expr, GaussDBAExpression left,
            GaussDBAExpression right, boolean negated) {
        return new GaussDBABetweenOperation(expr, left, right, negated);
    }
}