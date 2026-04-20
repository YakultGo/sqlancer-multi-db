package sqlancer.gaussdbpg.gen;

import java.util.List;

import sqlancer.Randomly;
import sqlancer.common.query.ExpectedErrors;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.gaussdbpg.GaussDBPGGlobalState;
import sqlancer.gaussdbpg.GaussDBPGSchema.GaussDBPGColumn;
import sqlancer.gaussdbpg.GaussDBPGSchema.GaussDBPGTable;
import sqlancer.gaussdbpg.GaussDBPGToStringVisitor;
import sqlancer.gaussdbpg.ast.GaussDBPGConstant;

public class GaussDBPGInsertGenerator {

    public static SQLQueryAdapter insertRow(GaussDBPGGlobalState globalState) throws Exception {
        GaussDBPGTable table = globalState.getSchema().getRandomDatabaseTable();
        return insertRow(globalState, table);
    }

    public static SQLQueryAdapter insertRow(GaussDBPGGlobalState globalState, GaussDBPGTable table) {
        ExpectedErrors errors = new ExpectedErrors();
        errors.add("violates not-null constraint");
        errors.add("violates unique constraint");
        errors.add("violates check constraint");
        errors.add("null value in column");
        errors.add("duplicate key value");

        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO ");
        sb.append(table.getName());
        List<GaussDBPGColumn> columns = table.getColumns();
        sb.append("(");
        for (int i = 0; i < columns.size(); i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(columns.get(i).getName());
        }
        sb.append(") VALUES ");
        int nrRows = (int) Randomly.getNotCachedInteger(1, 5);
        for (int i = 0; i < nrRows; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append("(");
            for (int j = 0; j < columns.size(); j++) {
                if (j != 0) {
                    sb.append(", ");
                }
                GaussDBPGConstant constant = getRandomValue(globalState, columns.get(j));
                sb.append(GaussDBPGToStringVisitor.asString(constant));
            }
            sb.append(")");
        }
        return new SQLQueryAdapter(sb.toString(), errors, true);
    }

    private static GaussDBPGConstant getRandomValue(GaussDBPGGlobalState state, GaussDBPGColumn column) {
        Randomly r = state.getRandomly();
        if (Randomly.getBooleanWithSmallProbability()) {
            return GaussDBPGConstant.createNullConstant();
        }
        switch (column.getType()) {
        case INT:
            return GaussDBPGConstant.createIntConstant(r.getInteger());
        case BOOLEAN:
            return GaussDBPGConstant.createBooleanConstant(Randomly.getBoolean());
        case TEXT:
            return GaussDBPGConstant.createTextConstant(r.getString());
        case DECIMAL:
            return GaussDBPGConstant.createDecimalConstant(r.getRandomBigDecimal());
        case FLOAT:
        case REAL:
            return GaussDBPGConstant.createFloatConstant(r.getDouble());
        default:
            return GaussDBPGConstant.createNullConstant();
        }
    }
}