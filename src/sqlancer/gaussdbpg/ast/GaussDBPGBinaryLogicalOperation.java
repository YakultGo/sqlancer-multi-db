package sqlancer.gaussdbpg.ast;

import sqlancer.IgnoreMeException;

public class GaussDBPGBinaryLogicalOperation implements GaussDBPGExpression {

    public enum GaussDBPGBinaryLogicalOperator {
        AND("AND"), OR("OR");

        private final String textRepresentation;

        GaussDBPGBinaryLogicalOperator(String textRepresentation) {
            this.textRepresentation = textRepresentation;
        }

        public String getTextRepresentation() {
            return textRepresentation;
        }

        public static GaussDBPGBinaryLogicalOperator getRandom() {
            return sqlancer.Randomly.fromOptions(values());
        }
    }

    private final GaussDBPGExpression left;
    private final GaussDBPGExpression right;
    private final GaussDBPGBinaryLogicalOperator op;

    public GaussDBPGBinaryLogicalOperation(GaussDBPGExpression left, GaussDBPGExpression right,
            GaussDBPGBinaryLogicalOperator op) {
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

        Boolean leftBool = getBooleanValue(leftExpected);
        Boolean rightBool = getBooleanValue(rightExpected);

        // PG three-valued logic
        switch (op) {
        case AND:
            // AND: TRUE AND NULL = NULL, FALSE AND NULL = FALSE, NULL AND NULL = NULL
            if (leftBool == null || rightBool == null) {
                if (leftBool != null && !leftBool) {
                    return GaussDBPGConstant.createBooleanConstant(false);
                }
                if (rightBool != null && !rightBool) {
                    return GaussDBPGConstant.createBooleanConstant(false);
                }
                return GaussDBPGConstant.createNullConstant();
            }
            return GaussDBPGConstant.createBooleanConstant(leftBool && rightBool);
        case OR:
            // OR: TRUE OR NULL = TRUE, FALSE OR NULL = NULL, NULL OR NULL = NULL
            if (leftBool == null || rightBool == null) {
                if (leftBool != null && leftBool) {
                    return GaussDBPGConstant.createBooleanConstant(true);
                }
                if (rightBool != null && rightBool) {
                    return GaussDBPGConstant.createBooleanConstant(true);
                }
                return GaussDBPGConstant.createNullConstant();
            }
            return GaussDBPGConstant.createBooleanConstant(leftBool || rightBool);
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

    public GaussDBPGBinaryLogicalOperator getOp() {
        return op;
    }
}