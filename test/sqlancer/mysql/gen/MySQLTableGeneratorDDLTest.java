package sqlancer.mysql.gen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import sqlancer.mysql.MySQLSchema.MySQLDataType;

/**
 * 临时表和外键支持的单元测试
 *
 * 注意：由于 DDL 生成依赖于完整的 GlobalState 和数据库连接，
 * 这里主要测试数据类型定义的正确性。
 * 完整的 DDL 生成测试应该在集成测试中进行。
 */
class MySQLTableGeneratorDDLTest {

    /**
     * 测试 MySQLDataType 枚举包含 JSON
     */
    @Test
    void dataType_enumContainsJSON() {
        assertTrue(MySQLDataType.JSON != null);
    }

    /**
     * 测试 MySQLDataType 枚举包含 BINARY 类型
     */
    @Test
    void dataType_enumContainsBinaryTypes() {
        assertTrue(MySQLDataType.BINARY != null);
        assertTrue(MySQLDataType.VARBINARY != null);
        assertTrue(MySQLDataType.BLOB != null);
    }

    /**
     * 测试 MySQLDataType.isNumeric() 方法正确排除 JSON
     */
    @Test
    void dataType_jsonIsNotNumeric() {
        assertTrue(!MySQLDataType.JSON.isNumeric());
    }

    /**
     * 测试 MySQLDataType.isNumeric() 方法正确排除 BINARY 类型
     */
    @Test
    void dataType_binaryTypesAreNotNumeric() {
        assertTrue(!MySQLDataType.BINARY.isNumeric());
        assertTrue(!MySQLDataType.VARBINARY.isNumeric());
        assertTrue(!MySQLDataType.BLOB.isNumeric());
    }
}