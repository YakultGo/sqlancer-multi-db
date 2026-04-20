package sqlancer.gaussdba.gen;

import java.util.List;
import java.util.stream.Collectors;

import sqlancer.Randomly;
import sqlancer.common.query.ExpectedErrors;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.gaussdba.GaussDBAErrors;
import sqlancer.gaussdba.GaussDBAGlobalState;
import sqlancer.gaussdba.GaussDBASchema.GaussDBAColumn;
import sqlancer.gaussdba.GaussDBASchema.GaussDBATable;
import sqlancer.gaussdba.ast.GaussDBAConstant;

public final class GaussDBAInsertGenerator {

    private GaussDBAInsertGenerator() {
    }

    public static SQLQueryAdapter insertRow(GaussDBAGlobalState globalState) {
        GaussDBATable table = globalState.getSchema().getRandomDatabaseTable();
        return insertRow(globalState, table);
    }

    public static SQLQueryAdapter insertRow(GaussDBAGlobalState globalState, GaussDBATable table) {
        ExpectedErrors errors = new ExpectedErrors();
        GaussDBAErrors.addInsertUpdateErrors(errors);
        errors.add("violates not-null constraint");
        errors.add("violates unique constraint");
        errors.add("violates check constraint");
        errors.add("null value in column");
        errors.add("duplicate key value");
        errors.add("value too large");
        errors.add("invalid input syntax");

        List<GaussDBAColumn> columns = table.getColumns();
        List<GaussDBAColumn> selectedColumns = Randomly.nonEmptySubset(columns);

        List<String> values = selectedColumns.stream()
                .map(c -> generateRandomValue(globalState, c))
                .collect(Collectors.toList());

        String columnNames = selectedColumns.stream()
                .map(GaussDBAColumn::getName)
                .collect(Collectors.joining(", "));

        String valueStr = values.stream().collect(Collectors.joining(", "));

        String sql = "INSERT INTO " + table.getName() + " (" + columnNames + ") VALUES (" + valueStr + ")";
        return new SQLQueryAdapter(sql, errors, true);
    }

    private static String generateRandomValue(GaussDBAGlobalState globalState, GaussDBAColumn column) {
        if (Randomly.getBooleanWithSmallProbability()) {
            return "NULL";
        }

        GaussDBAConstant constant = generateConstantForType(globalState, column.getType());
        return constant.getTextRepresentation();
    }

    private static GaussDBAConstant generateConstantForType(GaussDBAGlobalState globalState,
            sqlancer.gaussdba.ast.GaussDBADataType type) {
        Randomly r = globalState.getRandomly();

        switch (type) {
        case NUMBER:
            return GaussDBAConstant.createNumberConstant(r.getInteger());
        case VARCHAR2:
            String str = r.getString();
            // Oracle语义：空字符串被视为NULL
            // 为了避免插入NULL，确保字符串非空
            if (str.isEmpty()) {
                str = "x";
            }
            return GaussDBAConstant.createVarchar2Constant(str.substring(0, Math.min(str.length(), 50)));
        case DATE:
            return GaussDBAConstant.createDateConstant(java.time.LocalDate.now().plusDays(r.getInteger(-365, 365)));
        case TIMESTAMP:
            return GaussDBAConstant.createTimestampConstant(
                    java.time.LocalDateTime.now().plusDays(r.getInteger(-365, 365)));
        case CLOB:
            String clobStr = r.getString();
            if (clobStr.isEmpty()) {
                clobStr = "clob_data";
            }
            return GaussDBAConstant.createVarchar2Constant(clobStr.substring(0, Math.min(clobStr.length(), 100)));
        case BLOB:
            // BLOB类型暂用空值处理
            return GaussDBAConstant.createNullConstant();
        default:
            return GaussDBAConstant.createNumberConstant(r.getInteger());
        }
    }
}