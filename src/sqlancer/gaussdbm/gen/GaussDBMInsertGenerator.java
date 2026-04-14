package sqlancer.gaussdbm.gen;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.gaussdbm.GaussDBMGlobalState;
import sqlancer.gaussdbm.GaussDBMSchema.GaussDBColumn;
import sqlancer.gaussdbm.GaussDBMSchema.GaussDBTable;
import sqlancer.gaussdbm.ast.GaussDBConstant;

public final class GaussDBMInsertGenerator {

    private GaussDBMInsertGenerator() {
    }

    public static SQLQueryAdapter insertRow(GaussDBMGlobalState globalState) throws SQLException {
        GaussDBTable table = globalState.getSchema().getRandomTable();
        return insertRow(globalState, table);
    }

    public static SQLQueryAdapter insertRow(GaussDBMGlobalState globalState, GaussDBTable table) throws SQLException {
        List<GaussDBColumn> columns = table.getRandomNonEmptyColumnSubset();
        String cols = columns.stream().map(GaussDBColumn::getName).collect(Collectors.joining(", "));
        String vals = columns.stream().map(c -> GaussDBConstant.createRandomConstant().getTextRepresentation())
                .collect(Collectors.joining(", "));
        String sql = "INSERT INTO " + table.getName() + "(" + cols + ") VALUES (" + vals + ")";
        return new SQLQueryAdapter(sql);
    }
}

