package sqlancer.mysql.oracle.eet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Multiset equality for EET result comparison: order-insensitive, duplicates preserved.
 */
public final class EETMultisetComparator {

    private EETMultisetComparator() {
    }

    /**
     * Builds a stable string key for one row (tab-separated column values). {@code null} cells become literal
     * "null" for consistency with JDBC getString behavior on some drivers.
     */
    public static String rowKey(List<String> row) {
        List<String> parts = new ArrayList<>(row.size());
        for (String c : row) {
            parts.add(c == null ? "null" : c);
        }
        return String.join("\t", parts);
    }

    /**
     * Returns true iff the two multisets of rows are equal (same multiset of row keys).
     */
    public static boolean compareResultMultisets(List<List<String>> a, List<List<String>> b) {
        if (a.size() != b.size()) {
            return false;
        }
        List<String> keysA = toSortedKeys(a);
        List<String> keysB = toSortedKeys(b);
        return Objects.equals(keysA, keysB);
    }

    static List<String> toSortedKeys(List<List<String>> rows) {
        List<String> keys = new ArrayList<>(rows.size());
        for (List<String> row : rows) {
            keys.add(rowKey(row));
        }
        Collections.sort(keys);
        return keys;
    }
}
