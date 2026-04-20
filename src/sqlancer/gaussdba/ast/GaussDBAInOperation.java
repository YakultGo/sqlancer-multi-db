package sqlancer.gaussdba.ast;

import java.util.ArrayList;
import java.util.List;

import sqlancer.Randomly;

public class GaussDBAInOperation implements GaussDBAExpression {

    private final GaussDBAExpression expr;
    private final List<GaussDBAExpression> listElements;
    private final boolean negated;

    public GaussDBAInOperation(GaussDBAExpression expr, List<GaussDBAExpression> listElements, boolean negated) {
        this.expr = expr;
        this.listElements = listElements;
        this.negated = negated;
    }

    public static GaussDBAInOperation create(GaussDBAExpression expr, Randomly r, int nrElements) {
        List<GaussDBAExpression> listElements = new ArrayList<>();
        for (int i = 0; i < nrElements; i++) {
            listElements.add(GaussDBAConstant.createRandomConstant(r));
        }
        return new GaussDBAInOperation(expr, listElements, false);
    }

    public static GaussDBAInOperation create(GaussDBAExpression expr, List<GaussDBAExpression> listElements,
            boolean negated) {
        return new GaussDBAInOperation(expr, listElements, negated);
    }

    public GaussDBAExpression getExpr() {
        return expr;
    }

    public List<GaussDBAExpression> getListElements() {
        return listElements;
    }

    public boolean isNegated() {
        return negated;
    }

    @Override
    public GaussDBAConstant getExpectedValue() {
        GaussDBAConstant exprVal = expr.getExpectedValue();
        if (exprVal == null) {
            return null;
        }

        // Oracle语义：空串被视为NULL
        // 如果expr是NULL（包括空串），IN返回NULL
        if (exprVal.isNull() || (exprVal.isString() && exprVal.asString().isEmpty())) {
            return GaussDBAConstant.createNullConstant();
        }

        boolean found = false;
        boolean hasNull = false;

        for (GaussDBAExpression element : listElements) {
            GaussDBAConstant elemVal = element.getExpectedValue();
            if (elemVal == null) {
                continue;
            }

            // Oracle语义：空串被视为NULL
            if (elemVal.isNull() || (elemVal.isString() && elemVal.asString().isEmpty())) {
                hasNull = true;
                continue;
            }

            GaussDBAConstant eqResult = exprVal.isEquals(elemVal);
            if (eqResult != null && !eqResult.isNull() && eqResult.isNumber() && eqResult.asNumber() == 1) {
                found = true;
            }
        }

        // Oracle语义：如果找到匹配，返回TRUE
        // 如果没找到且有NULL元素，返回NULL
        // 如果没找到且无NULL元素，返回FALSE

        if (found) {
            return GaussDBAConstant.createNumberConstant(negated ? 0 : 1);
        }

        if (hasNull) {
            return GaussDBAConstant.createNullConstant();
        }

        return GaussDBAConstant.createNumberConstant(negated ? 1 : 0);
    }

    @Override
    public GaussDBADataType getExpressionType() {
        return GaussDBADataType.NUMBER;
    }
}