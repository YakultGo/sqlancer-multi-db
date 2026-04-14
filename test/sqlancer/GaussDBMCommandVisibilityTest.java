package sqlancer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class GaussDBMCommandVisibilityTest {

    @Test
    void gaussdbMCommandIsVisibleAndLegacyGaussdbIsNot() {
        List<DatabaseProvider<?, ?, ?>> providers = Main.getDBMSProviders();
        Set<String> names = providers.stream().map(DatabaseProvider::getDBMSName).collect(Collectors.toSet());

        assertTrue(names.contains("gaussdb-m"), "Expected gaussdb-m provider to be available");
        assertFalse(names.contains("gaussdb"), "Legacy gaussdb command must not be visible");
    }
}

