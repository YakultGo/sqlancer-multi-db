package sqlancer.postgres.oracle.ext.eet;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class EETMultisetComparator {

    private EETMultisetComparator() {
    }

    public static boolean compareResultMultisets(List<List<String>> a, List<List<String>> b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.size() != b.size()) {
            return false;
        }
        // stable ordering: stringify each row and sort
        List<String> sa = new ArrayList<>();
        for (List<String> row : a) {
            sa.add(rowKey(row));
        }
        List<String> sb = new ArrayList<>();
        for (List<String> row : b) {
            sb.add(rowKey(row));
        }
        sa.sort(String::compareTo);
        sb.sort(String::compareTo);
        return sa.equals(sb);
    }

    private static String rowKey(List<String> row) {
        if (row == null) {
            return "<null-row>";
        }
        return row.stream().map(v -> v == null ? "<null>" : v).collect(Collectors.joining("\u0001"));
    }
}

