package sqlancer.gaussdbm.gen;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import sqlancer.Randomly;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.gaussdbm.GaussDBMGlobalState;
import sqlancer.gaussdbm.GaussDBMSchema.GaussDBDataType;

public final class GaussDBMTableGenerator {

    private GaussDBMTableGenerator() {
    }

    public static SQLQueryAdapter generate(GaussDBMGlobalState globalState, String tableName) {
        int nrColumns = (int) Randomly.getNotCachedInteger(1, 4);
        List<String> columnDefs = new ArrayList<>();
        for (int i = 0; i < nrColumns; i++) {
            String col = "c" + i;
            GaussDBDataType type = Randomly.fromOptions(GaussDBDataType.INT, GaussDBDataType.VARCHAR);
            switch (type) {
            case INT:
                columnDefs.add(col + " INT");
                break;
            case VARCHAR:
                columnDefs.add(col + " VARCHAR(20)");
                break;
            default:
                throw new AssertionError(type);
            }
        }
        String sql = "CREATE TABLE " + tableName + " (" + columnDefs.stream().collect(Collectors.joining(", ")) + ")";
        return new SQLQueryAdapter(sql, true);
    }
}

