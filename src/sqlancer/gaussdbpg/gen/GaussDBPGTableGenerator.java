package sqlancer.gaussdbpg.gen;

import java.util.ArrayList;
import java.util.List;

import sqlancer.Randomly;
import sqlancer.common.DBMSCommon;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.gaussdbpg.GaussDBPGGlobalState;
import sqlancer.gaussdbpg.GaussDBPGSchema.GaussDBPGColumn;
import sqlancer.gaussdbpg.GaussDBPGSchema.GaussDBPGCompositeDataType;
import sqlancer.gaussdbpg.ast.GaussDBPGDataType;

public class GaussDBPGTableGenerator {

    public static SQLQueryAdapter generate(GaussDBPGGlobalState globalState, String tableName) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ");
        sb.append(tableName);
        sb.append("(");
        int nrColumns = (int) Randomly.getNotCachedInteger(1, 5);
        List<GaussDBPGColumn> columns = new ArrayList<>();
        for (int i = 0; i < nrColumns; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            String columnName = DBMSCommon.createColumnName(i);
            sb.append(columnName);
            sb.append(" ");
            GaussDBPGDataType dataType = getRandomType(globalState);
            sb.append(getDataTypeString(dataType));
            columns.add(new GaussDBPGColumn(columnName, dataType));
        }
        if (Randomly.getBoolean()) {
            sb.append(", PRIMARY KEY(");
            sb.append(Randomly.fromList(columns).getName());
            sb.append(")");
        }
        sb.append(")");
        return new SQLQueryAdapter(sb.toString(), true);
    }

    private static GaussDBPGDataType getRandomType(GaussDBPGGlobalState state) {
        GaussDBPGCompositeDataType compositeType = GaussDBPGCompositeDataType.getRandom();
        return compositeType.getPrimitiveDataType();
    }

    private static String getDataTypeString(GaussDBPGDataType type) {
        switch (type) {
        case INT:
            return "INTEGER";
        case BOOLEAN:
            return "BOOLEAN";
        case TEXT:
            return "TEXT";
        case DECIMAL:
            return "DECIMAL";
        case FLOAT:
            return "DOUBLE PRECISION";
        case REAL:
            return "REAL";
        case DATE:
            return "DATE";
        case TIME:
            return "TIME";
        case TIMESTAMP:
            return "TIMESTAMP";
        case TIMESTAMPTZ:
            return "TIMESTAMP WITH TIME ZONE";
        case INTERVAL:
            return "INTERVAL";
        default:
            throw new AssertionError(type);
        }
    }
}