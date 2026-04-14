package sqlancer.gaussdb.ast;

import sqlancer.common.ast.newast.Expression;
import sqlancer.gaussdb.GaussDBSchema.GaussDBColumn;

public interface GaussDBExpression extends Expression<GaussDBColumn> {

    default GaussDBConstant getExpectedValue() {
        throw new AssertionError("PQS not supported for this operator");
    }
}

