package sqlancer.mysql.ast;

import sqlancer.mysql.MySQLSchema.MySQLColumn;
import sqlancer.mysql.MySQLSchema.MySQLTable;

public class MySQLTableAndColumnRef implements MySQLExpression {

    private MySQLTable table;
    private MySQLColumn column;

    public MySQLTableAndColumnRef(MySQLTable table, MySQLColumn column) {
        this.table = table;
        this.column = column;
    }

    public MySQLTable getTable() {
        return table;
    }

    public MySQLColumn getColumn() {
        return column;
    }

    @Override
    public MySQLConstant getExpectedValue() {
        if (column != null && table != null) {
            // For CODDTest, we don't have actual values at AST level
            // Return a mock constant for testing purposes
            return MySQLConstant.createStringConstant("<COLUMN_REF_" + table.getName() + "." + column.getName() + ">");
        }
        return null;
    }

}