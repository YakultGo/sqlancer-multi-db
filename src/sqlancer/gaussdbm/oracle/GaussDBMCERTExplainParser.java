package sqlancer.gaussdbm.oracle;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;

import sqlancer.common.oracle.CERTOracle;
import sqlancer.common.query.SQLancerResultSet;

/**
 * Maps EXPLAIN result columns by label (GaussDB-M / MySQL-compatible layouts differ from fixed JDBC indices).
 */
public final class GaussDBMCERTExplainParser {

    private GaussDBMCERTExplainParser() {
    }

    public static CERTOracle.CheckedFunction<SQLancerResultSet, Optional<Long>> rowCountParser() {
        return rs -> {
            int idx = findColumnIndex(rs, "rows");
            if (idx < 0) {
                return Optional.empty();
            }
            try {
                return Optional.of(rs.getLong(idx));
            } catch (SQLException e) {
                String s = rs.getString(idx);
                if (s == null || s.isEmpty()) {
                    return Optional.empty();
                }
                try {
                    return Optional.of(Long.parseLong(s.trim()));
                } catch (NumberFormatException ex) {
                    return Optional.empty();
                }
            }
        };
    }

    /**
     * One token per EXPLAIN row for plan-shape comparison (aligned with MySQL using a stable per-row identifier).
     */
    public static CERTOracle.CheckedFunction<SQLancerResultSet, Optional<String>> queryPlanParser() {
        return rs -> {
            int idx = findColumnIndex(rs, "type", "select_type", "operation");
            if (idx < 0) {
                idx = findColumnIndex(rs, "table", "TABLE");
            }
            if (idx < 0) {
                return Optional.empty();
            }
            String s = rs.getString(idx);
            return Optional.ofNullable(s);
        };
    }

    private static int findColumnIndex(SQLancerResultSet rs, String... candidates) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        int n = md.getColumnCount();
        for (int i = 1; i <= n; i++) {
            String label = md.getColumnLabel(i);
            if (label == null || label.isEmpty()) {
                label = md.getColumnName(i);
            }
            if (label == null) {
                continue;
            }
            String norm = label.toLowerCase(Locale.ROOT);
            for (String c : candidates) {
                if (norm.equals(c.toLowerCase(Locale.ROOT))) {
                    return i;
                }
            }
        }
        return -1;
    }
}
