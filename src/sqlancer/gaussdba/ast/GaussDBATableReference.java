package sqlancer.gaussdba.ast;

import sqlancer.gaussdba.GaussDBASchema.GaussDBATable;

public class GaussDBATableReference implements GaussDBAExpression {

    private final GaussDBATable table;

    public GaussDBATableReference(GaussDBATable table) {
        this.table = table;
    }

    public static GaussDBATableReference create(GaussDBATable table) {
        return new GaussDBATableReference(table);
    }

    public GaussDBATable getTable() {
        return table;
    }

    @Override
    public GaussDBAConstant getExpectedValue() {
        return null;
    }

    @Override
    public GaussDBADataType getExpressionType() {
        return null;
    }
}