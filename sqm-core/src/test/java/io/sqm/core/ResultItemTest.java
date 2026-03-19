package io.sqm.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.deletedAll;
import static io.sqm.dsl.Dsl.star;
import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResultItemTest {

    @Test
    void fromNodes_supports_expression_select_and_result_variants() {
        List<ResultItem> items = ResultItem.fromNodes(
            col("id"),
            col("name").as("user_name"),
            star(),
            star("u"),
            deletedAll()
        );

        assertEquals(5, items.size());
        assertInstanceOf(ExprResultItem.class, items.get(0));
        assertEquals("user_name", items.get(1).matchResultItem().expr(item -> item.alias().value()).orElse(null));
        assertInstanceOf(StarResultItem.class, items.get(2));
        assertEquals("u", items.get(3).matchResultItem().qualifiedStar(item -> item.qualifier().value()).orElse(null));
        assertEquals(OutputRowSource.DELETED, items.get(4).matchResultItem().outputStar(OutputStarResultItem::source).orElse(null));
    }

    @Test
    void fromNodes_rejects_unsupported_nodes() {
        var error = assertThrows(IllegalStateException.class, () -> ResultItem.fromNodes(tbl("users")));
        org.junit.jupiter.api.Assertions.assertTrue(error.getMessage().contains("The provided node is not supported in the result clause"));
    }

    @Test
    void helper_factories_and_matcher_paths_are_reachable() {
        assertEquals("expr", ResultItem.fromNodes(col("id")).getFirst().matchResultItem()
            .star(ignore -> "star")
            .expr(ignore -> "expr")
            .otherwise(ignore -> "fallback"));

        assertEquals("star", ResultItem.fromNodes(star()).getFirst().matchResultItem()
            .star(ignore -> "star")
            .otherwise(ignore -> "fallback"));

        assertEquals("qualified", ResultItem.fromNodes(star("u")).getFirst().matchResultItem()
            .qualifiedStar(ignore -> "qualified")
            .otherwise(ignore -> "fallback"));

        assertEquals("inserted", ResultItem.fromNodes(io.sqm.dsl.Dsl.insertedAll()).getFirst().matchResultItem()
            .outputStar(item -> item.source().name().toLowerCase())
            .otherwise(ignore -> "fallback"));

        assertEquals("deleted", ResultItem.fromNodes(deletedAll()).getFirst().matchResultItem()
            .outputStar(item -> item.source().name().toLowerCase())
            .otherwise(ignore -> "fallback"));

        assertEquals("fallback", ResultItem.fromNodes(star()).getFirst().matchResultItem()
            .expr(ignore -> "expr")
            .otherwise(ignore -> "fallback"));
    }
}
