package ac.grim.grimac.manager.protection;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ServerBrandTest {

    @Test
    void encodesBrandWithVarIntLength() {
        byte[] expected = new byte[]{7, 'v', 'a', 'n', 'i', 'l', 'l', 'a'};
        assertArrayEquals(expected, ServerBrand.encodeBrand("vanilla"));
        String longName = "x".repeat(200); // length needs two VarInt bytes
        byte[] encoded = ServerBrand.encodeBrand(longName);
        assertEquals((byte) 0xC8, encoded[0]);
        assertEquals((byte) 0x01, encoded[1]);
        assertEquals(longName, new String(encoded, 2, 200, StandardCharsets.UTF_8));
    }

    @Test
    void removesServerSoftwareFromVersionName() {
        assertEquals("26.3", ServerBrand.cleanVersionName("Purpur 26.3"));
        assertEquals("26.3", ServerBrand.cleanVersionName("Paper 26.3"));
        assertEquals("1.8.x-26.3", ServerBrand.cleanVersionName("Velocity 1.8.x-26.3"));
        assertEquals("26.3", ServerBrand.cleanVersionName("26.3"));
        assertEquals("Paper", ServerBrand.cleanVersionName("Paper")); // never return an empty name
    }
}
