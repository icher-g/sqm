package io.sqm.parser.ansi;

import io.sqm.core.HierarchicalQueryClause;
import io.sqm.core.Query;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HierarchicalQueryParserTest {
    @Test
    void rejectsHierarchicalQuerySyntax() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var result = ctx.parse(Query.class, "SELECT id FROM categories CONNECT BY PRIOR id = parent_id");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Hierarchical queries are not supported"));
    }

    @Test
    void rejectsDirectHierarchicalQueryClauseParsing() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var result = ctx.parse(HierarchicalQueryClause.class, "CONNECT BY PRIOR id = parent_id");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Hierarchical queries are not supported"));
    }
}
