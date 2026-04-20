package sqlancer.mysql.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import sqlancer.mysql.MySQLSchema.MySQLDataType;

class MySQLBinaryConstantTest {

    @Test
    void emptyBinary_rendersAsHexFormat() {
        MySQLConstant c = MySQLConstant.createBinaryConstant(new byte[0]);
        assertEquals("X''", c.getTextRepresentation());
    }

    @Test
    void singleByte_rendersAsHexFormat() {
        MySQLConstant c = MySQLConstant.createBinaryConstant(new byte[] {0x41});  // 'A'
        assertEquals("X'41'", c.getTextRepresentation());
    }

    @Test
    void multipleBytes_rendersAsHexFormat() {
        MySQLConstant c = MySQLConstant.createBinaryConstant(new byte[] {0x48, 0x65, 0x6C, 0x6C, 0x6F});  // "Hello"
        assertEquals("X'48656C6C6F'", c.getTextRepresentation());
    }

    @Test
    void zeroByte_rendersAsHexFormat() {
        MySQLConstant c = MySQLConstant.createBinaryConstant(new byte[] {0x00});
        assertEquals("X'00'", c.getTextRepresentation());
    }

    @Test
    void mixedBytes_rendersAsHexFormat() {
        MySQLConstant c = MySQLConstant.createBinaryConstant(new byte[] {(byte) 0x00, 0x41, (byte) 0xFF});
        assertEquals("X'0041FF'", c.getTextRepresentation());
    }

    @Test
    void castAsString_asciiBytes_returnsString() {
        MySQLConstant c = MySQLConstant.createBinaryConstant(new byte[] {0x48, 0x65, 0x6C, 0x6C, 0x6F});  // "Hello"
        assertEquals("Hello", c.castAsString());
    }

    @Test
    void castAsString_nonAsciiBytes_returnsEscapedString() {
        MySQLConstant c = MySQLConstant.createBinaryConstant(new byte[] {(byte) 0x00, (byte) 0xFF});
        assertEquals("\\x00\\xFF", c.castAsString());
    }

    @Test
    void getType_returnsBLOB() {
        MySQLConstant c = MySQLConstant.createBinaryConstant(new byte[] {0x41});
        assertEquals(MySQLDataType.BLOB, c.getType());
    }

    @Test
    void asBooleanNotNull_nonZeroByte_returnsTrue() {
        MySQLConstant c = MySQLConstant.createBinaryConstant(new byte[] {0x41});
        assertTrue(c.asBooleanNotNull());
    }

    @Test
    void asBooleanNotNull_zeroByte_returnsFalse() {
        MySQLConstant c = MySQLConstant.createBinaryConstant(new byte[] {0x00});
        assertFalse(c.asBooleanNotNull());
    }

    @Test
    void asBooleanNotNull_emptyBinary_returnsFalse() {
        MySQLConstant c = MySQLConstant.createBinaryConstant(new byte[0]);
        assertFalse(c.asBooleanNotNull());
    }

    @Test
    void isEquals_sameBytes_returnsTrue() {
        MySQLConstant c1 = MySQLConstant.createBinaryConstant(new byte[] {0x41, 0x42});
        MySQLConstant c2 = MySQLConstant.createBinaryConstant(new byte[] {0x41, 0x42});
        assertTrue(c1.isEquals(c2).asBooleanNotNull());
    }

    @Test
    void isEquals_differentBytes_returnsFalse() {
        MySQLConstant c1 = MySQLConstant.createBinaryConstant(new byte[] {0x41, 0x42});
        MySQLConstant c2 = MySQLConstant.createBinaryConstant(new byte[] {0x41, 0x43});
        assertFalse(c1.isEquals(c2).asBooleanNotNull());
    }

    @Test
    void isEquals_differentLength_returnsFalse() {
        MySQLConstant c1 = MySQLConstant.createBinaryConstant(new byte[] {0x41, 0x42});
        MySQLConstant c2 = MySQLConstant.createBinaryConstant(new byte[] {0x41});
        assertFalse(c1.isEquals(c2).asBooleanNotNull());
    }

    @Test
    void isEquals_null_returnsNullConstant() {
        MySQLConstant c = MySQLConstant.createBinaryConstant(new byte[] {0x41});
        MySQLConstant nullC = MySQLConstant.createNullConstant();
        assertTrue(c.isEquals(nullC).isNull());
    }

    @Test
    void isLessThan_smallerBytes_returnsTrue() {
        MySQLConstant c1 = MySQLConstant.createBinaryConstant(new byte[] {0x41});  // 'A'
        MySQLConstant c2 = MySQLConstant.createBinaryConstant(new byte[] {0x42});  // 'B'
        assertTrue(c1.isLessThan(c2).asBooleanNotNull());
    }

    @Test
    void isLessThan_equalBytes_returnsFalse() {
        MySQLConstant c1 = MySQLConstant.createBinaryConstant(new byte[] {0x41});
        MySQLConstant c2 = MySQLConstant.createBinaryConstant(new byte[] {0x41});
        assertFalse(c1.isLessThan(c2).asBooleanNotNull());
    }

    @Test
    void isLessThan_shorterSamePrefix_returnsTrue() {
        MySQLConstant c1 = MySQLConstant.createBinaryConstant(new byte[] {0x41});  // shorter
        MySQLConstant c2 = MySQLConstant.createBinaryConstant(new byte[] {0x41, 0x42});  // longer
        assertTrue(c1.isLessThan(c2).asBooleanNotNull());
    }

    @Test
    void isLessThan_null_returnsNullConstant() {
        MySQLConstant c = MySQLConstant.createBinaryConstant(new byte[] {0x41});
        MySQLConstant nullC = MySQLConstant.createNullConstant();
        assertTrue(c.isLessThan(nullC).isNull());
    }

    @Test
    void getBinaryValue_returnsOriginalBytes() {
        byte[] original = new byte[] {0x41, 0x42, 0x43};
        MySQLConstant c = MySQLConstant.createBinaryConstant(original);
        byte[] retrieved = ((MySQLConstant.MySQLBinaryConstant) c).getBinaryValue();
        assertEquals(original.length, retrieved.length);
        for (int i = 0; i < original.length; i++) {
            assertEquals(original[i], retrieved[i]);
        }
    }
}