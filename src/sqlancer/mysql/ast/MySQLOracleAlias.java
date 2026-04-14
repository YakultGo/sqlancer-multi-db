package sqlancer.mysql.ast;

public class MySQLOracleAlias implements MySQLExpression {

    private MySQLExpression originalExpression;
    private MySQLExpression aliasExpression;

    public MySQLOracleAlias(MySQLExpression originalExpr, MySQLExpression aliasExpr) {
        this.originalExpression = originalExpr;
        this.aliasExpression = aliasExpr;
    }

    public MySQLExpression getOriginalExpression() {
        return originalExpression;
    }

    public MySQLExpression getAliasExpression() {
        return aliasExpression;
    }

    @Override
    public MySQLConstant getExpectedValue() {
        if (aliasExpression != null) {
            return aliasExpression.getExpectedValue();
        }
        return originalExpression != null ? originalExpression.getExpectedValue() : null;
    }

}