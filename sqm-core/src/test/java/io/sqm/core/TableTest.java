package io.sqm.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TableTest {

    @Test
    void of() {
        var table = tbl("t");
        assertEquals("t", table.name().value());
        assertNull(table.schema());
        assertEquals(Table.Inheritance.DEFAULT, table.inheritance());
        table = tbl("dbo", "t");
        assertEquals("t", table.name().value());
        assertEquals("dbo", table.schema().value());
        assertEquals(Table.Inheritance.DEFAULT, table.inheritance());
    }

    @Test
    void as() {
        var table = tbl("t").as("a");
        assertEquals("t", table.name().value());
        assertEquals("a", table.alias().value());
    }

    @Test
    void inSchema() {
        var table = tbl("t");
        assertEquals("t", table.name().value());
        assertNull(table.schema());
        table = table.inSchema("dbo");
        assertEquals("t", table.name().value());
        assertEquals("dbo", table.schema().value());
    }

    @Test
    void inheritance_flags() {
        var table = tbl("t").only();
        assertEquals(Table.Inheritance.ONLY, table.inheritance());
        table = table.includingDescendants();
        assertEquals(Table.Inheritance.INCLUDE_DESCENDANTS, table.inheritance());
    }

    @Test
    void inheritance_null_defaults() {
        var table = Table.of(Identifier.of("dbo"), Identifier.of("t"), Identifier.of("a"), null);
        assertEquals(Table.Inheritance.DEFAULT, table.inheritance());
    }

    @Test
    void preserves_identifier_quote_metadata() {
        var table = Table.of(
            Identifier.of("MySchema", QuoteStyle.DOUBLE_QUOTE),
            Identifier.of("User", QuoteStyle.DOUBLE_QUOTE),
            Identifier.of("u", QuoteStyle.BACKTICK),
            Table.Inheritance.DEFAULT
        );

        assertEquals("MySchema", table.schema().value());
        assertEquals("User", table.name().value());
        assertEquals("u", table.alias().value());
        assertEquals(QuoteStyle.DOUBLE_QUOTE, table.schema().quoteStyle());
        assertEquals(QuoteStyle.DOUBLE_QUOTE, table.name().quoteStyle());
        assertEquals(QuoteStyle.BACKTICK, table.alias().quoteStyle());
    }

    @Test
    void index_hints_are_immutable_and_preserved_by_mutators() {
        var table = tbl("users")
            .useIndex("idx_users_name")
            .ignoreIndex("idx_users_email")
            .withNoLock()
            .as("u")
            .inSchema("app");

        assertEquals(2, table.indexHints().size());
        assertEquals(1, table.lockHints().size());
        assertEquals(Table.IndexHintType.USE, table.indexHints().get(0).type());
        assertEquals(Table.IndexHintType.IGNORE, table.indexHints().get(1).type());
        assertThrows(UnsupportedOperationException.class, () -> table.indexHints().add(
            new Table.IndexHint(Table.IndexHintType.FORCE, Table.IndexHintScope.DEFAULT, List.of(Identifier.of("idx")))
        ));
        assertThrows(UnsupportedOperationException.class, () -> table.lockHints().add(Table.LockHint.holdlock()));
        assertEquals("u", table.alias().value());
        assertEquals("app", table.schema().value());
    }

    @Test
    void withIdentifierMutatorsPreserveIndexHints() {
        var table = tbl("users")
            .forceIndex("idx_users_id")
            .withUpdLock()
            .as(Identifier.of("u"))
            .inSchema(Identifier.of("app"));

        assertEquals("u", table.alias().value());
        assertEquals("app", table.schema().value());
        assertEquals(Table.IndexHintType.FORCE, table.indexHints().getFirst().type());
        assertEquals(Table.LockHintKind.UPDLOCK, table.lockHints().getFirst().kind());
    }

    @Test
    void ofTreatsNullIndexHintsAsEmptyList() {
        var table = Table.of(null, Identifier.of("users"), null, Table.Inheritance.DEFAULT, null);
        assertEquals(List.of(), table.indexHints());
        assertEquals(List.of(), table.lockHints());
    }

    @Test
    void ofTreatsNullSqlServerHintsAsEmptyList() {
        var table = Table.of(null, Identifier.of("users"), null, Table.Inheritance.DEFAULT, List.of(), null);

        assertEquals(List.of(), table.lockHints());
    }

    @Test
    void indexHintDefaultsNullScopeAndCopiesIndexes() {
        var indexes = new java.util.ArrayList<>(List.of(Identifier.of("idx_users_name")));
        var hint = new Table.IndexHint(Table.IndexHintType.USE, null, indexes);
        indexes.add(Identifier.of("idx_users_email"));

        assertEquals(Table.IndexHintScope.DEFAULT, hint.scope());
        assertEquals(1, hint.indexes().size());
        assertEquals("idx_users_name", hint.indexes().getFirst().value());
        assertThrows(UnsupportedOperationException.class, () -> hint.indexes().add(Identifier.of("idx_other")));
    }

    @Test
    void indexHintRejectsEmptyIndexes() {
        assertThrows(IllegalArgumentException.class,
            () -> new Table.IndexHint(Table.IndexHintType.USE, Table.IndexHintScope.DEFAULT, List.of()));
    }

    @Test
    void lockHintsAreImmutableAndPreservedByMutators() {
        var table = tbl("users")
            .withNoLock()
            .withHoldLock()
            .useIndex("idx_users_id")
            .as("u");

        assertEquals(2, table.lockHints().size());
        assertEquals(Table.LockHintKind.NOLOCK, table.lockHints().get(0).kind());
        assertEquals(Table.LockHintKind.HOLDLOCK, table.lockHints().get(1).kind());
        assertEquals(Table.IndexHintType.USE, table.indexHints().getFirst().type());
    }
}
