package io.sqm.codegen;

import io.sqm.core.OrderItem;
import io.sqm.core.Query;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqmJavaEmitterTest {

    private final SqmJavaEmitter emitter = new SqmJavaEmitter();

    @Test
    void emitQuery_coversMostSupportedNodes() {
        Query query = select(
            star(),
            star("u"),
            col("u", "name").as("user_name"),
            func("count", starArg())
                .distinct()
                .withinGroup(orderBy(order(col("u", "name")).asc()))
                .filter(col("u", "active").eq(lit(true)))
                .over(over(partition(col("u", "dept")), orderBy(order(col("u", "salary")).desc()), rows(preceding(1), currentRow()), excludeTies()))
                .as("cnt"),
            func("rank").over(over("w_ref")).as("rk"),
            param(),
            param("id"),
            param(2),
            row(lit(1), lit("x")),
            rows(row(lit(1), lit("a")), row(lit(2), lit("b"))),
            expr(select(star()).from(tbl("subq")).build())
        )
            .from(tbl("public", "users").as("u").only())
            .join(
                inner(tbl(select(star()).from(tbl("sub_src")).build()).as("sq").columnAliases("c1").lateral()).on(col("u", "id").eq(col("sq", "c1"))),
                left(tbl("orders").as("o")).on(col("o", "uid").eq(col("u", "id"))),
                right(tbl("right_t").as("r")).on(col("r", "uid").eq(col("u", "id"))),
                full(tbl("full_t").as("f")).on(col("f", "uid").eq(col("u", "id"))),
                cross(tbl("cross_t")),
                natural(tbl("natural_t")),
                inner(tbl("using_t")).using("uid")
            )
            .where(
                col("u", "name").like("%a%").escape("\\")
                    .and(col("u", "nick").notLike("%b%"))
                    .and(col("u", "nick2").ilike("%c%"))
                    .and(col("u", "nick3").notIlike("%d%"))
                    .and(col("u", "kind").similarTo("%e%"))
                    .and(col("u", "kind2").notSimilarTo("%f%"))
                    .and(col("u", "status").in(row(param("a"), param("b"))))
                    .and(col("u", "status").notIn(row(lit("x"), lit("y"))))
                    .and(col("u", "age").between(lit(1), lit(2)).symmetric(true).negated(true))
                    .and(col("u", "deleted").isNull())
                    .and(col("u", "created").isNotNull())
                    .and(exists(select(star()).from(tbl("ex")).build()))
                    .and(notExists(select(star()).from(tbl("nex")).build()))
                    .and(unary(lit(true)).not())
            )
            .groupBy(
                group(col("u", "dept")),
                group(1),
                groupingSet(group(col("u", "team")), group(2)),
                groupingSets(groupingSet(group(col("u", "x"))), groupingSet(group(3))),
                rollup(group(col("u", "r")), group(4)),
                cube(group(col("u", "c")), group(5))
            )
            .having(func("count", starArg()).gt(lit(0)))
            .window(
                window("w1", partition(col("u", "dept")), orderBy(order(col("u", "salary")).desc()), rows(preceding(2), following(2)), excludeCurrentRow())
            )
            .orderBy(
                order(col("u", "name")).asc().nullsFirst().collate("en_US").using(">"),
                order(1).desc().nullsLast()
            )
            .distinct(distinct())
            .limitOffset(limitAll(lit(10)))
            .lockFor(update(), ofTables("u"), true, false)
            .build();

        String source = emitter.emitQuery(query);
        assertTrue(source.contains("select("));
        assertTrue(source.contains("star(\"u\")"));
        assertTrue(source.contains(".withinGroup(orderBy("));
        assertTrue(source.contains(".filter("));
        assertTrue(source.contains(".over(over("));
        assertTrue(source.contains(".lateral()"));
        assertTrue(source.contains("left("));
        assertTrue(source.contains("right("));
        assertTrue(source.contains("full("));
        assertTrue(source.contains("cross("));
        assertTrue(source.contains("natural("));
        assertTrue(source.contains(".using(\"uid\")"));
        assertTrue(source.contains(".notLike("));
        assertTrue(source.contains(".ilike("));
        assertTrue(source.contains(".notIlike("));
        assertTrue(source.contains(".similarTo("));
        assertTrue(source.contains(".notSimilarTo("));
        assertTrue(source.contains(".between("));
        assertTrue(source.contains(".symmetric(true)"));
        assertTrue(source.contains(".negated(true)"));
        assertTrue(source.contains("exists("));
        assertTrue(source.contains("notExists("));
        assertTrue(source.contains("groupingSets("));
        assertTrue(source.contains("rollup("));
        assertTrue(source.contains("cube("));
        assertTrue(source.contains(".distinct(distinct())"));
        assertTrue(source.contains(".limitOffset(limitAll("));
        assertTrue(source.contains(".lockFor(update(), ofTables(\"u\"), true, false)"));
    }

    @Test
    void emitQuery_coversOverVariantsAndLimitOffsetVariants() {
        String overBaseOnly = emitter.emitQuery(select(func("f").over(overDef("base"))).from(tbl("t")).build());
        assertTrue(overBaseOnly.contains("overDef(\"base\")"));

        String overFrameExclude = emitter.emitQuery(
            select(func("f").over(over(orderBy(order(col("x"))), rows(unboundedPreceding(), currentRow()), excludeGroup()))).from(tbl("t")).build()
        );
        assertTrue(overFrameExclude.contains("excludeGroup()"));

        String overPartitionOnly = emitter.emitQuery(
            select(func("f").over(over(partition(col("p"))))).from(tbl("t")).build()
        );
        assertTrue(overPartitionOnly.contains("over(partition("));

        String limitOnly = emitter.emitQuery(select(star()).from(tbl("t")).limit(lit(5)).build());
        assertTrue(limitOnly.contains(".limit(lit(5))"));

        String offsetOnly = emitter.emitQuery(select(star()).from(tbl("t")).offset(lit(7)).build());
        assertTrue(offsetOnly.contains(".offset(lit(7))"));

        String bothNull = emitter.emitQuery(select(star()).from(tbl("t")).limitOffset(limitOffset(null, null)).build());
        assertTrue(bothNull.contains(".limitOffset(limitOffset(null, null))"));
    }

    @Test
    void emitQuery_formatsSupportedLiteralTypes() {
        String source = emitter.emitQuery(
            select(
                lit("text"),
                lit('x'),
                lit(1L),
                lit(1.5F),
                lit((short) 2),
                lit((byte) 3),
                lit(true),
                lit(4),
                lit(2.5),
                lit(null)
            ).from(tbl("t")).build()
        );

        assertTrue(source.contains("lit(\"text\")"));
        assertTrue(source.contains("lit('x')"));
        assertTrue(source.contains("lit(1L)"));
        assertTrue(source.contains("lit(1.5F)"));
        assertTrue(source.contains("lit((short)2)"));
        assertTrue(source.contains("lit((byte)3)"));
        assertTrue(source.contains("lit(true)"));
        assertTrue(source.contains("lit(4)"));
        assertTrue(source.contains("lit(2.5)"));
        assertTrue(source.contains("lit(null)"));
    }

    @Test
    void emitQuery_throwsOnUnsupportedLiteralType() {
        Query query = select(lit(new BigDecimal("1.25"))).from(tbl("t")).build();
        var error = assertThrows(IllegalStateException.class, () -> emitter.emitQuery(query));
        assertTrue(error.getMessage().contains("Unsupported literal value type"));
    }

    @Test
    void emitQuery_throwsOnOrderItemWithoutExprAndOrdinal() {
        OrderItem broken = OrderItem.of(null, null, null, null, null, null);
        Query query = select(star()).from(tbl("t")).orderBy(broken).build();

        var error = assertThrows(IllegalStateException.class, () -> emitter.emitQuery(query));
        assertTrue(error.getMessage().contains("Order item must have expression or ordinal"));
    }

    @Test
    void emitQuery_emitsTableInheritanceIncludingDescendants() {
        String source = emitter.emitQuery(select(star()).from(tbl("users").includingDescendants()).build());
        assertTrue(source.contains(".includingDescendants()"));
    }
}
