package sqlancer.mysql.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import sqlancer.mysql.MySQLSchema.MySQLDataType;

class MySQLJSONConstantTest {

    @Test
    void jsonType_returnsJSON() {
        MySQLConstant c = MySQLConstant.createJSONConstant("{\"key\": \"value\"}");
        assertEquals(MySQLDataType.JSON, c.getType());
    }

    @Test
    void simpleObject_rendersAsSingleQuoted() {
        MySQLConstant c = MySQLConstant.createJSONConstant("{\"a\": 1}");
        assertEquals("'{\"a\": 1}'", c.getTextRepresentation());
    }

    @Test
    void jsonArray_rendersAsSingleQuoted() {
        MySQLConstant c = MySQLConstant.createJSONConstant("[1, 2, 3]");
        assertEquals("'[1, 2, 3]'", c.getTextRepresentation());
    }

    @Test
    void jsonNull_rendersAsSingleQuoted() {
        MySQLConstant c = MySQLConstant.createJSONConstant("null");
        assertEquals("'null'", c.getTextRepresentation());
    }

    @Test
    void jsonString_rendersAsSingleQuoted() {
        MySQLConstant c = MySQLConstant.createJSONConstant("\"hello\"");
        assertEquals("'\"hello\"'", c.getTextRepresentation());
    }

    @Test
    void jsonWithSingleQuote_escapesSingleQuote() {
        MySQLConstant c = MySQLConstant.createJSONConstant("{\"key\": \"value'with'quote\"}");
        assertEquals("'{\"key\": \"value\\'with\\'quote\"}'", c.getTextRepresentation());
    }

    @Test
    void jsonWithBackslash_escapesBackslash() {
        MySQLConstant c = MySQLConstant.createJSONConstant("{\"path\": \"C:\\\\folder\"}");
        assertEquals("'{\"path\": \"C:\\\\\\\\folder\"}'", c.getTextRepresentation());
    }

    @Test
    void castAsString_returnsRawJson() {
        MySQLConstant c = MySQLConstant.createJSONConstant("{\"a\": 1}");
        assertEquals("{\"a\": 1}", c.castAsString());
    }

    @Test
    void isString_returnsTrue() {
        MySQLConstant c = MySQLConstant.createJSONConstant("{\"a\": 1}");
        assertTrue(c.isString());
    }

    @Test
    void getString_returnsRawJson() {
        MySQLConstant c = MySQLConstant.createJSONConstant("{\"a\": 1}");
        assertEquals("{\"a\": 1}", c.getString());
    }

    @Test
    void asBooleanNotNull_nonEmpty_returnsTrue() {
        MySQLConstant c = MySQLConstant.createJSONConstant("{\"a\": 1}");
        assertTrue(c.asBooleanNotNull());
    }

    @Test
    void asBooleanNotNull_empty_returnsFalse() {
        MySQLConstant c = MySQLConstant.createJSONConstant("");
        assertFalse(c.asBooleanNotNull());
    }

    @Test
    void isEquals_sameJson_returnsTrue() {
        MySQLConstant c1 = MySQLConstant.createJSONConstant("{\"a\": 1}");
        MySQLConstant c2 = MySQLConstant.createJSONConstant("{\"a\": 1}");
        assertTrue(c1.isEquals(c2).asBooleanNotNull());
    }

    @Test
    void isEquals_differentJson_returnsFalse() {
        MySQLConstant c1 = MySQLConstant.createJSONConstant("{\"a\": 1}");
        MySQLConstant c2 = MySQLConstant.createJSONConstant("{\"a\": 2}");
        assertFalse(c1.isEquals(c2).asBooleanNotNull());
    }

    @Test
    void isEquals_null_returnsNullConstant() {
        MySQLConstant c = MySQLConstant.createJSONConstant("{\"a\": 1}");
        MySQLConstant nullC = MySQLConstant.createNullConstant();
        assertTrue(c.isEquals(nullC).isNull());
    }

    @Test
    void isEquals_string_sameContent_returnsTrue() {
        MySQLConstant jsonC = MySQLConstant.createJSONConstant("{\"a\": 1}");
        MySQLConstant strC = MySQLConstant.createStringConstant("{\"a\": 1}");
        assertTrue(jsonC.isEquals(strC).asBooleanNotNull());
    }

    @Test
    void nestedJson_rendersCorrectly() {
        MySQLConstant c = MySQLConstant.createJSONConstant("{\"outer\": {\"inner\": 123}}");
        assertEquals("'{\"outer\": {\"inner\": 123}}'", c.getTextRepresentation());
    }

    @Test
    void mixedArray_rendersCorrectly() {
        MySQLConstant c = MySQLConstant.createJSONConstant("[1, \"two\", null, true]");
        assertEquals("'[1, \"two\", null, true]'", c.getTextRepresentation());
    }
}