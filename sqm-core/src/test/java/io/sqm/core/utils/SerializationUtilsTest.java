package io.sqm.core.utils;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SerializationUtilsTest {

    @Test
    void serializes_simple_value() {
        byte[] bytes = SerializationUtils.serialize("ok");
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }

    @Test
    void wraps_io_error_from_custom_serializable() {
        assertThrows(IllegalStateException.class, () -> SerializationUtils.serialize(new BrokenSerializable()));
    }

    @Test
    void private_constructor_is_invocable_via_reflection() throws Exception {
        Constructor<SerializationUtils> ctor = SerializationUtils.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        var instance = ctor.newInstance();
        assertNotNull(instance);
    }

    private static final class BrokenSerializable implements Serializable {
        @Serial
        @SuppressWarnings("unused")
        private void writeObject(ObjectOutputStream out) throws IOException {
            throw new IOException("boom");
        }
    }
}
