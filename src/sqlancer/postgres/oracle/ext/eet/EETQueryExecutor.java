package sqlancer.postgres.oracle.ext.eet;

import java.sql.SQLException;
import java.util.List;

public interface EETQueryExecutor {

    List<List<String>> executeQuery(String sql) throws SQLException;
}

