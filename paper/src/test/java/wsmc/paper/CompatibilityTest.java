package wsmc.paper;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CompatibilityTest {

    @Test
    public void testVersionRange() {
        // Simple test to verify version logic concept
        String[] supportedVersions = {"1.20.1", "1.20.4", "1.21", "1.21.11"};
        for (String version : supportedVersions) {
            assertTrue(isSupported(version), "Version " + version + " should be supported");
        }
    }

    private boolean isSupported(String version) {
        // Logic mirroring gradle.properties range [1.20, 1.22)
        if (version.startsWith("1.20") || version.startsWith("1.21")) {
            return true;
        }
        return false;
    }
}
