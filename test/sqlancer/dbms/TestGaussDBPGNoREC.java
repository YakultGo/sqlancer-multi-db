package sqlancer.dbms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Test;

import sqlancer.Main;

public class TestGaussDBPGNoREC {

    @Test
    public void testNoREC() {
        assumeTrue(TestConfig.isEnvironmentTrue(TestConfig.GAUSSDB_PG_ENV));
        assertEquals(0,
                Main.executeMain(new String[] { "--random-seed", "0", "--timeout-seconds", TestConfig.SECONDS,
                        "--num-threads", "4", "--num-queries", TestConfig.NUM_QUERIES, "gaussdb-pg", "--oracle",
                        "NOREC" }));
    }
}