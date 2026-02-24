package io.sqm.render.postgresql;

import io.sqm.core.BinaryOperatorExpr;
import io.sqm.core.Identifier;
import io.sqm.core.OperatorName;
import io.sqm.core.QualifiedName;
import io.sqm.core.QuoteStyle;
import io.sqm.render.postgresql.spi.PostgresDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("PostgreSQL BinaryOperatorExprRenderer Tests")
class BinaryOperatorExprRendererTest {

    private final RenderContext renderContext = RenderContext.of(new PostgresDialect());

    @Test
    @DisplayName("Render OPERATOR(schema.op) preserves supported quote style")
    void rendersOperatorSyntaxWithQuotedSchema() {
        var expr = BinaryOperatorExpr.of(
            col("a"),
            OperatorName.operator(
                QualifiedName.of(Identifier.of("PgOps", QuoteStyle.DOUBLE_QUOTE)),
                "##"
            ),
            col("b")
        );

        var sql = renderContext.render(expr).sql();

        assertEquals("a OPERATOR(\"PgOps\".##) b", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render OPERATOR(schema.op) falls back unsupported quote style")
    void rendersOperatorSyntaxWithUnsupportedQuoteFallback() {
        var expr = BinaryOperatorExpr.of(
            col("a"),
            OperatorName.operator(
                QualifiedName.of(Identifier.of("PgOps", QuoteStyle.BACKTICK)),
                "##"
            ),
            col("b")
        );

        var sql = renderContext.render(expr).sql();

        assertEquals("a OPERATOR(\"PgOps\".##) b", normalizeWhitespace(sql));
    }

    private String normalizeWhitespace(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
