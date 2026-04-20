package sqlancer.mysql.gen;

import sqlancer.Randomly;
import sqlancer.common.query.ExpectedErrors;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.mysql.MySQLErrors;
import sqlancer.mysql.MySQLGlobalState;
import sqlancer.mysql.MySQLSchema.MySQLTable;

public final class MySQLDropViewGenerator {

    private MySQLDropViewGenerator() {
    }

    public static SQLQueryAdapter drop(MySQLGlobalState globalState) {
        ExpectedErrors errors = new ExpectedErrors();
        StringBuilder sb = new StringBuilder("DROP VIEW");

        // 可选的 IF EXISTS
        if (Randomly.getBoolean()) {
            sb.append(" IF EXISTS");
        }

        sb.append(" ");

        // 找到一个视图来删除
        MySQLTable viewToDrop = null;
        for (MySQLTable table : globalState.getSchema().getDatabaseTables()) {
            if (table.isView()) {
                viewToDrop = table;
                break;
            }
        }

        if (viewToDrop == null) {
            // 如果没有视图，生成一个假的视图名（会触发错误，但这是预期的）
            sb.append("v0");
        } else {
            sb.append(viewToDrop.getName());
        }

        // 添加预期错误
        errors.add("Unknown view");
        errors.add("doesn't exist");
        errors.add("Unknown table");
        errors.add("DROP VIEW");
        MySQLErrors.addExpressionErrors(errors);

        return new SQLQueryAdapter(sb.toString(), errors, true);
    }
}