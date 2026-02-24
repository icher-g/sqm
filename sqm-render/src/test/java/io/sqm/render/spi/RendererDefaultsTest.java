package io.sqm.render.spi;

import io.sqm.core.Expression;
import io.sqm.core.Identifier;
import io.sqm.core.QuoteStyle;
import io.sqm.core.RowExpr;
import io.sqm.core.ValuesTable;
import io.sqm.render.RenderTestDialect;
import io.sqm.render.SqlWriter;
import io.sqm.render.defaults.DefaultSqlWriter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RendererDefaultsTest {

    @Test
    void renderAliasedWritesAliasAndColumns() {
        var dialect = new RenderTestDialect();
        var ctx = RenderContext.of(dialect);
        SqlWriter w = new DefaultSqlWriter(ctx);

        var values = ValuesTable.of(RowExpr.of(List.of(Expression.literal(1))))
            .as("v")
            .columnAliases("c1", "c2");

        new ValuesTableRenderer().renderAliased(values, ctx, w);

        assertEquals("AS \"v\"(\"c1\", \"c2\")", w.toText(List.of()).sql());
    }

    @Test
    void renderAliasedSkipsWhenNoAlias() {
        var dialect = new RenderTestDialect();
        var ctx = RenderContext.of(dialect);
        SqlWriter w = new DefaultSqlWriter(ctx);

        var values = ValuesTable.of(RowExpr.of(List.of(Expression.literal(1))));

        new ValuesTableRenderer().renderAliased(values, ctx, w);

        assertEquals("", w.toText(List.of()).sql());
    }

    @Test
    void renderIdentifierPreservesOrFallsBackByQuoterSupport() {
        var renderer = new ValuesTableRenderer();
        var preservingQuoter = new PreservingQuoter(true);
        var fallbackQuoter = new PreservingQuoter(false);

        assertEquals("`x`", renderer.renderIdentifier(Identifier.of("x", QuoteStyle.BACKTICK), preservingQuoter));
        assertEquals("\"x\"", renderer.renderIdentifier(Identifier.of("x", QuoteStyle.BACKTICK), fallbackQuoter));
        assertEquals("plain", renderer.renderIdentifier(Identifier.of("plain"), preservingQuoter));
    }

    private static final class ValuesTableRenderer implements Renderer<ValuesTable> {
        @Override
        public void render(ValuesTable node, RenderContext ctx, SqlWriter w) {
            w.append("VALUES");
        }

        @Override
        public Class<ValuesTable> targetType() {
            return ValuesTable.class;
        }
    }

    private static final class PreservingQuoter implements IdentifierQuoter {
        private final boolean supportBacktick;

        private PreservingQuoter(boolean supportBacktick) {
            this.supportBacktick = supportBacktick;
        }

        @Override
        public String quote(String identifier) {
            return "\"" + identifier + "\"";
        }

        @Override
        public String quote(String identifier, io.sqm.core.QuoteStyle quoteStyle) {
            if (quoteStyle == null || quoteStyle == io.sqm.core.QuoteStyle.NONE) {
                return quoteIfNeeded(identifier);
            }
            return switch (quoteStyle) {
                case BACKTICK -> "`" + identifier + "`";
                case DOUBLE_QUOTE -> "\"" + identifier + "\"";
                default -> throw new IllegalArgumentException("unsupported");
            };
        }

        @Override
        public String quoteIfNeeded(String identifier) {
            return identifier;
        }

        @Override
        public String qualify(String schemaOrNull, String name) {
            return schemaOrNull == null ? name : schemaOrNull + "." + name;
        }

        @Override
        public boolean needsQuoting(String identifier) {
            return false;
        }

        @Override
        public boolean supports(io.sqm.core.QuoteStyle quoteStyle) {
            if (quoteStyle == io.sqm.core.QuoteStyle.BACKTICK) {
                return supportBacktick;
            }
            return IdentifierQuoter.super.supports(quoteStyle);
        }
    }
}
