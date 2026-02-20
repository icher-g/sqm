package io.sqm.core.control;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DecisionResultTest {

    @Test
    void allow_factory() {
        var result = DecisionResult.allow();
        assertEquals(DecisionKind.ALLOW, result.kind());
        assertEquals(ReasonCode.NONE, result.reasonCode());
    }

    @Test
    void deny_factory() {
        var result = DecisionResult.deny(ReasonCode.DENY_DDL, "DDL is blocked");
        assertEquals(DecisionKind.DENY, result.kind());
        assertEquals(ReasonCode.DENY_DDL, result.reasonCode());
        assertEquals("DDL is blocked", result.message());
    }

    @Test
    void rewrite_factory() {
        var result = DecisionResult.rewrite(ReasonCode.REWRITE_LIMIT, "Added LIMIT", "select * from t limit 100");
        assertEquals(DecisionKind.REWRITE, result.kind());
        assertEquals(ReasonCode.REWRITE_LIMIT, result.reasonCode());
        assertEquals("select * from t limit 100", result.rewrittenSql());
    }

    @Test
    void allow_requires_none_reason_code() {
        assertThrows(IllegalArgumentException.class,
            () -> new DecisionResult(DecisionKind.ALLOW, ReasonCode.DENY_DDL, null, null));
    }

    @Test
    void rewrite_requires_rewritten_sql() {
        assertThrows(IllegalArgumentException.class,
            () -> DecisionResult.rewrite(ReasonCode.REWRITE_LIMIT, "Added LIMIT", " "));
    }

    @Test
    void non_rewrite_disallows_rewritten_sql() {
        assertThrows(IllegalArgumentException.class,
            () -> new DecisionResult(DecisionKind.DENY, ReasonCode.DENY_DML, "blocked", "select 1"));
    }

    @Test
    void serializable_roundtrip_preserves_equality() throws Exception {
        var original = DecisionResult.rewrite(
            ReasonCode.REWRITE_QUALIFICATION,
            "Qualified identifiers",
            "select app.users.id from app.users");

        var bytes = new ByteArrayOutputStream();
        try (var out = new ObjectOutputStream(bytes)) {
            out.writeObject(original);
        }

        DecisionResult restored;
        try (var in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            restored = (DecisionResult) in.readObject();
        }

        assertEquals(original, restored);
        assertEquals(original.hashCode(), restored.hashCode());
    }
}
