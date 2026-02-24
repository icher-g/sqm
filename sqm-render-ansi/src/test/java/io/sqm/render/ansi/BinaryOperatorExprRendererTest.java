package io.sqm.render.ansi;

import io.sqm.core.BinaryOperatorExpr;
import io.sqm.core.Identifier;
import io.sqm.core.OperatorName;
import io.sqm.core.QualifiedName;
import io.sqm.core.QuoteStyle;
import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.defaults.DefaultSqlWriter;
import io.sqm.render.spi.Booleans;
import io.sqm.render.spi.IdentifierQuoter;
import io.sqm.render.spi.NullSorting;
import io.sqm.render.spi.Operators;
import io.sqm.render.spi.PaginationStyle;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.RenderersRepository;
import io.sqm.render.spi.SqlDialect;
import io.sqm.render.spi.ValueFormatter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.lit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BinaryOperatorExprRendererTest {

    @Test
    void rejectsCustomOperatorsWhenDialectDoesNotSupportFeature() {
        var renderer = new BinaryOperatorExprRenderer();
        var ctx = RenderContext.of(new NoCustomOperatorDialect(new AnsiDialect()));
        SqlWriter w = new DefaultSqlWriter(ctx);
        var node = BinaryOperatorExpr.of(lit(1), "+", lit(2));

        assertThrows(UnsupportedDialectFeatureException.class, () -> renderer.render(node, ctx, w));
    }

    @Test
    void rendersBareOperatorWithSupportingDialect() {
        var renderer = new BinaryOperatorExprRenderer();
        var ctx = RenderContext.of(new AnsiDialect());
        SqlWriter w = new DefaultSqlWriter(ctx);
        var node = BinaryOperatorExpr.of(lit(1), "##", lit(2));

        renderer.render(node, ctx, w);

        assertEquals("1 ## 2", w.toText(List.of()).sql());
    }

    @Test
    void rendersOperatorKeywordSyntaxAndFallsBackForUnsupportedQuoteStyle() {
        var renderer = new BinaryOperatorExprRenderer();
        var ctx = RenderContext.of(new AnsiDialect());
        SqlWriter w = new DefaultSqlWriter(ctx);
        var schema = QualifiedName.of(Identifier.of("pg_catalog", QuoteStyle.BACKTICK));
        var node = BinaryOperatorExpr.of(lit(1), OperatorName.operator(schema, "##"), lit(2));

        renderer.render(node, ctx, w);

        assertEquals("1 OPERATOR(\"pg_catalog\".##) 2", w.toText(List.of()).sql());
    }

    private record NoCustomOperatorDialect(SqlDialect delegate) implements SqlDialect {
        @Override public String name() { return delegate.name(); }
        @Override public IdentifierQuoter quoter() { return delegate.quoter(); }
        @Override public ValueFormatter formatter() { return delegate.formatter(); }
        @Override public Operators operators() { return delegate.operators(); }
        @Override public Booleans booleans() { return delegate.booleans(); }
        @Override public NullSorting nullSorting() { return delegate.nullSorting(); }
        @Override public PaginationStyle paginationStyle() { return delegate.paginationStyle(); }
        @Override public RenderersRepository renderers() { return delegate.renderers(); }
        @Override
        public DialectCapabilities capabilities() {
            return feature -> feature != SqlFeature.CUSTOM_OPERATOR && delegate.capabilities().supports(feature);
        }
    }
}
