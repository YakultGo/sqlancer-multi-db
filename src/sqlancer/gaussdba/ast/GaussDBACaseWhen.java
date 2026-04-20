package sqlancer.gaussdba.ast;

import java.util.ArrayList;
import java.util.List;

public class GaussDBACaseWhen implements GaussDBAExpression {

    private final List<GaussDBAExpression> conditions;
    private final List<GaussDBAExpression> thenExpressions;
    private final GaussDBAExpression elseExpression;

    public GaussDBACaseWhen(List<GaussDBAExpression> conditions, List<GaussDBAExpression> thenExpressions,
            GaussDBAExpression elseExpression) {
        this.conditions = conditions;
        this.thenExpressions = thenExpressions;
        this.elseExpression = elseExpression;
    }

    public static GaussDBACaseWhen create(GaussDBAExpression condition, GaussDBAExpression thenExpr,
            GaussDBAExpression elseExpr) {
        List<GaussDBAExpression> conditions = new ArrayList<>();
        conditions.add(condition);
        List<GaussDBAExpression> thenExpressions = new ArrayList<>();
        thenExpressions.add(thenExpr);
        return new GaussDBACaseWhen(conditions, thenExpressions, elseExpr);
    }

    public List<GaussDBAExpression> getConditions() {
        return conditions;
    }

    public List<GaussDBAExpression> getThenExpressions() {
        return thenExpressions;
    }

    public GaussDBAExpression getElseExpression() {
        return elseExpression;
    }

    @Override
    public GaussDBAConstant getExpectedValue() {
        // 遍历条件，找到第一个TRUE的
        for (int i = 0; i < conditions.size(); i++) {
            GaussDBAConstant condVal = conditions.get(i).getExpectedValue();
            if (condVal == null) {
                continue;
            }

            // Oracle语义：NULL不是TRUE也不是FALSE
            if (condVal.isNull()) {
                continue;
            }

            if (condVal.isNumber() && condVal.asNumber() == 1) {
                return thenExpressions.get(i).getExpectedValue();
            }
        }

        // 没有匹配的条件，返回ELSE
        if (elseExpression != null) {
            return elseExpression.getExpectedValue();
        }

        // 无ELSE返回NULL
        return GaussDBAConstant.createNullConstant();
    }

    @Override
    public GaussDBADataType getExpressionType() {
        // 返回第一个THEN表达式的类型
        if (!thenExpressions.isEmpty()) {
            return thenExpressions.get(0).getExpressionType();
        }
        return GaussDBADataType.NUMBER;
    }
}