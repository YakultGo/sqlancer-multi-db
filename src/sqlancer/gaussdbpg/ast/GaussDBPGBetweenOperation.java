package sqlancer.gaussdbpg.ast;

import sqlancer.IgnoreMeException;

public class GaussDBPGBetweenOperation implements GaussDBPGExpression {

    private final GaussDBPGExpression expr;
    private final GaussDBPGExpression left;
    private final GaussDBPGExpression right;
    private final boolean negated;

    public GaussDBPGBetweenOperation(GaussDBPGExpression expr, GaussDBPGExpression left, GaussDBPGExpression right,
            boolean negated) {
        this.expr = expr;
        this.left = left;
        this.right = right;
        this.negated = negated;
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
        GaussDBPGConstant exprExpected = expr.getExpectedValue();
        GaussDBPGConstant leftExpected = left.getExpectedValue();
        GaussDBPGConstant rightExpected = right.getExpectedValue();

        if (exprExpected == null || leftExpected == null || rightExpected == null) {
            return null;
        }

        if (exprExpected.isNull() || leftExpected.isNull() || rightExpected.isNull()) {
            return GaussDBPGConstant.createNullConstant();
        }

        // expr BETWEEN left AND right is equivalent to expr >= left AND expr <= right
        GaussDBPGConstant greaterOrEqualLeft = computeGreaterOrEqual(exprExpected, leftExpected);
        GaussDBPGConstant lessOrEqualRight = computeLessOrEqual(exprExpected, rightExpected);

        if (greaterOrEqualLeft.isNull() || lessOrEqualRight.isNull()) {
            return GaussDBPGConstant.createNullConstant();
        }

        Boolean leftBool = getBooleanValue(greaterOrEqualLeft);
        Boolean rightBool = getBooleanValue(lessOrEqualRight);
        if (leftBool == null || rightBool == null) {
            return GaussDBPGConstant.createNullConstant();
        }

        boolean result = leftBool && rightBool;

        if (negated) {
            return GaussDBPGConstant.createBooleanConstant(!result);
        }
        return GaussDBPGConstant.createBooleanConstant(result);
    }

    private GaussDBPGConstant computeGreaterOrEqual(GaussDBPGConstant left, GaussDBPGConstant right) {
        GaussDBPGConstant lessResult = right.isLessThan(left);
        if (lessResult.isNull()) {
            return GaussDBPGConstant.createNullConstant();
        }
        Boolean lessBool = getBooleanValue(lessResult);
        if (lessBool == null) {
            return GaussDBPGConstant.createNullConstant();
        }
        if (lessBool) {
            return GaussDBPGConstant.createBooleanConstant(false);
        }
        GaussDBPGConstant eqResult = left.isEquals(right);
        return eqResult;
    }

    private GaussDBPGConstant computeLessOrEqual(GaussDBPGConstant left, GaussDBPGConstant right) {
        GaussDBPGConstant lessResult = left.isLessThan(right);
        if (lessResult.isNull()) {
            return GaussDBPGConstant.createNullConstant();
        }
        Boolean lessBool = getBooleanValue(lessResult);
        if (lessBool == null) {
            return GaussDBPGConstant.createNullConstant();
        }
        if (lessBool) {
            return GaussDBPGConstant.createBooleanConstant(true);
        }
        GaussDBPGConstant eqResult = left.isEquals(right);
        return eqResult;
    }

    @Override
    public GaussDBPGDataType getExpressionType() {
        return GaussDBPGDataType.BOOLEAN;
    }

    public GaussDBPGExpression getExpr() {
        return expr;
    }

    public GaussDBPGExpression getLeft() {
        return left;
    }

    public GaussDBPGExpression getRight() {
        return right;
    }

    public boolean isNegated() {
        return negated;
    }
}