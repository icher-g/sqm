package io.sqm.core.utils;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.*;

class HashUtilsTest {

    @Test
    void sha256_hex_is_stable() {
        var hash = HashUtils.sha256Hex("abc".getBytes());
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", hash);
    }

    @Test
    void private_constructor_is_invocable_via_reflection() throws Exception {
        Constructor<HashUtils> ctor = HashUtils.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        var instance = ctor.newInstance();
        assertNotNull(instance);
    }
}
