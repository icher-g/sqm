package io.sqm.codegen;

import io.sqm.core.*;
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
                straight(tbl("plan_locked").as("pl")).on(col("pl", "uid").eq(col("u", "id"))),
                cross(tbl("cross_t")),
                natural(tbl("natural_t")),
                inner(tbl("using_t")).using("uid"),
                straight(tbl("using_plan_locked")).using("uid")
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
        assertTrue(source.contains("builder.select("));
        assertTrue(source.contains("select("));
        assertTrue(source.contains("star(\"u\")"));
        assertTrue(source.contains(".withinGroup(orderBy("));
        assertTrue(source.contains(".filter("));
        assertTrue(source.contains(".over(over("));
        assertTrue(source.contains(".lateral()"));
        assertTrue(source.contains("left("));
        assertTrue(source.contains("right("));
        assertTrue(source.contains("full("));
        assertTrue(source.contains("straight("));
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
        assertTrue(overBaseOnly.contains("builder.select("));
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
    void emitQuery_coversTopSpecVariants() {
        String plainTop = emitter.emitQuery(
            select(star()).from(tbl("t")).top(lit(5)).build()
        );
        String topPercentWithTies = emitter.emitQuery(
            select(star()).from(tbl("t")).top(TopSpec.of(lit(10), true, true)).build()
        );

        assertTrue(plainTop.contains(".top(lit(5))"));
        assertTrue(topPercentWithTies.contains(".top(TopSpec.of(lit(10), true, true))"));
    }

    @Test
    void emitQuery_coversDistinctOnAndLimitAllWithoutOffsetVariants() {
        String distinctOnSource = emitter.emitQuery(
            select(col("id")).from(tbl("t")).distinct(distinctOn(col("id"))).build()
        );
        String limitAllSource = emitter.emitQuery(
            select(star()).from(tbl("t")).limitOffset(limitAll()).build()
        );

        assertTrue(distinctOnSource.contains(".distinct(col(\"id\"))"));
        assertTrue(limitAllSource.contains(".limitOffset(limitAll())"));
    }

    @Test
    void emitQuery_prefersSqlServerFunctionHelpersWhenAvailable() {
        var source = emitter.emitQuery(
            select(
                len(col("name")),
                dataLength(col("payload")),
                getDate(),
                dateAdd("day", lit(1), col("created_at")),
                dateDiff("day", col("created_at"), col("updated_at")),
                isNullFn(col("name"), lit("unknown")),
                stringAgg(col("name"), lit(",")).withinGroup(orderBy(order(col("name"))))
            )
                .from(tbl("users"))
                .build()
        );

        assertTrue(source.contains("len(col(\"name\"))"));
        assertTrue(source.contains("dataLength(col(\"payload\"))"));
        assertTrue(source.contains("getDate()"));
        assertTrue(source.contains("dateAdd(\"day\", lit(1), col(\"created_at\"))"));
        assertTrue(source.contains("dateDiff(\"day\", col(\"created_at\"), col(\"updated_at\"))"));
        assertTrue(source.contains("isNullFn(col(\"name\"), lit(\"unknown\"))"));
        assertTrue(source.contains("stringAgg(col(\"name\"), lit(\",\"))"));
    }

    @Test
    void emitQuery_fallsBackToGenericFunctionEmissionWhenSqlServerHelpersDoNotFit() {
        var source = emitter.emitQuery(
            select(
                func("GETDATE", arg(lit(1))),
                func("DATEADD", arg(col("datepart")), arg(lit(1)), arg(col("created_at"))),
                func("LEN", starArg())
            )
                .from(tbl("users"))
                .build()
        );

        assertTrue(source.contains("func(\"GETDATE\", arg(lit(1)))"));
        assertTrue(source.contains("func(\"DATEADD\", arg(col(\"datepart\")), arg(lit(1)), arg(col(\"created_at\")))"));
        assertTrue(source.contains("len(starArg())"));
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

    @Test
    void emitQuery_emitsSqlServerTableHintHelpers() {
        String source = emitter.emitQuery(select(star()).from(tbl("users").withNoLock().withUpdLock().withHoldLock()).build());

        assertTrue(source.contains(".withNoLock()"));
        assertTrue(source.contains(".withUpdLock()"));
        assertTrue(source.contains(".withHoldLock()"));
    }

    @Test
    void emitQuery_escapesIdentifierValuesFromTypedModelNodes() {
        Query query = select(
            col("t", "a").as("a\"b"),
            io.sqm.core.SelectItem.star(Identifier.of("q\\x"))
        )
            .from(tbl("public", "ta\"b").as("t"))
            .window(io.sqm.core.WindowDef.of(Identifier.of("w\"1"), over()))
            .build();

        String source = emitter.emitQuery(query);

        assertTrue(source.contains(".as(\"a\\\"b\")"));
        assertTrue(source.contains("star(\"q\\\\x\")"));
        assertTrue(source.contains("tbl(\"public\", \"ta\\\"b\")"));
        assertTrue(source.contains("window(\"w\\\"1\", over())"));
    }

    @Test
    void emitStatement_coversDmlStatements() {
        var insert = insert(tbl("users"))
            .ignore()
            .columns(id("id"), id("name", QuoteStyle.BACKTICK))
            .values(row(lit(1), lit("alice")))
            .result(inserted("id").as("new_id"))
            .build();
        var update = update(tbl("users"))
            .optimizerHint("MAX_EXECUTION_TIME(1000)")
            .set(set("u", "name", lit("alice")))
            .from(tbl("src"))
            .where(col("u", "id").eq(lit(1)))
            .result(resultInto(tbl("audit"), id("user_id")), insertedAll(), inserted("id").as("user_id"))
            .build();
        var delete = delete(tbl("users"))
            .optimizerHint("BKA(users)")
            .using(tbl("audit"))
            .where(col("users", "id").eq(col("audit", "user_id")))
            .result(deleted("id"))
            .build();

        var insertSource = emitter.emitStatement(insert);
        var updateSource = emitter.emitStatement(update);
        var deleteSource = emitter.emitStatement(delete);

        assertTrue(insertSource.contains("insert(tbl(\"users\"))"));
        assertTrue(insertSource.contains(".ignore()"));
        assertTrue(insertSource.contains(".columns(id(\"id\"), id(\"name\", QuoteStyle.BACKTICK))"));
        assertTrue(insertSource.contains(".values(row(lit(1), lit(\"alice\")))"));
        assertTrue(insertSource.contains(".result(inserted(id(\"id\")).as(id(\"new_id\")))"));

        assertTrue(updateSource.contains("update(tbl(\"users\"))"));
        assertTrue(updateSource.contains(".optimizerHints(java.util.List.of(\"MAX_EXECUTION_TIME(1000)\"))"));
        assertTrue(updateSource.contains(".set(set(QualifiedName.of(id(\"u\"), id(\"name\")), lit(\"alice\")))"));
        assertTrue(updateSource.contains(".from(tbl(\"src\"))"));
        assertTrue(updateSource.contains(".result(resultInto(tbl(\"audit\"), id(\"user_id\")), insertedAll(), inserted(id(\"id\")).as(id(\"user_id\")))"));

        assertTrue(deleteSource.contains("delete(tbl(\"users\"))"));
        assertTrue(deleteSource.contains(".optimizerHints(java.util.List.of(\"BKA(users)\"))"));
        assertTrue(deleteSource.contains(".using(tbl(\"audit\"))"));
        assertTrue(deleteSource.contains(".result(deleted(id(\"id\")))"));
    }

    @Test
    void emitQuery_usesGenericNodePathForNonSelectQueries() {
        Query query = compose(
            java.util.List.of(
                select(star()).from(tbl("t1")).build(),
                select(star()).from(tbl("t2")).build()
            ),
            java.util.List.of(io.sqm.core.SetOperator.UNION)
        );

        var error = assertThrows(IllegalStateException.class, () -> emitter.emitQuery(query));
        assertTrue(error.getMessage().contains("Unsupported node"));
    }

    @Test
    void emitStatement_coversInsertConflictVariants_and_additional_lock_modes() {
        var insertDoNothing = insert(tbl("users"))
            .replace()
            .columns(id("id"))
            .query(select(lit(1)).build())
            .onConflictDoNothing(id("id"))
            .build();
        var insertDoUpdate = insert(tbl("users"))
            .columns(id("id"), id("name"))
            .values(row(lit(1), lit("alice")))
            .onConflictDoUpdate(
                java.util.List.of(id("id")),
                java.util.List.of(set("name", lit("updated"))),
                col("name").isNotNull()
            )
            .build();
        var noKeyUpdateLock = select(star()).from(tbl("users")).lockFor(noKeyUpdate(), ofTables("users"), false, true).build();
        var shareLock = select(star()).from(tbl("users")).lockFor(share(), ofTables("users"), false, false).build();
        var keyShareLock = select(star()).from(tbl("users")).lockFor(keyShare(), ofTables("users"), true, false).build();

        var doNothingSource = emitter.emitStatement(insertDoNothing);
        var doUpdateSource = emitter.emitStatement(insertDoUpdate);

        assertTrue(doNothingSource.contains(".replace()"));
        assertTrue(doNothingSource.contains(".query(select("));
        assertTrue(doNothingSource.contains(".onConflictDoNothing(id(\"id\"))"));

        assertTrue(doUpdateSource.contains(".onConflictDoUpdate(java.util.List.of(id(\"id\")), java.util.List.of("));
        assertTrue(doUpdateSource.contains("set(id(\"name\"), lit(\"updated\"))"));
        assertTrue(doUpdateSource.contains("col(\"name\").isNotNull()"));

        assertTrue(emitter.emitQuery(noKeyUpdateLock).contains(".lockFor(noKeyUpdate(), ofTables(\"users\"), false, true)"));
        assertTrue(emitter.emitQuery(shareLock).contains(".lockFor(share(), ofTables(\"users\"), false, false)"));
        assertTrue(emitter.emitQuery(keyShareLock).contains(".lockFor(keyShare(), ofTables(\"users\"), true, false)"));
    }

    @Test
    void emitStatement_covers_generic_result_star_variants() {
        var insert = insert(tbl("users"))
            .columns(id("id"))
            .values(row(lit(1)))
            .result(star(), star("u"))
            .build();

        var source = emitter.emitStatement(insert);

        assertTrue(source.contains(".result(star(), star(id(\"u\")))"));
    }

    @Test
    void emitStatement_coversMergeStatements() {
        var mergeStatement = merge(tbl("users"))
            .source(tbl("src").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .top(top(5))
            .whenMatchedUpdate(col("s", "active").eq(lit(true)), java.util.List.of(set("name", col("s", "name"))))
            .whenMatchedDelete(col("s", "deleted").eq(lit(true)))
            .whenNotMatchedBySourceDelete(col("users", "active").eq(lit(false)))
            .whenNotMatchedInsert(col("s", "name").isNotNull(), java.util.List.of(id("id"), id("name")), row(col("s", "id"), col("s", "name")))
            .build();

        var source = emitter.emitStatement(mergeStatement);

        assertTrue(source.contains("merge(tbl(\"users\"))"));
        assertTrue(source.contains(".source(tbl(\"src\").as(\"s\"))"));
        assertTrue(source.contains(".top(lit(5L))"));
        assertTrue(source.contains(".whenMatchedUpdate(col(\"s\", \"active\").eq(lit(true)), set(id(\"name\"), col(\"s\", \"name\")))"));
        assertTrue(source.contains(".whenMatchedDelete(col(\"s\", \"deleted\").eq(lit(true)))"));
        assertTrue(source.contains(".whenNotMatchedBySourceDelete(col(\"users\", \"active\").eq(lit(false)))"));
        assertTrue(source.contains(".whenNotMatchedInsert(col(\"s\", \"name\").isNotNull(), java.util.List.of(id(\"id\"), id(\"name\")), row(col(\"s\", \"id\"), col(\"s\", \"name\")))"));
    }

    @Test
    void emitStatement_coversMergeVariantsWithoutColumnsAndWithResultClause() {
        var mergeStatement = merge(tbl("users"))
            .source(tbl("src").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenMatchedDelete()
            .whenNotMatchedInsert(row(col("s", "id")))
            .result(inserted("id"))
            .build();

        var source = emitter.emitStatement(mergeStatement);

        assertTrue(source.contains(".whenMatchedDelete()"));
        assertTrue(source.contains(".whenNotMatchedInsert(row(col(\"s\", \"id\")))"));
        assertTrue(source.contains(".result(inserted(id(\"id\")))"));
    }

    @Test
    void emitStatement_coversPredicateAwareMergeInsertWithoutColumns() {
        var mergeStatement = merge(tbl("users"))
            .source(tbl("src").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenNotMatchedInsert(col("s", "id").gt(lit(0)), row(col("s", "id")))
            .build();

        var source = emitter.emitStatement(mergeStatement);

        assertTrue(source.contains(".whenNotMatchedInsert(col(\"s\", \"id\").gt(lit(0)), row(col(\"s\", \"id\")))"));
    }

    @Test
    void emitStatement_coversMergeDoNothingVariants() {
        var mergeStatement = merge(tbl("users"))
            .source(tbl("src").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenMatchedDoNothing()
            .whenNotMatchedDoNothing(col("s", "id").gt(lit(0)))
            .whenNotMatchedBySourceDoNothing()
            .build();

        var source = emitter.emitStatement(mergeStatement);

        assertTrue(source.contains(".whenMatchedDoNothing()"));
        assertTrue(source.contains(".whenNotMatchedDoNothing(col(\"s\", \"id\").gt(lit(0)))"));
        assertTrue(source.contains(".whenNotMatchedBySourceDoNothing()"));
    }

    @Test
    void emitStatement_coversMergeTopSpecAndVarargBySourceUpdateVariants() {
        var mergeStatement = merge(tbl("users"))
            .source(tbl("src").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .top(TopSpec.of(lit(10), true, true))
            .whenNotMatchedBySourceUpdate(set("name", lit("archived")))
            .build();

        var source = emitter.emitStatement(mergeStatement);

        assertTrue(source.contains(".top(TopSpec.of(lit(10), true, true))"));
        assertTrue(source.contains(".whenNotMatchedBySourceUpdate(set(id(\"name\"), lit(\"archived\")))"));
    }

    @Test
    void emitQuery_covers_remaining_window_distinct_and_limit_variants() {
        var baseFrameOnly = emitter.emitQuery(
            select(func("f").over(over("base", rows(currentRow())))).from(tbl("t")).build()
        );
        var baseOrderOnly = emitter.emitQuery(
            select(func("f").over(over("base", orderBy(order(col("x")))))).from(tbl("t")).build()
        );
        var partitionFrameOnly = emitter.emitQuery(
            select(func("f").over(over(partition(col("p")), rows(currentRow())))).from(tbl("t")).build()
        );
        var plainExcludeNoOthers = emitter.emitQuery(
            select(func("f").over(over(orderBy(order(col("x"))), rows(currentRow()), excludeNoOthers()))).from(tbl("t")).build()
        );
        var distinctOn = emitter.emitQuery(
            select(star()).from(tbl("t")).distinct(distinctOn(col("t", "id"))).build()
        );
        var limitAllNoOffset = emitter.emitQuery(select(star()).from(tbl("t")).limitOffset(limitAll()).build());

        assertTrue(baseFrameOnly.contains("over(\"base\", rows(currentRow()))"));
        assertTrue(baseOrderOnly.contains("over(\"base\", orderBy("));
        assertTrue(partitionFrameOnly.contains("over(partition("));
        assertTrue(plainExcludeNoOthers.contains("excludeNoOthers()"));
        assertTrue(distinctOn.contains(".distinct(col(\"t\", \"id\"))"));
        assertTrue(limitAllNoOffset.contains(".limitOffset(limitAll())"));
    }
}

