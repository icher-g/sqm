package io.sqm.parser.mysql;

import io.sqm.core.ExpressionHintArg;
import io.sqm.core.IdentifierHintArg;
import io.sqm.core.QualifiedNameHintArg;
import io.sqm.parser.mysql.spi.MySqlSpecs;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MySqlHintParserSupportTest {

    @Test
    void parsesMultipleHintsFromSingleCommentWithTypedArgs() {
        var ctx = ParseContext.of(new MySqlSpecs());

        var hints = MySqlHintParserSupport.parseCommentHints(
            "MAX_EXECUTION_TIME(1000) BKA(users) NO_RANGE_OPTIMIZATION(app.users)",
            ctx
        );

        assertEquals(3, hints.size());
        assertEquals("MAX_EXECUTION_TIME", hints.get(0).name().value());
        assertEquals(1000L, assertInstanceOf(ExpressionHintArg.class, hints.get(0).args().getFirst()).value()
            .matchExpression().literal(l -> l.value()).orElseThrow(() -> new AssertionError("expected literal hint arg")));
        assertEquals("BKA", hints.get(1).name().value());
        assertEquals("users", assertInstanceOf(IdentifierHintArg.class, hints.get(1).args().getFirst()).value().value());
        assertEquals(
            java.util.List.of("app", "users"),
            assertInstanceOf(QualifiedNameHintArg.class, hints.get(2).args().getFirst()).value().values()
        );
    }

    @Test
    void preservesMysqlSpecificRawAssignmentLikeHintArgs() {
        var ctx = ParseContext.of(new MySqlSpecs());

        var hints = MySqlHintParserSupport.parseCommentHints("SET_VAR(sort_buffer_size=16M)", ctx);

        assertEquals(1, hints.size());
        assertEquals("SET_VAR", hints.getFirst().name().value());
        assertEquals("sort_buffer_size=16M", assertInstanceOf(IdentifierHintArg.class, hints.getFirst().args().getFirst()).value().value());
    }

    @Test
    void parsesDecimalAndStringLiteralHintArgs() {
        var ctx = ParseContext.of(new MySqlSpecs());

        var hints = MySqlHintParserSupport.parseCommentHints("MAX_EXECUTION_TIME(12.5) QB_NAME('main')", ctx);

        assertEquals(2, hints.size());
        assertEquals(
            new BigDecimal("12.5"),
            assertInstanceOf(ExpressionHintArg.class, hints.getFirst().args().getFirst()).value()
                .matchExpression().literal(l -> l.value()).orElseThrow(() -> new AssertionError("expected decimal literal"))
        );
        assertEquals(
            "main",
            assertInstanceOf(ExpressionHintArg.class, hints.get(1).args().getFirst()).value()
                .matchExpression().literal(l -> l.value()).orElseThrow(() -> new AssertionError("expected string literal"))
        );
    }

    @Test
    void rejectsMissingHintArg() {
        var ctx = ParseContext.of(new MySqlSpecs());

        assertThrows(IllegalArgumentException.class, () -> MySqlHintParserSupport.parseCommentHints("BKA(,)", ctx));
    }

    @Test
    void mapsIndexHintNamesAndRejectsUnknownScopes() {
        assertEquals("USE_INDEX", MySqlHintParserSupport.indexHintName("USE", ""));
        assertEquals("FORCE_INDEX_FOR_ORDER_BY", MySqlHintParserSupport.indexHintName("FORCE", "ORDER_BY"));
        assertThrows(IllegalArgumentException.class, () -> MySqlHintParserSupport.indexHintName("USE", "WINDOW"));
    }
}
