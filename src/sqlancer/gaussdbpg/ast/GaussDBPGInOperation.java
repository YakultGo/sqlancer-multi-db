package sqlancer.gaussdbpg.ast;

import java.util.ArrayList;
import java.util.List;

import sqlancer.IgnoreMeException;
import sqlancer.Randomly;

public class GaussDBPGInOperation implements GaussDBPGExpression {

    private final GaussDBPGExpression expr;
    private final List<GaussDBPGExpression> listElements;
    private final boolean negated;

    public GaussDBPGInOperation(GaussDBPGExpression expr, List<GaussDBPGExpression> listElements, boolean negated) {
        this.expr = expr;
        this.listElements = listElements;
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
        if (exprExpected == null) {
            return null;
        }

        if (exprExpected.isNull()) {
            // NULL IN (...) returns NULL
            return GaussDBPGConstant.createNullConstant();
        }

        for (GaussDBPGExpression element : listElements) {
            GaussDBPGConstant elementExpected = element.getExpectedValue();
            if (elementExpected == null) {
                return null;
            }
            GaussDBPGConstant equalsResult = exprExpected.isEquals(elementExpected);
            if (equalsResult.isNull()) {
                // If any element is NULL and we haven't found a match yet, result is NULL
                continue;
            }
            Boolean eqBool = getBooleanValue(equalsResult);
            if (eqBool == null) {
                continue;
            }
            if (eqBool) {
                return GaussDBPGConstant.createBooleanConstant(!negated);
            }
        }

        // No match found and no NULL elements
        return GaussDBPGConstant.createBooleanConstant(negated);
    }

    @Override
    public GaussDBPGDataType getExpressionType() {
        return GaussDBPGDataType.BOOLEAN;
    }

    public GaussDBPGExpression getExpr() {
        return expr;
    }

    public List<GaussDBPGExpression> getListElements() {
        return listElements;
    }

    public boolean isNegated() {
        return negated;
    }

    public static GaussDBPGInOperation create(GaussDBPGExpression expr, int nrElements) {
        List<GaussDBPGExpression> elements = new ArrayList<>();
        for (int i = 0; i < nrElements; i++) {
            elements.add(GaussDBPGConstant.createRandomConstant(new Randomly()));
        }
        return new GaussDBPGInOperation(expr, elements, Randomly.getBoolean());
    }
}