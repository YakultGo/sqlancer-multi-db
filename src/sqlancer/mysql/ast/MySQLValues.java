package sqlancer.mysql.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sqlancer.mysql.MySQLSchema.MySQLColumn;

public class MySQLValues implements MySQLExpression {

    private Map<String, List<MySQLConstant>> values;
    private List<MySQLColumn> columns;
    private List<String> columnNames;

    public MySQLValues() {
        this.values = new HashMap<>();
        this.columns = new ArrayList<>();
        this.columnNames = new ArrayList<>();
    }

    public void addColumn(String columnName, MySQLColumn column) {
        this.columnNames.add(columnName);
        this.columns.add(column);
        if (!values.containsKey(columnName)) {
            values.put(columnName, new ArrayList<>());
        }
    }

    public void addRow(List<MySQLConstant> rowValues) {
        if (rowValues.size() != columnNames.size()) {
            throw new IllegalArgumentException("Row values count must match column count");
        }

        for (int i = 0; i < columnNames.size(); i++) {
            String columnName = columnNames.get(i);
            List<MySQLConstant> columnValues = values.get(columnName);
            columnValues.add(rowValues.get(i));
        }
    }

    public List<MySQLConstant> getColumnValues(String columnName) {
        return values.getOrDefault(columnName, Collections.emptyList());
    }

    public List<String> getColumnNames() {
        return Collections.unmodifiableList(columnNames);
    }

    public List<MySQLColumn> getColumns() {
        return Collections.unmodifiableList(columns);
    }

    public int getRowCount() {
        if (columnNames.isEmpty()) {
            return 0;
        }
        return values.get(columnNames.get(0)).size();
    }

    public MySQLConstant getAt(int row, int col) {
        if (row < 0 || row >= getRowCount() || col < 0 || col >= columnNames.size()) {
            throw new IndexOutOfBoundsException("Row: " + row + ", Col: " + col);
        }
        String columnName = columnNames.get(col);
        return values.get(columnName).get(row);
    }

    @Override
    public MySQLConstant getExpectedValue() {
        return null;
    }

}