package sqlancer.mysql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class MySQLOptionsTest {

    @Test
    void getSpecifiedEngines_emptyWhenNullOrBlank() {
        MySQLOptions opts = new MySQLOptions();
        opts.engines = null;
        assertTrue(opts.getSpecifiedEngines().isEmpty());

        opts.engines = "   ";
        assertTrue(opts.getSpecifiedEngines().isEmpty());
    }

    @Test
    void getSpecifiedEngines_parsesCommaSeparatedNames() {
        MySQLOptions opts = new MySQLOptions();
        opts.engines = "InnoDB, MyISAM,custom_engine";
        assertEquals(List.of("InnoDB", "MyISAM", "custom_engine"), opts.getSpecifiedEngines());
    }
}

