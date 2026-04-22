package sqlancer.mysql.gen;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import sqlancer.mysql.MySQLBugs;

/**
 * Tests for MySQL bug workaround configuration.
 * These tests verify that the bug flags are properly set and that
 * the workarounds can be toggled for testing.
 */
class MySQLBugsConfigurationTest {

    // ========== Bug #99135 - Binary bitwise operations ==========

    @Test
    void bug99135_isDisabled() {
        // We have enabled this by setting bug99135 = false
        assertFalse(MySQLBugs.bug99135, "Bug #99135 should be disabled (false) to enable bitwise operations");
    }

    // ========== Bug #99181 - BETWEEN operator ==========

    @Test
    void bug99181_isDisabled() {
        // We have enabled this by setting bug99181 = false
        assertFalse(MySQLBugs.bug99181, "Bug #99181 should be disabled (false) to enable BETWEEN operations");
    }

    // ========== Bug #99183 - Precision/scale ==========

    @Test
    void bug99183_isDisabled() {
        // We have enabled this by setting bug99183 = false
        assertFalse(MySQLBugs.bug99183, "Bug #99183 should be disabled (false) to enable precision/scale generation");
    }

    // ========== Bug #99127 - UNSIGNED type ==========

    @Test
    void bug99127_isEnabled() {
        // This is still enabled for safety
        assertTrue(MySQLBugs.bug99127, "Bug #99127 should remain enabled for UNSIGNED type safety");
    }

    // ========== Other bugs ==========

    @Test
    void bug95894_exists() {
        // Just verify the field exists
        assertNotNull(MySQLBugs.bug95894);
    }

    @Test
    void bug111471_exists() {
        // Just verify the field exists
        assertNotNull(MySQLBugs.bug111471);
    }

    @Test
    void bug112242_exists() {
        // Just verify the field exists
        assertNotNull(MySQLBugs.bug112242);
    }

    @Test
    void bug112243_exists() {
        // Just verify the field exists
        assertNotNull(MySQLBugs.bug112243);
    }

    @Test
    void bug112264_exists() {
        // Just verify the field exists
        assertNotNull(MySQLBugs.bug112264);
    }

    @Test
    void bug114533_exists() {
        // Just verify the field exists
        assertNotNull(MySQLBugs.bug114533);
    }

    @Test
    void bug114534_exists() {
        // Just verify the field exists
        assertNotNull(MySQLBugs.bug114534);
    }
}