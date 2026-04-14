package sqlancer.mysql.ast;

public class MySQLTypeof implements MySQLExpression {

    private MySQLExpression innerExpr;

    public MySQLTypeof(MySQLExpression expr) {
        this.innerExpr = expr;
    }

    public MySQLExpression getInnerExpression() {
        return innerExpr;
    }

    @Override
    public MySQLConstant getExpectedValue() {
        // MySQL doesn't have typeof(), so we'll return a type name as string
        if (innerExpr == null) {
            return MySQLConstant.createStringConstant("NULL");
        }

        MySQLConstant value = innerExpr.getExpectedValue();
        if (value == null) {
            return MySQLConstant.createStringConstant("UNKNOWN");
        }

        String type;
        if (value.isNull()) {
            type = "NULL";
        } else if (value.isInt()) {
            type = "INT";
        } else if (value.isString()) {
            type = "TEXT";
        } else {
            type = "BLOB";
        }

        return MySQLConstant.createStringConstant(type);
    }

}