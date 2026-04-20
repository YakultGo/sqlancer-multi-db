package sqlancer.gaussdba.gen;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import sqlancer.Randomly;
import sqlancer.common.query.ExpectedErrors;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.gaussdba.GaussDBAGlobalState;
import sqlancer.gaussdba.GaussDBASchema.GaussDBACompositeDataType;

public final class GaussDBATableGenerator {

    private GaussDBATableGenerator() {
    }

    public static SQLQueryAdapter generate(GaussDBAGlobalState globalState, String tableName) {
        ExpectedErrors errors = new ExpectedErrors();
        errors.add("invalid input syntax");
        errors.add("value too large");
        errors.add("cannot create");
        errors.add("already exists");

        int nrColumns = (int) Randomly.getNotCachedInteger(1, 4);
        List<String> columnDefs = new ArrayList<>();
        boolean hasPrimaryKey = false;
        List<String> primaryKeyColumns = new ArrayList<>();
        List<String> uniqueColumns = new ArrayList<>();

        for (int i = 0; i < nrColumns; i++) {
            String col = "c" + i;
            GaussDBACompositeDataType type = GaussDBACompositeDataType.getRandom();
            StringBuilder colDef = new StringBuilder();
            colDef.append(col);
            colDef.append(" ");

            // Data type
            switch (type) {
            case NUMBER:
                colDef.append("NUMBER");
                break;
            case VARCHAR2:
                colDef.append("VARCHAR2(100)");
                break;
            case DATE:
                colDef.append("DATE");
                break;
            case TIMESTAMP:
                colDef.append("TIMESTAMP");
                break;
            case CLOB:
                colDef.append("CLOB");
                break;
            case BLOB:
                colDef.append("BLOB");
                break;
            default:
                throw new AssertionError(type);
            }

            // Column constraints (Oracle A-compatible syntax)
            List<ColumnConstraint> constraints = generateColumnConstraints(type, hasPrimaryKey);
            for (ColumnConstraint constraint : constraints) {
                colDef.append(" ");
                colDef.append(constraint.getText());
                if (constraint == ColumnConstraint.PRIMARY_KEY) {
                    hasPrimaryKey = true;
                    primaryKeyColumns.add(col);
                }
                if (constraint == ColumnConstraint.UNIQUE) {
                    uniqueColumns.add(col);
                }
            }

            columnDefs.add(colDef.toString());
        }

        // Table-level constraints
        if (!hasPrimaryKey && Randomly.getBooleanWithSmallProbability()) {
            // Add table-level PRIMARY KEY constraint
            List<String> pkCols = Randomly.nonEmptySubset(columnDefs.stream()
                    .map(c -> c.split(" ")[0])
                    .collect(Collectors.toList()));
            columnDefs.add("PRIMARY KEY (" + pkCols.stream().collect(Collectors.joining(", ")) + ")");
            hasPrimaryKey = true;
        }

        if (Randomly.getBooleanWithSmallProbability() && !uniqueColumns.isEmpty()) {
            // Add table-level UNIQUE constraint
            List<String> ukCols = Randomly.nonEmptySubset(columnDefs.stream()
                    .map(c -> c.split(" ")[0])
                    .collect(Collectors.toList()));
            columnDefs.add("UNIQUE (" + ukCols.stream().collect(Collectors.joining(", ")) + ")");
        }

        String sql = "CREATE TABLE " + tableName + " (" + columnDefs.stream().collect(Collectors.joining(", ")) + ")";
        return new SQLQueryAdapter(sql, errors, true);
    }

    private enum ColumnConstraint {
        NOT_NULL("NOT NULL"),
        NULL("NULL"),
        UNIQUE("UNIQUE"),
        PRIMARY_KEY("PRIMARY KEY"),
        DEFAULT("DEFAULT ");

        private final String text;

        ColumnConstraint(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }
    }

    private static List<ColumnConstraint> generateColumnConstraints(GaussDBACompositeDataType type,
            boolean hasPrimaryKey) {
        List<ColumnConstraint> constraints = new ArrayList<>();

        // NOT NULL constraint (fairly common in Oracle)
        if (Randomly.getBoolean()) {
            constraints.add(Randomly.fromOptions(ColumnConstraint.NOT_NULL, ColumnConstraint.NULL));
        }

        // PRIMARY KEY - only one per table
        if (!hasPrimaryKey && Randomly.getBooleanWithSmallProbability()) {
            // PRIMARY KEY columns should be NOT NULL
            if (!constraints.contains(ColumnConstraint.NOT_NULL)) {
                constraints.add(ColumnConstraint.NOT_NULL);
            }
            constraints.add(ColumnConstraint.PRIMARY_KEY);
        }

        // UNIQUE constraint
        if (!constraints.contains(ColumnConstraint.PRIMARY_KEY) && Randomly.getBooleanWithSmallProbability()) {
            constraints.add(ColumnConstraint.UNIQUE);
        }

        return constraints;
    }
}