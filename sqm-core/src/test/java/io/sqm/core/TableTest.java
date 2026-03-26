package io.sqm.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void hints_are_immutable_and_preserved_by_identifier_mutators() {
        var table = tbl("users")
            .useIndex("idx_users_name")
            .ignoreIndex("idx_users_email")
            .withNoLock()
            .as("u")
            .inSchema("app");

        assertEquals(3, table.hints().size());
        assertEquals("USE_INDEX", table.hints().get(0).name().value());
        assertEquals("IGNORE_INDEX", table.hints().get(1).name().value());
        assertEquals("NOLOCK", table.hints().get(2).name().value());
        assertThrows(UnsupportedOperationException.class, () -> table.hints().add(TableHint.of("HOLDLOCK")));
        assertEquals("u", table.alias().value());
        assertEquals("app", table.schema().value());
    }

    @Test
    void withIdentifierMutatorsPreserveHints() {
        var table = tbl("users")
            .forceIndex("idx_users_id")
            .withUpdLock()
            .as(Identifier.of("u"))
            .inSchema(Identifier.of("app"));

        assertEquals("u", table.alias().value());
        assertEquals("app", table.schema().value());
        assertEquals("FORCE_INDEX", table.hints().getFirst().name().value());
        assertEquals("UPDLOCK", table.hints().getLast().name().value());
    }

    @Test
    void ofTreatsNullHintsAsEmptyList() {
        var table = Table.of(null, Identifier.of("users"), null, Table.Inheritance.DEFAULT, null);
        assertEquals(List.of(), table.hints());
    }

    @Test
    void withHintsCopiesValues() {
        var table = tbl("users").withHints(List.of(
            TableHint.of("USE_INDEX", HintArg.identifier("idx_users_name")),
            TableHint.of("UPDLOCK")
        ));

        assertEquals(2, table.hints().size());
        assertEquals("USE_INDEX", table.hints().getFirst().name().value());
        assertEquals("UPDLOCK", table.hints().getLast().name().value());
        assertThrows(UnsupportedOperationException.class, () -> table.hints().add(TableHint.of("HOLDLOCK")));
    }

    @Test
    void withHintsTreatsNullAsEmptyList() {
        var table = tbl("users").withNoLock().useIndex("idx_users_id").withHints(null);

        assertTrue(table.hints().isEmpty());
    }

    @Test
    void addHintAppendsGenericHint() {
        var table = tbl("users")
            .withNoLock()
            .hint(TableHint.of("INDEX", HintArg.identifier("idx_new")));

        assertEquals(2, table.hints().size());
        assertEquals("NOLOCK", table.hints().getFirst().name().value());
        assertEquals("INDEX", table.hints().getLast().name().value());
    }

    @Test
    void hintAppendsGenericHint() {
        var table = tbl("users")
            .hint(TableHint.of("NOLOCK"))
            .hint(TableHint.of("HOLDLOCK"));

        assertEquals(2, table.hints().size());
        assertEquals("NOLOCK", table.hints().getFirst().name().value());
        assertEquals("HOLDLOCK", table.hints().getLast().name().value());
    }

    @Test
    void helperHintsCreateGenericTypedHintArgs() {
        var table = tbl("users")
            .withNoLock()
            .withHoldLock()
            .useIndex("idx_users_id")
            .as("u");

        assertEquals(3, table.hints().size());
        assertEquals("NOLOCK", table.hints().get(0).name().value());
        assertEquals("HOLDLOCK", table.hints().get(1).name().value());
        assertEquals("USE_INDEX", table.hints().get(2).name().value());
        var arg = (IdentifierHintArg) table.hints().get(2).args().getFirst();
        assertEquals("idx_users_id", arg.value().value());
    }

    @Test
    void hintConvenienceConversionUsesSingleHintArgMapping() {
        var table = tbl("users")
            .hint("USE_INDEX", "idx_users_name")
            .hint(Identifier.of("MAX_EXECUTION_TIME"), 1000)
            .hint("INDEX", QualifiedName.of("users", "idx_users_name"))
            .hint("QB_NAME", Identifier.of("main_qb"));

        assertEquals(4, table.hints().size());
        assertEquals("idx_users_name",
            assertInstanceOf(IdentifierHintArg.class, table.hints().get(0).args().getFirst()).value().value());
        assertEquals(1000,
            assertInstanceOf(LiteralExpr.class,
                assertInstanceOf(ExpressionHintArg.class, table.hints().get(1).args().getFirst()).value()).value());
        assertEquals(List.of("users", "idx_users_name"),
            assertInstanceOf(QualifiedNameHintArg.class, table.hints().get(2).args().getFirst()).value().values());
        assertEquals("main_qb",
            assertInstanceOf(IdentifierHintArg.class, table.hints().get(3).args().getFirst()).value().value());
    }
}
