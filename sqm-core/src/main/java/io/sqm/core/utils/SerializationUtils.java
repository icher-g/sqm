package io.sqm.core.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Utility helpers for Java object serialization.
 */
public final class SerializationUtils {
    private SerializationUtils() {
    }

    /**
     * Serializes value to byte array using Java serialization.
     *
     * @param value serializable value.
     * @return serialized bytes.
     */
    public static byte[] serialize(Serializable value) {
        try (var out = new ByteArrayOutputStream(); var objectOut = new ObjectOutputStream(out)) {
            objectOut.writeObject(value);
            objectOut.flush();
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to serialize object", ex);
        }
    }
}
