package io.sqm.render.ansi;

import io.sqm.core.PowerArithmeticExpr;
import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.core.dialect.VersionedDialectCapabilities;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.defaults.DefaultSqlWriter;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.lit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link PowerArithmeticExprRenderer}.
 * <p>Exponentiation is not supported in ANSI SQL, so an {@link UnsupportedDialectFeatureException} should be thrown.</p>
 */
@DisplayName("ANSI PowerArithmeticExprRenderer Tests")
class PowerArithmeticExprRendererTest {

    private final RenderContext ctx = RenderContext.of(new AnsiDialect());
    private final RenderContext enabledCtx = RenderContext.of(new TestDialectWithExponentiation());
    private final PowerArithmeticExprRenderer renderer = new PowerArithmeticExprRenderer();

    @Test
    @DisplayName("Power expression throws UnsupportedDialectFeatureException")
    void powerExprThrowsException() {
        var expr = PowerArithmeticExpr.of(lit(2), lit(3));
        var writer = new DefaultSqlWriter(ctx);

        assertThrows(UnsupportedDialectFeatureException.class,
            () -> renderer.render(expr, ctx, writer),
            "Exponentiation should not be supported in ANSI SQL");
    }

    @Test
    @DisplayName("UnsupportedDialectFeatureException has descriptive message")
    void exceptionMessageIsDescriptive() {
        var expr = PowerArithmeticExpr.of(lit(2), lit(3));
        var writer = new DefaultSqlWriter(ctx);

        UnsupportedDialectFeatureException ex =
            assertThrows(UnsupportedDialectFeatureException.class,
                () -> renderer.render(expr, ctx, writer));

        String message = ex.getMessage();
        assertNotNull(message);
        assertTrue(message.toLowerCase().contains("exponent"));
    }

    @Test
    @DisplayName("Target type is PowerArithmeticExpr")
    void targetTypeIsPowerArithmeticExpr() {
        assertEquals(PowerArithmeticExpr.class, renderer.targetType());
    }

    @Test
    @DisplayName("Power expression renders when feature is enabled")
    void powerExprRendersWhenSupported() {
        var expr = PowerArithmeticExpr.of(lit(2), lit(3));
        var writer = new DefaultSqlWriter(enabledCtx);

        renderer.render(expr, enabledCtx, writer);

        var sql = writer.toText(java.util.List.of()).sql();
        assertEquals("2 ^ 3", sql.trim());
    }

    private static final class TestDialectWithExponentiation extends AnsiDialect {
        private DialectCapabilities capabilities;

        @Override
        public DialectCapabilities capabilities() {
            if (capabilities == null) {
                capabilities = VersionedDialectCapabilities.builder(SqlDialectVersion.of(2016))
                    .supports(SqlFeature.EXPONENTIATION_OPERATOR)
                    .build();
            }
            return capabilities;
        }
    }
}
