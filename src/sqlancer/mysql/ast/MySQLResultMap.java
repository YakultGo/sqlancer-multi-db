package sqlancer.mysql.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MySQLResultMap implements MySQLExpression {

    private MySQLValues values;
    private List<MySQLColumnReference> columns;
    private List<MySQLConstant> summary;

    public MySQLResultMap(MySQLValues values, MySQLColumnReference columns, List<MySQLConstant> summary) {
        this.values = values;
        this.columns = new ArrayList<>();
        if (columns != null) {
            this.columns.add(columns);
        }
        this.summary = summary != null ? summary : new ArrayList<>();
    }

    @Override
    public MySQLConstant getExpectedValue() {
        if (summary.isEmpty()) {
            return null;
        }
        return summary.get(0);
    }

    public MySQLValues getValues() {
        return values;
    }

    public List<MySQLColumnReference> getColumns() {
        return Collections.unmodifiableList(columns);
    }

    public List<MySQLConstant> getSummary() {
        return Collections.unmodifiableList(summary);
    }

    public MySQLConstant getSummaryValue(int index) {
        if (index < 0 || index >= summary.size()) {
            throw new IndexOutOfBoundsException("Summary index: " + index);
        }
        return summary.get(index);
    }

}