package io.sqm.render.mysql;

import io.sqm.core.Expression;
import io.sqm.core.RegexMode;
import io.sqm.core.RegexPredicate;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.mysql.spi.MySqlDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MySqlRegexPredicateRendererTest {

    @Test
    void rendersRegexpPredicate() {
        var ctx = RenderContext.of(new MySqlDialect());
        var predicate = RegexPredicate.of(RegexMode.MATCH, col("name"), Expression.literal("^a"), false);

        assertEquals("name REGEXP '^a'", ctx.render(predicate).sql());
    }

    @Test
    void rendersNotRegexpPredicate() {
        var ctx = RenderContext.of(new MySqlDialect());
        var predicate = RegexPredicate.of(RegexMode.MATCH, col("name"), Expression.literal("^a"), true);

        assertEquals("name NOT REGEXP '^a'", ctx.render(predicate).sql());
    }

    @Test
    void rejectsUnsupportedRegexMode() {
        var ctx = RenderContext.of(new MySqlDialect());
        var predicate = RegexPredicate.of(RegexMode.MATCH_INSENSITIVE, col("name"), Expression.literal("^a"), false);

        assertThrows(UnsupportedDialectFeatureException.class, () -> ctx.render(predicate));
    }
}


