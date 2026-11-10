package ac.grim.grimac.checks.impl.misc;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ClientSignaturesTest {
    private final ClientSignatures defaults = new ClientSignatures(ClientSignatures.DEFAULT_PATTERNS);

    @Test
    void knownHackedClientsMatchCaseInsensitive() {
        assertNotNull(defaults.match("Meteor-Client"));
        assertNotNull(defaults.match("meteor-client:main"));
        assertNotNull(defaults.match("LiquidBounce"));
        assertNotNull(defaults.match("baritone:cmd"));
    }

    @Test
    void legitClientsAndCommonModsDoNotMatch() {
        for (String legit : List.of("vanilla", "fabric", "quilt", "forge", "lunarclient:v2.18.3-2443,fabric",
                "Feather Fabric", "labymod", "sodium:main", "fabric-screen-handler-api-v1:open_screen",
                "minecraft:brand", "voicechat:secret", "xaerominimap:main", "iris:main")) {
            assertNull(defaults.match(legit), legit);
        }
    }

    @Test
    void invalidRegexIsReportedNotThrown() {
        ClientSignatures s = new ClientSignatures(List.of("ok", "(broken"));
        assertEquals(List.of("(broken"), s.invalidPatterns());
        assertNotNull(s.match("is ok"));
    }

    @Test
    void decodesBrandAndRegisterPayloads() {
        byte[] brandBody = "meteor-client".getBytes(StandardCharsets.UTF_8);
        byte[] brand = new byte[brandBody.length + 1];
        brand[0] = (byte) brandBody.length;
        System.arraycopy(brandBody, 0, brand, 1, brandBody.length);
        assertEquals("meteor-client", ClientSignatures.decodeBrand(brand));
        assertEquals("", ClientSignatures.decodeBrand(new byte[0]));

        byte[] register = "fabric:registry/sync\0sodium:main\0\0".getBytes(StandardCharsets.UTF_8);
        assertEquals(List.of("fabric:registry/sync", "sodium:main"), ClientSignatures.decodeRegister(register));
    }
}
