package io.sqm.render.postgresql;

import io.sqm.core.Lateral;
import io.sqm.core.TableRef;
import io.sqm.render.postgresql.spi.PostgresDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for PostgreSQL LATERAL rendering.
 */
@DisplayName("PostgreSQL LateralRenderer Tests")
class LateralRendererTest {

    private RenderContext renderContext;

    @BeforeEach
    void setUp() {
        renderContext = RenderContext.of(new PostgresDialect());
    }

    @Test
    @DisplayName("Render LATERAL with subquery")
    void rendersLateralWithSubquery() {
        var subquery = select(col("*"))
            .from(tbl("orders"))
            .where(col("user_id").eq(col("u", "id")))
            .build();

        var lateral = Lateral.of(TableRef.query(subquery).as("o"));
        var sql = renderContext.render(lateral).sql();

        assertTrue(normalizeWhitespace(sql).startsWith("LATERAL"));
        assertTrue(normalizeWhitespace(sql).contains("SELECT"));
    }

    @Test
    @DisplayName("Render LATERAL with function call")
    void rendersLateralWithFunction() {
        var func = func("unnest", arg(col("u", "tags")));
        var lateral = Lateral.of(TableRef.function(func).as("t").columnAliases("tag"));
        var sql = renderContext.render(lateral).sql();

        assertTrue(normalizeWhitespace(sql).startsWith("LATERAL"));
        assertTrue(normalizeWhitespace(sql).contains("unnest"));
    }

    @Test
    @DisplayName("Render LATERAL in full query context")
    void rendersLateralInFullQuery() {
        var subquery = select(col("*"))
            .from(tbl("orders"))
            .where(col("user_id").eq(col("u", "id")))
            .limit(1)
            .build();

        var query = select(col("u", "*"), col("o", "*"))
            .from(tbl("users").as("u"))
            .join(inner(Lateral.of(TableRef.query(subquery).as("o"))).on(unary(lit(true))))
            .build();

        var sql = renderContext.render(query).sql();

        assertTrue(normalizeWhitespace(sql).contains("LATERAL"));
        assertTrue(normalizeWhitespace(sql).contains("JOIN"));
    }

    @Test
    @DisplayName("Render LATERAL LEFT JOIN")
    void rendersLateralLeftJoin() {
        var subquery = select(col("*"))
            .from(tbl("orders"))
            .where(col("user_id").eq(col("u", "id")))
            .limit(1)
            .build();

        var query = select(col("*"))
            .from(tbl("users").as("u"))
            .join(left(Lateral.of(TableRef.query(subquery).as("o"))).on(unary(lit(true))))
            .build();

        var sql = renderContext.render(query).sql();

        assertTrue(normalizeWhitespace(sql).contains("LATERAL"));
        assertTrue(normalizeWhitespace(sql).contains("LEFT"));
    }

    @Test
    @DisplayName("Render LATERAL with VALUES")
    void rendersLateralWithValues() {
        var values = rows(row(col("u", "id"), col("u", "name")));
        var lateral = Lateral.of(TableRef.values(values).as("v").columnAliases("id", "name"));
        var sql = renderContext.render(lateral).sql();

        assertTrue(normalizeWhitespace(sql).startsWith("LATERAL"));
        assertTrue(normalizeWhitespace(sql).contains("VALUES"));
    }

    @Test
    @DisplayName("Render multiple LATERAL joins")
    void rendersMultipleLateralJoins() {
        var subq1 = select(col("*"))
            .from(tbl("orders"))
            .where(col("user_id").eq(col("u", "id")))
            .build();

        var subq2 = select(col("*"))
            .from(tbl("payments"))
            .where(col("order_id").eq(col("o", "id")))
            .build();

        var query = select(col("*"))
            .from(tbl("users").as("u"))
            .join(inner(Lateral.of(TableRef.query(subq1).as("o"))).on(unary(lit(true))))
            .join(inner(Lateral.of(TableRef.query(subq2).as("p"))).on(unary(lit(true))))
            .build();

        var sql = renderContext.render(query).sql();

        // Count occurrences of LATERAL
        int count = normalizeWhitespace(sql).split("LATERAL", -1).length - 1;
        assertEquals(2, count);
    }

    @Test
    @DisplayName("Render LATERAL with aggregate")
    void rendersLateralWithAggregate() {
        var subquery = select(func("COUNT", starArg()).as("order_count"))
            .from(tbl("orders"))
            .where(col("user_id").eq(col("u", "id")))
            .build();

        var query = select(col("u", "name"), col("cnt", "order_count"))
            .from(tbl("users").as("u"))
            .join(inner(Lateral.of(TableRef.query(subquery).as("cnt"))).on(unary(lit(true))))
            .build();

        var sql = renderContext.render(query).sql();

        assertTrue(normalizeWhitespace(sql).contains("LATERAL"));
        assertTrue(normalizeWhitespace(sql).contains("COUNT"));
    }

    private String normalizeWhitespace(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
