package net.noskillanticheat.velocity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ProxyBrandTest {

    @Test
    void encodesBrand() {
        assertArrayEquals(new byte[]{7, 'v', 'a', 'n', 'i', 'l', 'l', 'a'}, ProxyBrand.encodeBrand("vanilla"));
    }

    @Test
    void removesProxySoftwareFromVersion() {
        assertEquals("4.3.1", ProxyBrand.cleanVersionName("Velocity 4.3.1"));
        assertEquals("1.8.x-26.3", ProxyBrand.cleanVersionName("Velocity 1.8.x-26.3"));
        assertEquals("26.3", ProxyBrand.cleanVersionName("26.3"));
        assertEquals("Velocity", ProxyBrand.cleanVersionName("Velocity"));
    }
}
