package sqlancer.gaussdba.ast;

public class GaussDBALikeOperation implements GaussDBAExpression {

    private final GaussDBAExpression left;
    private final GaussDBAExpression right;
    private final boolean negated;

    public GaussDBALikeOperation(GaussDBAExpression left, GaussDBAExpression right, boolean negated) {
        this.left = left;
        this.right = right;
        this.negated = negated;
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
        GaussDBAConstant leftVal = left.getExpectedValue();
        GaussDBAConstant rightVal = right.getExpectedValue();

        if (leftVal == null || rightVal == null) {
            return null;
        }

        // Oracle语义：空串被视为NULL
        // 如果任一值为NULL（包括空串），LIKE返回NULL
        if (leftVal.isNull() || rightVal.isNull()) {
            return GaussDBAConstant.createNullConstant();
        }

        if (leftVal.isString() && leftVal.asString().isEmpty()) {
            return GaussDBAConstant.createNullConstant();
        }

        if (rightVal.isString() && rightVal.asString().isEmpty()) {
            return GaussDBAConstant.createNullConstant();
        }

        // LIKE匹配逻辑简化：假设所有LIKE都匹配（实际需要正则匹配）
        // 这里只是占位实现，实际LIKE语义需要更复杂处理
        return GaussDBAConstant.createNumberConstant(1);
    }

    @Override
    public GaussDBADataType getExpressionType() {
        return GaussDBADataType.NUMBER;
    }

    public static GaussDBALikeOperation create(GaussDBAExpression left, GaussDBAExpression right, boolean negated) {
        return new GaussDBALikeOperation(left, right, negated);
    }
}