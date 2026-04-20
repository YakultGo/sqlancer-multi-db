package sqlancer.gaussdbpg.ast;

import java.util.ArrayList;
import java.util.List;

import sqlancer.IgnoreMeException;

public class GaussDBPGCaseWhen implements GaussDBPGExpression {

    private final List<GaussDBPGExpression> conditions;
    private final List<GaussDBPGExpression> thenExpressions;
    private final GaussDBPGExpression elseExpression;

    public GaussDBPGCaseWhen(GaussDBPGExpression condition, GaussDBPGExpression thenExpr,
            GaussDBPGExpression elseExpr) {
        this.conditions = new ArrayList<>();
        this.conditions.add(condition);
        this.thenExpressions = new ArrayList<>();
        this.thenExpressions.add(thenExpr);
        this.elseExpression = elseExpr;
    }

    public GaussDBPGCaseWhen(List<GaussDBPGExpression> conditions, List<GaussDBPGExpression> thenExpressions,
            GaussDBPGExpression elseExpression) {
        this.conditions = conditions;
        this.thenExpressions = thenExpressions;
        this.elseExpression = elseExpression;
    }

    @Override
    public GaussDBPGConstant getExpectedValue() {
        // For PQS, evaluate the first matching condition
        for (int i = 0; i < conditions.size(); i++) {
            GaussDBPGConstant condExpected = conditions.get(i).getExpectedValue();
            if (condExpected == null) {
                return null;
            }
            if (!condExpected.isNull()) {
                GaussDBPGConstant casted = condExpected.cast(GaussDBPGDataType.BOOLEAN);
                if (casted == null) {
                    throw new IgnoreMeException();
                }
                if (casted.asBoolean()) {
                    return thenExpressions.get(i).getExpectedValue();
                }
            }
        }
        return elseExpression != null ? elseExpression.getExpectedValue() : null;
    }

    @Override
    public GaussDBPGDataType getExpressionType() {
        // Return type is based on then/else expressions
        if (!thenExpressions.isEmpty()) {
            return thenExpressions.get(0).getExpressionType();
        }
        return elseExpression != null ? elseExpression.getExpressionType() : null;
    }

    public List<GaussDBPGExpression> getConditions() {
        return conditions;
    }

    public List<GaussDBPGExpression> getThenExpressions() {
        return thenExpressions;
    }

    public GaussDBPGExpression getElseExpression() {
        return elseExpression;
    }
}