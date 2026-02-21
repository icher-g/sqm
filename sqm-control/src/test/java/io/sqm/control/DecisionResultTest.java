package io.sqm.control;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class DecisionResultTest {

    @Test
    void allow_factory() {
        var result = DecisionResult.allow();
        assertEquals(DecisionKind.ALLOW, result.kind());
        assertEquals(ReasonCode.NONE, result.reasonCode());
        assertNull(result.fingerprint());
    }

    @Test
    void deny_factory() {
        var result = DecisionResult.deny(ReasonCode.DENY_DDL, "DDL is blocked");
        assertEquals(DecisionKind.DENY, result.kind());
        assertEquals(ReasonCode.DENY_DDL, result.reasonCode());
        assertEquals("DDL is blocked", result.message());
        assertEquals("remove_ddl", result.guidance().suggestedAction());
        assertTrue(result.guidance().retryable());
    }

    @Test
    void rewrite_factory() {
        var result = DecisionResult.rewrite(ReasonCode.REWRITE_LIMIT, "Added LIMIT", "select * from t limit 100");
        assertEquals(DecisionKind.REWRITE, result.kind());
        assertEquals(ReasonCode.REWRITE_LIMIT, result.reasonCode());
        assertEquals("select * from t limit 100", result.rewrittenSql());
        assertNull(result.fingerprint());
    }

    @Test
    void allow_factory_with_fingerprint() {
        var result = DecisionResult.allow("abc");
        assertEquals("abc", result.fingerprint());
    }

    @Test
    void rewrite_factory_with_fingerprint() {
        var result = DecisionResult.rewrite(
            ReasonCode.REWRITE_LIMIT,
            "Added LIMIT",
            "select * from t limit 100",
            "abc"
        );
        assertEquals("abc", result.fingerprint());
    }

    @Test
    void allow_requires_none_reason_code() {
        assertThrows(IllegalArgumentException.class,
            () -> new DecisionResult(DecisionKind.ALLOW, ReasonCode.DENY_DDL, null, null, null, null));
    }

    @Test
    void rewrite_requires_rewritten_sql() {
        assertThrows(IllegalArgumentException.class,
            () -> DecisionResult.rewrite(ReasonCode.REWRITE_LIMIT, "Added LIMIT", " "));
    }

    @Test
    void non_rewrite_disallows_rewritten_sql() {
        assertThrows(IllegalArgumentException.class,
            () -> new DecisionResult(
                DecisionKind.DENY,
                ReasonCode.DENY_DML,
                "blocked",
                "select 1",
                null,
                ReasonGuidanceCatalog.forReason(ReasonCode.DENY_DML)));
    }

    @Test
    void deny_requires_guidance() {
        assertThrows(IllegalArgumentException.class,
            () -> new DecisionResult(DecisionKind.DENY, ReasonCode.DENY_DDL, "blocked", null, null, null));
    }

    @Test
    void non_deny_disallows_guidance() {
        assertThrows(IllegalArgumentException.class,
            () -> new DecisionResult(
                DecisionKind.ALLOW,
                ReasonCode.NONE,
                null,
                null,
                null,
                DecisionGuidance.retryable("hint", "act", "retry")));
    }

    @Test
    void blank_fingerprint_is_rejected() {
        assertThrows(IllegalArgumentException.class,
            () -> new DecisionResult(DecisionKind.ALLOW, ReasonCode.NONE, null, null, " ", null));
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
