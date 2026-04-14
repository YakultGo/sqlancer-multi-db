package sqlancer.gaussdb.gen;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.gaussdb.GaussDBGlobalState;
import sqlancer.gaussdb.GaussDBSchema.GaussDBColumn;
import sqlancer.gaussdb.GaussDBSchema.GaussDBTable;
import sqlancer.gaussdb.ast.GaussDBConstant;

public final class GaussDBInsertGenerator {

    private GaussDBInsertGenerator() {
    }

    public static SQLQueryAdapter insertRow(GaussDBGlobalState globalState) throws SQLException {
        GaussDBTable table = globalState.getSchema().getRandomTable();
        return insertRow(globalState, table);
    }

    public static SQLQueryAdapter insertRow(GaussDBGlobalState globalState, GaussDBTable table) throws SQLException {
        List<GaussDBColumn> columns = table.getRandomNonEmptyColumnSubset();
        String cols = columns.stream().map(GaussDBColumn::getName).collect(Collectors.joining(", "));
        String vals = columns.stream().map(c -> GaussDBConstant.createRandomConstant().getTextRepresentation())
                .collect(Collectors.joining(", "));
        String sql = "INSERT INTO " + table.getName() + "(" + cols + ") VALUES (" + vals + ")";
        return new SQLQueryAdapter(sql);
    }
}

