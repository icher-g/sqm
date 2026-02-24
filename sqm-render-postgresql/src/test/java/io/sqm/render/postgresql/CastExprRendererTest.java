package io.sqm.render.postgresql;

import io.sqm.core.*;
import io.sqm.render.postgresql.spi.PostgresDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PostgreSQL {@link CastExprRenderer}.
 */
@DisplayName("PostgreSQL CastExprRenderer Tests")
class CastExprRendererTest {

    private RenderContext renderContext;

    @BeforeEach
    void setUp() {
        renderContext = RenderContext.of(new PostgresDialect());
    }

    @Test
    @DisplayName("Render simple cast")
    void rendersSimpleCast() {
        var cast = CastExpr.of(col("a"), type("int"));
        var sql = renderContext.render(cast).sql();
        
        assertEquals("a::int", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render cast with qualified type")
    void rendersCastWithQualifiedType() {
        var cast = CastExpr.of(col("value"), type("pg_catalog", "int4"));
        var sql = renderContext.render(cast).sql();
        
        assertEquals("value::pg_catalog.int4", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render cast with array type")
    void rendersCastWithArrayType() {
        var arrayType = TypeName.of(QualifiedName.of(java.util.List.of("text")), null, java.util.List.of(), 1, TimeZoneSpec.NONE);
        var cast = CastExpr.of(col("vals"), arrayType);
        var sql = renderContext.render(cast).sql();
        
        assertEquals("vals::text[]", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render cast with multi-dimensional array type")
    void rendersCastWithMultiDimArrayType() {
        var arrayType = TypeName.of(QualifiedName.of(java.util.List.of("int")), null, java.util.List.of(), 2, TimeZoneSpec.NONE);
        var cast = CastExpr.of(col("matrix"), arrayType);
        var sql = renderContext.render(cast).sql();
        
        assertEquals("matrix::int[][]", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render cast with literal")
    void rendersCastWithLiteral() {
        var cast = CastExpr.of(lit("2024-01-01"), type("date"));
        var sql = renderContext.render(cast).sql();
        
        assertEquals("'2024-01-01'::date", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render cast with function")
    void rendersCastWithFunction() {
        var cast = CastExpr.of(func("now"), type("text"));
        var sql = renderContext.render(cast).sql();
        
        assertEquals("now()::text", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render nested cast")
    void rendersNestedCast() {
        var inner = CastExpr.of(col("value"), type("text"));
        var outer = CastExpr.of(inner, type("varchar").withModifiers(lit(10)));
        var sql = renderContext.render(outer).sql();
        
        assertEquals("value::text::varchar(10)", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render cast with arithmetic expression")
    void rendersCastWithArithmetic() {
        var expr = col("a").add(col("b"));
        var cast = CastExpr.of(expr, type("numeric"));
        var sql = renderContext.render(cast).sql();
        
        assertEquals("(a + b)::numeric", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render cast in SELECT list")
    void rendersCastInSelectList() {
        var query = select(CastExpr.of(col("id"), type("text")).as("id_str"))
            .from(tbl("t"))
            .build();
        
        var sql = renderContext.render(query).sql();
        
        assertTrue(normalizeWhitespace(sql).contains("id::text AS id_str"));
    }

    @Test
    @DisplayName("Render cast in WHERE clause")
    void rendersCastInWhereClause() {
        var query = select(col("*"))
            .from(tbl("t"))
            .where(CastExpr.of(col("id"), type("text")).eq(lit("123")))
            .build();
        
        var sql = renderContext.render(query).sql();
        
        assertTrue(normalizeWhitespace(sql).contains("id::text = '123'"));
    }

    @Test
    @DisplayName("Render cast with type modifiers")
    void rendersCastWithTypeModifiers() {
        var typeName = TypeName.of(
            QualifiedName.of(java.util.List.of("varchar")),
            null,
            java.util.List.of(lit(10)),
            0,
            TimeZoneSpec.NONE
        );
        var cast = CastExpr.of(col("name"), typeName);
        var sql = renderContext.render(cast).sql();
        
        assertEquals("name::varchar(10)", normalizeWhitespace(sql));
    }

    private String normalizeWhitespace(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}



