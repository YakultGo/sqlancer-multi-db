package sqlancer.gaussdbpg.ast;

import sqlancer.common.ast.newast.Expression;
import sqlancer.gaussdbpg.GaussDBPGSchema.GaussDBPGColumn;

public interface GaussDBPGExpression extends Expression<GaussDBPGColumn> {

    default GaussDBPGConstant getExpectedValue() {
        throw new AssertionError("PQS not supported for this operator");
    }

    GaussDBPGDataType getExpressionType();
}