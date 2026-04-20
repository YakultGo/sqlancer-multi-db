package sqlancer.gaussdbpg.ast;

import sqlancer.IgnoreMeException;

public class GaussDBPGBinaryComparisonOperation implements GaussDBPGExpression {

    public enum GaussDBPGBinaryComparisonOperator {
        EQUALS("="), NOT_EQUALS("<>"), LESS_THAN("<"), GREATER_THAN(">"), LESS_THAN_OR_EQUALS("<="),
        GREATER_THAN_OR_EQUALS(">=");

        private final String textRepr;

        GaussDBPGBinaryComparisonOperator(String textRepr) {
            this.textRepr = textRepr;
        }

        public String getTextRepr() {
            return textRepr;
        }

        public static GaussDBPGBinaryComparisonOperator getRandom() {
            return sqlancer.Randomly.fromOptions(values());
        }
    }

    private final GaussDBPGExpression left;
    private final GaussDBPGExpression right;
    private final GaussDBPGBinaryComparisonOperator op;

    public GaussDBPGBinaryComparisonOperation(GaussDBPGExpression left, GaussDBPGExpression right,
            GaussDBPGBinaryComparisonOperator op) {
        this.left = left;
        this.right = right;
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
        GaussDBPGConstant leftExpected = left.getExpectedValue();
        GaussDBPGConstant rightExpected = right.getExpectedValue();
        if (leftExpected == null || rightExpected == null) {
            return null;
        }

        switch (op) {
        case EQUALS:
            return leftExpected.isEquals(rightExpected);
        case NOT_EQUALS:
            GaussDBPGConstant equalsResult = leftExpected.isEquals(rightExpected);
            if (equalsResult.isNull()) {
                return GaussDBPGConstant.createNullConstant();
            }
            Boolean eqBool = getBooleanValue(equalsResult);
            if (eqBool == null) {
                return GaussDBPGConstant.createNullConstant();
            }
            return GaussDBPGConstant.createBooleanConstant(!eqBool);
        case LESS_THAN:
            return leftExpected.isLessThan(rightExpected);
        case GREATER_THAN:
            return rightExpected.isLessThan(leftExpected);
        case LESS_THAN_OR_EQUALS:
            GaussDBPGConstant lessResult = leftExpected.isLessThan(rightExpected);
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
            GaussDBPGConstant eqResult = leftExpected.isEquals(rightExpected);
            if (eqResult.isNull()) {
                return GaussDBPGConstant.createNullConstant();
            }
            return eqResult;
        case GREATER_THAN_OR_EQUALS:
            GaussDBPGConstant greaterLessResult = rightExpected.isLessThan(leftExpected);
            if (greaterLessResult.isNull()) {
                return GaussDBPGConstant.createNullConstant();
            }
            Boolean greaterLessBool = getBooleanValue(greaterLessResult);
            if (greaterLessBool == null) {
                return GaussDBPGConstant.createNullConstant();
            }
            if (greaterLessBool) {
                return GaussDBPGConstant.createBooleanConstant(true);
            }
            GaussDBPGConstant greaterEqResult = leftExpected.isEquals(rightExpected);
            if (greaterEqResult.isNull()) {
                return GaussDBPGConstant.createNullConstant();
            }
            return greaterEqResult;
        default:
            throw new AssertionError(op);
        }
    }

    @Override
    public GaussDBPGDataType getExpressionType() {
        return GaussDBPGDataType.BOOLEAN;
    }

    public GaussDBPGExpression getLeft() {
        return left;
    }

    public GaussDBPGExpression getRight() {
        return right;
    }

    public GaussDBPGBinaryComparisonOperator getOp() {
        return op;
    }
}