package sqlancer.gaussdba.ast;

import sqlancer.common.ast.newast.Expression;
import sqlancer.gaussdba.GaussDBASchema.GaussDBAColumn;

public interface GaussDBAExpression extends Expression<GaussDBAColumn> {

    default GaussDBAConstant getExpectedValue() {
        throw new AssertionError("PQS not supported for this operator");
    }

    GaussDBADataType getExpressionType();
}