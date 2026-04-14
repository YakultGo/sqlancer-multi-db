package sqlancer.mysql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import sqlancer.mysql.MySQLSchema.MySQLDataType;

class MySQLSchemaTemporalTypesTest {

    private static MySQLDataType getColumnTypeViaReflection(String typeString) {
        try {
            Method m = MySQLSchema.class.getDeclaredMethod("getColumnType", String.class);
            m.setAccessible(true);
            return (MySQLDataType) m.invoke(null, typeString);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof AssertionError) {
                throw (AssertionError) cause;
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new RuntimeException(cause);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void getColumnType_mapsTemporalTypes() {
        assertEquals(MySQLDataType.DATE, getColumnTypeViaReflection("date"));
        assertEquals(MySQLDataType.TIME, getColumnTypeViaReflection("time"));
        assertEquals(MySQLDataType.DATETIME, getColumnTypeViaReflection("datetime"));
        assertEquals(MySQLDataType.TIMESTAMP, getColumnTypeViaReflection("timestamp"));
        assertEquals(MySQLDataType.YEAR, getColumnTypeViaReflection("year"));
    }

    @Test
    void getColumnType_throwsOnUnknownType() {
        assertThrows(AssertionError.class, () -> getColumnTypeViaReflection("this_type_does_not_exist"));
    }
}

