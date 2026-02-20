package io.sqm.core.control;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExecutionContextTest {

    @Test
    void of_minimal() {
        var context = ExecutionContext.of("postgresql", ExecutionMode.ANALYZE);
        assertEquals("postgresql", context.dialect());
        assertEquals(ExecutionMode.ANALYZE, context.mode());
    }

    @Test
    void rejects_blank_dialect() {
        assertThrows(IllegalArgumentException.class,
            () -> ExecutionContext.of(" ", ExecutionMode.EXECUTE));
    }

    @Test
    void rejects_null_mode() {
        assertThrows(NullPointerException.class,
            () -> ExecutionContext.of("postgresql", null));
    }

    @Test
    void serializable_roundtrip_preserves_equality() throws Exception {
        var original = ExecutionContext.of("postgresql", "alice", "tenant-a", ExecutionMode.EXECUTE);

        var bytes = new ByteArrayOutputStream();
        try (var out = new ObjectOutputStream(bytes)) {
            out.writeObject(original);
        }

        ExecutionContext restored;
        try (var in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            restored = (ExecutionContext) in.readObject();
        }

        assertEquals(original, restored);
        assertEquals(original.hashCode(), restored.hashCode());
    }
}
