package io.cherlabs.sqlmodel.dsl;

import io.cherlabs.sqlmodel.core.Query;
import io.cherlabs.sqlmodel.render.DefaultSqlWriter;
import io.cherlabs.sqlmodel.render.SqlText;
import io.cherlabs.sqlmodel.render.ansi.spi.AnsiRenderContext;
import io.cherlabs.sqlmodel.render.spi.RenderContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.cherlabs.sqlmodel.dsl.DSL.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Rendering the DSL examples through the ANSI renderer.
 * The assertions normalize whitespace so renderer formatting changes won’t cause flakes.
 */
public class DslTest {

    private RenderContext ctx;

    private static void assertSql(String actual, String expected) {
        assertEquals(norm(expected), norm(actual), () ->
                "Expected:\n" + expected + "\n\nActual:\n" + actual);
    }

    private static String norm(String s) {
        return s.replaceAll("\\s+", " ").trim();
    }

    @BeforeEach
    void setUp() {
        this.ctx = new AnsiRenderContext();                       // <-- adapt to your actual context factory
    }

    @Test
    @DisplayName("1) Simple SELECT … FROM")
    void simpleSelectFrom() {
        Query q = q()
                .select(c("u", "id"), c("u", "name"))
                .from(t("users").as("u"));

        SqlText out = render(q);
        assertSql(
                out.sql(),
                "SELECT u.id, u.name FROM users AS u"
        );
        assertTrue(out.params().isEmpty(), "No params expected");
    }

    @Test
    @DisplayName("2) WHERE + ORDER BY + LIMIT/OFFSET")
    void whereOrderLimitOffset() {
        Query q = q()
                .select(c("u", "id"), c("u", "name"))
                .from(t("users").as("u"))
                .where(eq(c("u", "active"), true))
                .orderBy(asc(c("u", "name")))
                .limit(10)
                .offset(20);

        SqlText out = render(q);
        assertSql(
                out.sql(),
                "SELECT u.id, u.name " +
                        "FROM users AS u " +
                        "WHERE u.active = TRUE " +
                        "ORDER BY u.name ASC " +
                        "OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY"
        );
    }

    @Test
    @DisplayName("3) Aggregation + GROUP BY + HAVING")
    void groupByHaving() {
        Query q = q()
                .select(func("count", star()).as("cnt"), c("o", "status"))
                .from(t("orders").as("o"))
                .groupBy(g(c("o", "status")))
                .having(gt(func("count", star()), 10));

        SqlText out = render(q);
        assertSql(
                out.sql(),
                "SELECT count(*) AS cnt, o.status " +
                        "FROM orders AS o " +
                        "GROUP BY o.status " +
                        "HAVING count(*) > 10"
        );
    }

    @Test
    @DisplayName("4) INNER JOIN with predicate + WHERE")
    void innerJoinWithWhere() {
        Query q = q()
                .select(c("u", "id"), c("u", "name"), c("o", "total"))
                .from(t("users").as("u"))
                .join(inner(t("orders").as("o")).on(eq(c("u", "id"), c("o", "user_id"))))
                .where(gt(c("o", "total"), 100));

        SqlText out = render(q);
        assertSql(
                out.sql(),
                "SELECT u.id, u.name, o.total " +
                        "FROM users AS u " +
                        "INNER JOIN orders AS o ON u.id = o.user_id " +
                        "WHERE o.total > 100"
        );
    }

    // ---------- helpers ----------

    @Test
    @DisplayName("5) CASE expression in SELECT list")
    void caseExpression() {
        Query q = q()
                .select(
                        c("p", "id"),
                        kase(
                                when(eq(c("p", "status"), "A")).then(val("Active")),
                                when(eq(c("p", "status"), "I")).then(val("Inactive"))
                        ).elseValue(val("Unknown")).as("status_text")
                )
                .from(t("products").as("p"));

        SqlText out = render(q);
        assertSql(
                out.sql(),
                "SELECT p.id, " +
                        "CASE " +
                        "WHEN p.status = 'A' THEN 'Active' " +
                        "WHEN p.status = 'I' THEN 'Inactive' " +
                        "ELSE 'Unknown' " +
                        "END AS status_text " +
                        "FROM products AS p"
        );
    }

    @Test
    @DisplayName("6) Tuple IN")
    void tupleIn() {
        Query q = q()
                .select(c("id"), c("first_name"), c("last_name"))
                .from(t("person"))
                .where(in(
                        List.of(c("first_name"), c("last_name")),
                        List.of(
                                List.of("John", "Doe"),
                                List.of("Jane", "Smith")
                        )
                ));

        SqlText out = render(q);
        // Depending on your placeholders strategy, the VALUES may be literal or parameterized.
        // Here we assert the literal form; if you emit placeholders, adjust expected accordingly.
        assertSql(
                out.sql(),
                "SELECT id, first_name, last_name " +
                        "FROM person " +
                        "WHERE (first_name, last_name) IN (('John', 'Doe'), ('Jane', 'Smith'))"
        );
    }

    private SqlText render(Query q) {
        var w = new DefaultSqlWriter(ctx);
        w.append(q);
        return w.toText(List.of());
    }
}
