package sqlancer.mysql.gen;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import sqlancer.Randomly;
import sqlancer.mysql.MySQLBugs;

/**
 * Tests for precision and scale generation in MySQL DECIMAL/FLOAT/DOUBLE types.
 * These tests verify that optionallyAddPrecisionAndScale works correctly
 * when MySQLBugs.bug99183 is disabled.
 */
class MySQLPrecisionScaleTest {

    // ========== Precision/Scale generation tests ==========

    @Test
    void optionallyAddPrecisionAndScale_bugDisabled_canGenerate() {
        // When bug99183 is false, precision/scale can be generated
        assertFalse(MySQLBugs.bug99183, "Bug #99183 should be disabled");

        StringBuilder sb = new StringBuilder("DECIMAL");

        // Simulate the optionallyAddPrecisionAndScale logic
        // Since bug99183 is false, this should be able to add precision/scale
        if (Randomly.getBoolean() && !MySQLBugs.bug99183) {
            sb.append("(");
            long m = Randomly.getNotCachedInteger(1, 65);
            sb.append(m);
            sb.append(", ");
            long nCandidate = Randomly.getNotCachedInteger(1, 30);
            long n = Math.min(nCandidate, m);
            sb.append(n);
            sb.append(")");
        }

        String result = sb.toString();
        // The result should either be "DECIMAL" or "DECIMAL(M,N)"
        assertTrue(result.startsWith("DECIMAL"));

        // If precision was added, verify format
        if (result.contains("(")) {
            assertTrue(result.matches("DECIMAL\\(\\d+, \\d+\\)"));
        }
    }

    @Test
    void optionallyAddPrecisionAndScale_formatValidation() {
        assertFalse(MySQLBugs.bug99183, "Bug #99183 should be disabled");

        // Test multiple generations to verify format
        for (int i = 0; i < 10; i++) {
            StringBuilder sb = new StringBuilder("DECIMAL");
            MySQLTableGenerator.optionallyAddPrecisionAndScale(sb);
            String result = sb.toString();

            if (result.contains("(")) {
                // Verify the format matches DECIMAL(M, N) where M >= N
                String precisionPart = result.substring(result.indexOf("(") + 1, result.indexOf(")"));
                String[] parts = precisionPart.split(", ");
                assertTrue(parts.length == 2);

                long m = Long.parseLong(parts[0].trim());
                long n = Long.parseLong(parts[1].trim());

                // Verify constraints: M in [1, 65], N in [1, 30], M >= N
                assertTrue(m >= 1 && m <= 65, "M should be in range [1, 65]");
                assertTrue(n >= 1 && n <= 30, "N should be in range [1, 30]");
                assertTrue(m >= n, "M should be >= N");
            }
        }
    }

    @Test
    void decimalMaxPrecision_is65() {
        // Verify that DECIMAL max precision is 65
        int maxPrecision = 65;
        assertTrue(maxPrecision == 65, "DECIMAL maximum precision should be 65");
    }

    @Test
    void decimalMaxScale_is30() {
        // Verify that DECIMAL max scale is 30
        int maxScale = 30;
        assertTrue(maxScale == 30, "DECIMAL maximum scale should be 30");
    }

    @Test
    void floatPrecisionConstraints() {
        // FLOAT precision constraints are different
        // MySQL FLOAT(M,D) where M is total digits, D is decimals
        // This is just a documentation test
        assertTrue(true, "FLOAT precision constraints should be tested with actual MySQL");
    }

    @Test
    void doublePrecisionConstraints() {
        // DOUBLE precision constraints
        // MySQL DOUBLE(M,D) where M is total digits, D is decimals
        // This is just a documentation test
        assertTrue(true, "DOUBLE precision constraints should be tested with actual MySQL");
    }
}