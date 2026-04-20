package sqlancer.gaussdba.oracle;

import java.sql.SQLException;
import java.util.Optional;

import sqlancer.common.oracle.CERTOracle;
import sqlancer.common.oracle.TestOracle;
import sqlancer.common.query.ExpectedErrors;
import sqlancer.common.query.SQLancerResultSet;
import sqlancer.gaussdba.GaussDBAErrors;
import sqlancer.gaussdba.GaussDBAGlobalState;
import sqlancer.gaussdba.GaussDBASchema;
import sqlancer.gaussdba.GaussDBASchema.GaussDBAColumn;
import sqlancer.gaussdba.GaussDBASchema.GaussDBATable;
import sqlancer.gaussdba.ast.GaussDBAExpression;
import sqlancer.gaussdba.ast.GaussDBAJoin;
import sqlancer.gaussdba.ast.GaussDBASelect;
import sqlancer.gaussdba.gen.GaussDBAExpressionGenerator;

/**
 * CERT Oracle for GaussDB A compatibility mode.
 * Checks that query plan changes do not affect result counts.
 */
public class GaussDBACERTOracle implements TestOracle<GaussDBAGlobalState> {

    private final CERTOracle<GaussDBASelect, GaussDBAJoin, GaussDBAExpression, GaussDBASchema, GaussDBATable,
            GaussDBAColumn, GaussDBAGlobalState> certOracle;

    public GaussDBACERTOracle(GaussDBAGlobalState globalState) {
        GaussDBAExpressionGenerator gen = new GaussDBAExpressionGenerator(globalState);
        ExpectedErrors errors = ExpectedErrors.newErrors()
                .with(GaussDBAErrors.getExpressionErrorStrings())
                .with(GaussDBAErrors.getPlanErrorStrings())
                .build();

        this.certOracle = new CERTOracle<>(
                globalState,
                gen,
                errors,
                this::parseRowCount,
                this::parseQueryPlan);
    }

    @Override
    public void check() throws SQLException {
        certOracle.check();
    }

    /**
     * Parse row count from EXPLAIN output.
     * GaussDB A mode EXPLAIN format may show estimated rows.
     */
    private Optional<Long> parseRowCount(SQLancerResultSet rs) throws SQLException {
        String line = rs.getString(1);
        if (line == null) {
            return Optional.empty();
        }
        // Look for patterns like "rows=N" or "Rows=N"
        String lowerLine = line.toLowerCase();
        if (lowerLine.contains("rows=") || lowerLine.contains("row=")) {
            try {
                int idx = lowerLine.indexOf("rows=");
                if (idx == -1) {
                    idx = lowerLine.indexOf("row=");
                }
                if (idx != -1) {
                    String numPart = lowerLine.substring(idx + 5).trim();
                    StringBuilder numStr = new StringBuilder();
                    for (char c : numPart.toCharArray()) {
                        if (Character.isDigit(c)) {
                            numStr.append(c);
                        } else {
                            break;
                        }
                    }
                    if (numStr.length() > 0) {
                        return Optional.of(Long.parseLong(numStr.toString()));
                    }
                }
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    /**
     * Parse query plan node type from EXPLAIN output.
     */
    private Optional<String> parseQueryPlan(SQLancerResultSet rs) throws SQLException {
        String line = rs.getString(1);
        if (line == null) {
            return Optional.empty();
        }
        line = line.trim();
        if (line.isEmpty()) {
            return Optional.empty();
        }
        String[] parts = line.split("\\s+");
        if (parts.length > 0) {
            return Optional.of(parts[0]);
        }
        return Optional.empty();
    }
}