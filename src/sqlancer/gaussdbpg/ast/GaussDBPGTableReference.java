package sqlancer.gaussdbpg.ast;

import sqlancer.common.ast.newast.TableReferenceNode;
import sqlancer.gaussdbpg.GaussDBPGSchema.GaussDBPGTable;

public class GaussDBPGTableReference extends TableReferenceNode<GaussDBPGExpression, GaussDBPGTable>
        implements GaussDBPGExpression {

    public GaussDBPGTableReference(GaussDBPGTable table) {
        super(table);
    }

    public static GaussDBPGTableReference create(GaussDBPGTable table) {
        return new GaussDBPGTableReference(table);
    }

    @Override
    public GaussDBPGConstant getExpectedValue() {
        return null;
    }

    @Override
    public GaussDBPGDataType getExpressionType() {
        return null;
    }
}