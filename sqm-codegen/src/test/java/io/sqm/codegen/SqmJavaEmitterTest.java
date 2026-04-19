package io.sqm.codegen;

import io.sqm.core.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class SqmJavaEmitterTest {

    private final SqmJavaEmitter emitter = new SqmJavaEmitter();

    @Test
    void emit_coversMostSupportedNodes() {
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

        String source = emitter.emit(query);
        assertTrue(source.contains("select("));
        assertTrue(source.contains("select("));
        assertTrue(source.contains("star(\"u\")"));
        assertTrue(source.contains(".withinGroup("));
        assertTrue(source.contains("orderBy("));
        assertTrue(source.contains(".filter("));
        assertTrue(source.contains(".over("));
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
    void emit_coversOverVariantsAndLimitOffsetVariants() {
        String overBaseOnly = emitter.emit(select(func("f").over(overDef("base"))).from(tbl("t")).build());
        assertTrue(overBaseOnly.contains("select("));
        assertTrue(overBaseOnly.contains(".over("));
        assertTrue(overBaseOnly.contains("\"base\""));

        String overFrameExclude = emitter.emit(
            select(func("f").over(over(orderBy(order(col("x"))), rows(unboundedPreceding(), currentRow()), excludeGroup()))).from(tbl("t")).build()
        );
        assertTrue(overFrameExclude.contains("excludeGroup()"));

        String overPartitionOnly = emitter.emit(
            select(func("f").over(over(partition(col("p"))))).from(tbl("t")).build()
        );
        assertTrue(overPartitionOnly.contains(".over("));
        assertTrue(overPartitionOnly.contains("partition("));

        String limitOnly = emitter.emit(select(star()).from(tbl("t")).limit(lit(5)).build());
        assertTrue(limitOnly.contains(".limit(lit(5))"));

        String offsetOnly = emitter.emit(select(star()).from(tbl("t")).offset(lit(7)).build());
        assertTrue(offsetOnly.contains(".offset(lit(7))"));

        String bothNull = emitter.emit(select(star()).from(tbl("t")).limitOffset(limitOffset(null, null)).build());
        assertTrue(bothNull.contains(".limitOffset(limitOffset(null, null))"));
    }

    @Test
    void emit_coversTopSpecVariants() {
        String plainTop = emitter.emit(
            select(star()).from(tbl("t")).top(lit(5)).build()
        );
        String topPercentWithTies = emitter.emit(
            select(star()).from(tbl("t")).top(TopSpec.of(lit(10), true, true)).build()
        );

        assertTrue(plainTop.contains(".top(lit(5))"));
        assertTrue(topPercentWithTies.contains(".top(lit(10), true, true)"));
    }

    @Test
    void emit_coversDistinctOnAndLimitAllWithoutOffsetVariants() {
        String distinctOnSource = emitter.emit(
            select(col("id")).from(tbl("t")).distinct(distinctOn(col("id"))).build()
        );
        String limitAllSource = emitter.emit(
            select(star()).from(tbl("t")).limitOffset(limitAll()).build()
        );

        assertTrue(distinctOnSource.contains(".distinct(col(\"id\"))"));
        assertTrue(limitAllSource.contains(".limitOffset(limitAll())"));
    }

    @Test
    void emit_coversAtTimeZoneExpr() {
        String source = emitter.emit(
            select(col("u", "created_at").atTimeZone(lit("UTC")).as("created_utc"))
                .from(tbl("users").as("u"))
                .build()
        );

        assertTrue(source.contains("col(\"u\", \"created_at\").atTimeZone(lit(\"UTC\"))"));
    }

    @Test
    void emit_prefersSqlServerFunctionHelpersWhenAvailable() {
        var source = emitter.emit(
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
    void emit_fallsBackToGenericFunctionEmissionWhenSqlServerHelpersDoNotFit() {
        var source = emitter.emit(
            select(
                func("LEN"),
                func("ISNULL", arg(col("name"))),
                func("GETDATE", arg(lit(1))),
                func("DATEADD", arg(col("datepart")), arg(lit(1)), arg(col("created_at"))),
                func("LEN", starArg())
            )
                .from(tbl("users"))
                .build()
        );

        assertTrue(source.contains("func(\"LEN\")"));
        assertTrue(source.contains("func(\"ISNULL\", arg(col(\"name\")))"));
        assertTrue(source.contains("func(\"GETDATE\", arg(lit(1)))"));
        assertTrue(source.contains("func(\"DATEADD\", arg(col(\"datepart\")), arg(lit(1)), arg(col(\"created_at\")))"));
        assertTrue(source.contains("len(starArg())"));
    }

    @Test
    void emit_formatsSupportedLiteralTypes() {
        String source = emitter.emit(
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
    void emit_throwsOnUnsupportedLiteralType() {
        Query query = select(lit(new BigDecimal("1.25"))).from(tbl("t")).build();
        var error = assertThrows(IllegalStateException.class, () -> emitter.emit(query));
        assertTrue(error.getMessage().contains("Unsupported literal value type"));
    }

    @Test
    void emit_throwsOnOrderItemWithoutExprAndOrdinal() {
        OrderItem broken = OrderItem.of(null, null, null, null, null, null);
        Query query = select(star()).from(tbl("t")).orderBy(broken).build();

        var error = assertThrows(IllegalStateException.class, () -> emitter.emit(query));
        assertTrue(error.getMessage().contains("Order item must have expression or ordinal"));
    }

    @Test
    void emit_emitsTableInheritanceIncludingDescendants() {
        String source = emitter.emit(select(star()).from(tbl("users").includingDescendants()).build());
        assertTrue(source.contains(".includingDescendants()"));
    }

    @Test
    void emit_emitsSqlServerTableHintHelpers() {
        String source = emitter.emit(select(star()).from(tbl("users").withNoLock().withUpdLock().withHoldLock()).build());

        assertTrue(source.contains(".withNoLock()"));
        assertTrue(source.contains(".withUpdLock()"));
        assertTrue(source.contains(".withHoldLock()"));
    }

    @Test
    void emit_escapesIdentifierValuesFromTypedModelNodes() {
        Query query = select(
            col("t", "a").as("a\"b"),
            io.sqm.core.SelectItem.star(Identifier.of("q\\x"))
        )
            .from(tbl("public", "ta\"b").as("t"))
            .window(io.sqm.core.WindowDef.of(Identifier.of("w\"1"), over()))
            .build();

        String source = emitter.emit(query);

        assertTrue(source.contains(".as(\"a\\\"b\")"));
        assertTrue(source.contains("star(\"q\\\\x\")"));
        assertTrue(source.contains("tbl(\"public\", \"ta\\\"b\")"));
        assertTrue(source.contains("window(\"w\\\"1\", over())"));
    }

    @Test
    void emit_coversDmlStatements() {
        var insert = insert(tbl("users"))
            .hint("APPEND")
            .ignore()
            .columns(id("id"), id("name", QuoteStyle.BACKTICK))
            .values(row(lit(1), lit("alice")))
            .result(inserted("id").as("new_id"))
            .build();
        var update = update(tbl("users"))
            .hint("MAX_EXECUTION_TIME", 1000)
            .set(set("u", "name", lit("alice")))
            .from(tbl("src"))
            .where(col("u", "id").eq(lit(1)))
            .result(resultInto(tableVar("audit"), id("user_id")), insertedAll(), inserted("id").as("user_id"))
            .build();
        var delete = delete(tbl("users"))
            .hint("BKA", "users")
            .using(tbl("audit"))
            .where(col("users", "id").eq(col("audit", "user_id")))
            .result(deleted("id"))
            .build();

        var insertSource = emitter.emit(insert);
        var updateSource = emitter.emit(update);
        var deleteSource = emitter.emit(delete);

        assertTrue(insertSource.contains("insert(tbl(\"users\"))"));
        assertTrue(insertSource.contains("\n.ignore()"));
        assertTrue(insertSource.contains("\n.columns("));
        assertTrue(insertSource.contains("\n.values("));
        assertTrue(insertSource.contains("\n.result("));
        assertTrue(insertSource.contains("\n.build()"));
        assertTrue(insertSource.contains(".hint(\"APPEND\")"));
        assertTrue(insertSource.contains(".ignore()"));
        assertTrue(insertSource.contains(".columns(id(\"id\"), id(\"name\", QuoteStyle.BACKTICK))"));
        assertTrue(insertSource.contains(".values(row(lit(1), lit(\"alice\")))"));
        assertTrue(insertSource.contains(".result(inserted(id(\"id\")).as(id(\"new_id\")))"));

        assertTrue(updateSource.contains("update(tbl(\"users\"))"));
        assertTrue(updateSource.contains("\n.set("));
        assertTrue(updateSource.contains("\n.from("));
        assertTrue(updateSource.contains("\n.where("));
        assertTrue(updateSource.contains("\n.result("));
        assertTrue(updateSource.contains("\n.build()"));
        assertTrue(updateSource.contains(".hint(\"MAX_EXECUTION_TIME\", 1000)"));
        assertTrue(updateSource.contains(".set(\"u\", \"name\", lit(\"alice\"))"));
        assertTrue(updateSource.contains(".from(tbl(\"src\"))"));
        assertTrue(updateSource.contains(".result(resultInto(tableVar(\"audit\"), id(\"user_id\")), insertedAll(), inserted(id(\"id\")).as(id(\"user_id\")))"));

        assertTrue(deleteSource.contains("delete(tbl(\"users\"))"));
        assertTrue(deleteSource.contains("\n.using("));
        assertTrue(deleteSource.contains("\n.where("));
        assertTrue(deleteSource.contains("\n.result("));
        assertTrue(deleteSource.contains("\n.build()"));
        assertTrue(deleteSource.contains(".hint(\"BKA\", \"users\")"));
        assertTrue(deleteSource.contains(".using(tbl(\"audit\"))"));
        assertTrue(deleteSource.contains(".result(deleted(id(\"id\")))"));
    }

    @Test
    void emit_compose() {
        Query query = compose(
            List.of(
                select(star()).from(tbl("t1")).build(),
                select(star()).from(tbl("t2")).build()
            ),
            List.of(SetOperator.UNION)
        );

        var code = emitter.emit(query);
        assertTrue(code.contains("compose("));
        assertTrue(code.contains(".from(tbl(\"t1\"))\n"));
        assertTrue(code.contains(".from(tbl(\"t2\"))\n"));
        assertTrue(code.contains("SetOperator.UNION"));
    }

    @Test
    void emit_coversInsertConflictVariants_and_additional_lock_modes() {
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
                List.of(id("id")),
                List.of(set("name", lit("updated"))),
                col("name").isNotNull()
            )
            .build();
        var noKeyUpdateLock = select(star()).from(tbl("users")).lockFor(noKeyUpdate(), ofTables("users"), false, true).build();
        var shareLock = select(star()).from(tbl("users")).lockFor(share(), ofTables("users"), false, false).build();
        var keyShareLock = select(star()).from(tbl("users")).lockFor(keyShare(), ofTables("users"), true, false).build();

        var doNothingSource = emitter.emit(insertDoNothing);
        var doUpdateSource = emitter.emit(insertDoUpdate);

        assertTrue(doNothingSource.contains(".replace()"));
        assertTrue(doNothingSource.contains(".query(select("));
        assertTrue(doNothingSource.contains(".onConflictDoNothing(id(\"id\"))"));

        assertTrue(doUpdateSource.contains(".onConflictDoUpdate(List.of(id(\"id\")), List.of("));
        assertTrue(doUpdateSource.contains("set(id(\"name\"), lit(\"updated\"))"));
        assertTrue(doUpdateSource.contains("col(\"name\").isNotNull()"));

        assertTrue(emitter.emit(noKeyUpdateLock).contains(".lockFor(noKeyUpdate(), ofTables(\"users\"), false, true)"));
        assertTrue(emitter.emit(shareLock).contains(".lockFor(share(), ofTables(\"users\"), false, false)"));
        assertTrue(emitter.emit(keyShareLock).contains(".lockFor(keyShare(), ofTables(\"users\"), true, false)"));
    }

    @Test
    void emit_covers_generic_result_star_variants() {
        var insert = insert(tbl("users"))
            .columns(id("id"))
            .values(row(lit(1)))
            .result(star(), star("u"))
            .build();

        var source = emitter.emit(insert);

        assertTrue(source.contains(".result(star(), star(id(\"u\")))"));
    }

    @Test
    void emit_coversMergeStatements() {
        var mergeStatement = merge(tbl("users"))
            .hint("MERGE_HINT")
            .source(tbl("src").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .top(top(5))
            .whenMatchedUpdate(col("s", "active").eq(lit(true)), List.of(set("name", col("s", "name"))))
            .whenMatchedDelete(col("s", "deleted").eq(lit(true)))
            .whenNotMatchedBySourceDelete(col("users", "active").eq(lit(false)))
            .whenNotMatchedInsert(col("s", "name").isNotNull(), List.of(id("id"), id("name")), row(col("s", "id"), col("s", "name")))
            .build();

        var source = emitter.emit(mergeStatement);

        assertTrue(source.contains("merge(tbl(\"users\"))"));
        assertTrue(source.contains("\n.source("));
        assertTrue(source.contains("\n.on("));
        assertTrue(source.contains("\n.top("));
        assertTrue(source.contains("\n.whenMatchedUpdate("));
        assertTrue(source.contains("\n.whenMatchedDelete("));
        assertTrue(source.contains("\n.whenNotMatchedBySourceDelete("));
        assertTrue(source.contains("\n.whenNotMatchedInsert("));
        assertTrue(source.contains("\n.build()"));
        assertTrue(source.contains(".hint(\"MERGE_HINT\")"));
        assertTrue(source.contains(".source(tbl(\"src\").as(\"s\"))"));
        assertTrue(source.contains(".top(lit(5L))"));
        assertTrue(source.contains(".whenMatchedUpdate(col(\"s\", \"active\").eq(lit(true)), set(id(\"name\"), col(\"s\", \"name\")))"));
        assertTrue(source.contains(".whenMatchedDelete(col(\"s\", \"deleted\").eq(lit(true)))"));
        assertTrue(source.contains(".whenNotMatchedBySourceDelete(col(\"users\", \"active\").eq(lit(false)))"));
        assertTrue(source.contains(".whenNotMatchedInsert(col(\"s\", \"name\").isNotNull(), List.of(id(\"id\"), id(\"name\")), row(col(\"s\", \"id\"), col(\"s\", \"name\")))"));
    }

    @Test
    void emitStatement_emitsTypedHints() {
        var query = select(star()).from(tbl("users")).hint("MAX_EXECUTION_TIME", 1000).build();
        var source = emitter.emit(query);

        assertTrue(source.contains(".hint(\"MAX_EXECUTION_TIME\", 1000)"));
    }

    @Test
    void emit_emitsGenericTableHintsAndTypedHintArgs() {
        Query query = select(star())
            .from(tbl("users")
                .hint(TableHint.of("GENERIC_TABLE_HINT", qualify("app", "users"), lit("x")))
            )
            .hint(statementHint("QUALIFIED_HINT", qualify("app", "users")))
            .build();

        var source = emitter.emit(query);

        assertTrue(source.contains(".hint(\"QUALIFIED_HINT\", qualify(id(\"app\"), id(\"users\")))"));
        assertTrue(source.contains(".hint(\"GENERIC_TABLE_HINT\", qualify(id(\"app\"), id(\"users\")), lit(\"x\"))"));
    }

    @Test
    void emit_coversMergeVariantsWithoutColumnsAndWithResultClause() {
        var mergeStatement = merge(tbl("users"))
            .source(tbl("src").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenMatchedDelete()
            .whenNotMatchedInsert(row(col("s", "id")))
            .result(inserted("id"))
            .build();

        var source = emitter.emit(mergeStatement);

        assertTrue(source.contains(".whenMatchedDelete()"));
        assertTrue(source.contains(".whenNotMatchedInsert(row(col(\"s\", \"id\")))"));
        assertTrue(source.contains(".result(inserted(id(\"id\")))"));
    }

    @Test
    void emit_coversPredicateAwareMergeInsertWithoutColumns() {
        var mergeStatement = merge(tbl("users"))
            .source(tbl("src").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenNotMatchedInsert(col("s", "id").gt(lit(0)), row(col("s", "id")))
            .build();

        var source = emitter.emit(mergeStatement);

        assertTrue(source.contains(".whenNotMatchedInsert(col(\"s\", \"id\").gt(lit(0)), row(col(\"s\", \"id\")))"));
    }

    @Test
    void emit_coversMergeDoNothingVariants() {
        var mergeStatement = merge(tbl("users"))
            .source(tbl("src").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenMatchedDoNothing()
            .whenNotMatchedDoNothing(col("s", "id").gt(lit(0)))
            .whenNotMatchedBySourceDoNothing()
            .build();

        var source = emitter.emit(mergeStatement);

        assertTrue(source.contains(".whenMatchedDoNothing()"));
        assertTrue(source.contains(".whenNotMatchedDoNothing(col(\"s\", \"id\").gt(lit(0)))"));
        assertTrue(source.contains(".whenNotMatchedBySourceDoNothing()"));
    }

    @Test
    void emit_coversMergeTopSpecAndVarargBySourceUpdateVariants() {
        var mergeStatement = merge(tbl("users"))
            .source(tbl("src").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .top(lit(10), true, true)
            .whenNotMatchedBySourceUpdate(set("name", lit("archived")))
            .build();

        var source = emitter.emit(mergeStatement);

        assertTrue(source.contains(".top(lit(10), true, true)"));
        assertTrue(source.contains(".whenNotMatchedBySourceUpdate(set(id(\"name\"), lit(\"archived\")))"));
    }

    @Test
    void emit_covers_remaining_window_distinct_and_limit_variants() {
        var baseFrameOnly = emitter.emit(
            select(func("f").over("base", rows(currentRow()))).from(tbl("t")).build()
        );
        var baseOrderOnly = emitter.emit(
            select(func("f").over("base", orderBy(order(col("x"))))).from(tbl("t")).build()
        );
        var partitionFrameOnly = emitter.emit(
            select(func("f").over(partition(col("p")), rows(currentRow()))).from(tbl("t")).build()
        );
        var plainExcludeNoOthers = emitter.emit(
            select(func("f").over(orderBy(order(col("x"))), rows(currentRow()), excludeNoOthers())).from(tbl("t")).build()
        );
        var distinctOn = emitter.emit(
            select(star()).from(tbl("t")).distinct(distinctOn(col("t", "id"))).build()
        );
        var limitAllNoOffset = emitter.emit(select(star()).from(tbl("t")).limitOffset(limitAll()).build());

        assertTrue(baseFrameOnly.contains(".over("));
        assertTrue(baseFrameOnly.contains("\"base\", rows(currentRow())"));
        assertTrue(baseOrderOnly.contains(".over("));
        assertTrue(baseOrderOnly.contains("\"base\", orderBy("));
        assertTrue(partitionFrameOnly.contains(".over("));
        assertTrue(partitionFrameOnly.contains("partition("));
        assertTrue(plainExcludeNoOthers.contains("excludeNoOthers()"));
        assertTrue(distinctOn.contains(".distinct(col(\"t\", \"id\"))"));
        assertTrue(limitAllNoOffset.contains(".limitOffset(limitAll())"));
    }

    @Test
    void emit_coversDslSyntaxRegressionCases() {
        var subquery = select(col("age")).from(tbl("limits")).build();
        var query = select(
            col("flags").unary("~"),
            col("payload").cast(type(qualify("pg_catalog", "time"), List.of(lit(6)), 0, TimeZoneSpec.WITH_TIME_ZONE)),
            lit("1").cast(type(TypeKeyword.DOUBLE_PRECISION)),
            col("payload").op(op("?"), lit("name")),
            col("payload").op(op(qualify("pg_catalog"), "?"), lit("name")),
            func("dense_rank").over(OverSpec.def((Identifier) null, null, rows(currentRow()), OverSpec.Exclude.CURRENT_ROW)),
            func("rank").over(OverSpec.def(Identifier.of("base"), null, rows(currentRow()), OverSpec.Exclude.GROUP)),
            func("sum", arg(col("amount"))).over(OverSpec.def(partition(col("dept")), null, rows(currentRow()), OverSpec.Exclude.NO_OTHERS))
        )
            .from(tbl("users"))
            .where(
                col("age").any(ComparisonOperator.GT, subquery)
                    .and(col("name").regex(RegexMode.MATCH_INSENSITIVE, lit("^a"), true))
            )
            .lockFor(update(), List.of(), false, false)
            .build();
        var updateStatement = update(tbl("users"))
            .set(qualify("app", "users", "name"), lit("alice"))
            .build();

        var querySource = emitter.emit(query);
        var updateSource = emitter.emit(updateStatement);

        assertTrue(querySource.contains("col(\"flags\").unary(\"~\")"));
        assertTrue(querySource.contains("type(qualify(id(\"pg_catalog\"), id(\"time\")), List.of(lit(6)), 0, TimeZoneSpec.WITH_TIME_ZONE)"));
        assertTrue(querySource.contains("type(TypeKeyword.DOUBLE_PRECISION)"));
        assertTrue(querySource.contains(".op(\"?\", lit(\"name\"))"));
        assertTrue(querySource.contains(".op(op(qualify(\"pg_catalog\"), \"?\"), lit(\"name\"))"));
        assertTrue(querySource.contains(".over("));
        assertTrue(querySource.contains("null, rows(currentRow()), excludeCurrentRow()"));
        assertTrue(querySource.contains("\"base\", null, rows(currentRow()), excludeGroup()"));
        assertTrue(querySource.contains("partition(col(\"dept\")), null, rows(currentRow()), excludeNoOthers()"));
        assertTrue(querySource.contains(".any(ComparisonOperator.GT, select("));
        assertTrue(querySource.contains(".regex(RegexMode.MATCH_INSENSITIVE, lit(\"^a\"), true)"));
        assertTrue(querySource.contains(".lockFor(update(), List.of(), false, false)"));
        assertTrue(updateSource.contains(".set(qualify(id(\"app\"), id(\"users\"), id(\"name\")), lit(\"alice\"))"));
    }

    @Test
    void emit_coversAdditionalExpressionAndQueryNodes() {
        var query = select(
            date("2026-04-19"),
            bit("1010"),
            hex("ff"),
            interval("1", "DAY"),
            timestamp("2026-04-19 10:15:00", TimeZoneSpec.WITH_TIME_ZONE),
            time("10:15:00", TimeZoneSpec.WITHOUT_TIME_ZONE),
            dollar("tag", "body"),
            escape("line\\n"),
            array(lit(1), lit(2)),
            col("items").slice(lit(1), lit(3)),
            col("items").slice(null, lit(3)),
            col("items").at(lit(1)),
            col("name").collate(qualify("pg_catalog", "en_US")),
            concat(col("first"), lit(" "), col("last")),
            col("a").add(lit(1)),
            col("a").sub(lit(1)),
            col("a").mul(lit(2)),
            col("a").div(lit(2)),
            col("a").mod(lit(2)),
            col("a").pow(lit(2)),
            col("a").neg(),
            col("a").isDistinctFrom(col("b")),
            col("a").isNotDistinctFrom(col("b")),
            kase(
                when(col("active").eq(lit(true)), lit("active")),
                when(col("active").eq(lit(false)), lit("inactive"))
            ).elseExpr(lit("unknown"))
        )
            .from(tbl(func("generate_series", arg(lit(1)), arg(lit(3)))))
            .where(col("a").ne(lit(0)).or(col("b").lte(lit(10))))
            .orderBy(order(col("name")).nullsDefault())
            .build();
        var valuesQuery = select(star()).from(tbl(rows(row(lit(1), lit("a"))))).build();
        var withQuery = with(
            cte(
                "active_users",
                select(star()).from(tbl("users")).where(col("active").eq(lit(true))).build(),
                List.of("id", "name"),
                CteDef.Materialization.MATERIALIZED
            )
        )
            .recursive(true)
            .body(select(star()).from(tbl("active_users")).build());

        var source = emitter.emit(query);
        var valuesSource = emitter.emit(valuesQuery);
        var withSource = emitter.emit(withQuery);

        assertTrue(source.contains("date(\"2026-04-19\")"));
        assertTrue(source.contains("bit(\"1010\")"));
        assertTrue(source.contains("hex(\"ff\")"));
        assertTrue(source.contains("interval(\"1\", \"DAY\")"));
        assertTrue(source.contains("timestamp(\"2026-04-19 10:15:00\", TimeZoneSpec.WITH_TIME_ZONE)"));
        assertTrue(source.contains("time(\"10:15:00\", TimeZoneSpec.WITHOUT_TIME_ZONE)"));
        assertTrue(source.contains("dollar(\"tag\", \"body\")"));
        assertTrue(source.contains("escape(\"line\\\\n\")"));
        assertTrue(source.contains("array(lit(1), lit(2))"));
        assertTrue(source.contains("col(\"items\").slice(lit(1), lit(3))"));
        assertTrue(source.contains("col(\"items\").slice(null, lit(3))"));
        assertTrue(source.contains("col(\"items\").at(lit(1))"));
        assertTrue(source.contains("col(\"name\").collate(\"pg_catalog.en_US\")"));
        assertTrue(source.contains("concat(col(\"first\"), lit(\" \"), col(\"last\"))"));
        assertTrue(source.contains(".add(lit(1))"));
        assertTrue(source.contains(".sub(lit(1))"));
        assertTrue(source.contains(".mul(lit(2))"));
        assertTrue(source.contains(".div(lit(2))"));
        assertTrue(source.contains(".mod(lit(2))"));
        assertTrue(source.contains(".pow(lit(2))"));
        assertTrue(source.contains(".neg()"));
        assertTrue(source.contains(".isDistinctFrom(col(\"b\"))"));
        assertTrue(source.contains(".isNotDistinctFrom(col(\"b\"))"));
        assertTrue(source.contains("kase("));
        assertTrue(source.contains(".elseExpr(lit(\"unknown\"))"));
        assertTrue(source.contains("tbl("));
        assertTrue(source.contains("func(\"generate_series\", arg(lit(1)), arg(lit(3)))"));
        assertTrue(source.contains(".or("));
        assertTrue(source.contains(".nullsDefault()"));
        assertTrue(valuesSource.contains("tbl("));
        assertTrue(valuesSource.contains("rows(row(lit(1), lit(\"a\")))"));
        assertTrue(withSource.contains("with(cte("));
        assertTrue(withSource.contains(".columnAliases(\"id\", \"name\")"));
        assertTrue(withSource.contains(".materialization(CteDef.Materialization.MATERIALIZED)"));
        assertTrue(withSource.contains(".recursive(true)"));
        assertTrue(withSource.contains(".body("));
    }

    @Test
    void emittedFunctionOverDslCompilesAgainstCurrentOverloads(@TempDir Path tempDir) throws Exception {
        List<Statement> statements = List.of(
            select(func("f").over(over())).from(tbl("t")).build(),
            select(func("f").over(orderBy(order(col("x"))))).from(tbl("t")).build(),
            select(func("f").over(orderBy(order(col("x"))), rows(currentRow()))).from(tbl("t")).build(),
            select(func("f").over(rows(currentRow()))).from(tbl("t")).build(),
            select(func("f").over(rows(currentRow()), excludeNoOthers())).from(tbl("t")).build(),
            select(func("f").over(partition(col("p")), orderBy(order(col("x"))), rows(currentRow()), excludeTies())).from(tbl("t")).build(),
            select(func("f").over("base", null, rows(currentRow()), excludeGroup())).from(tbl("t")).build()
        );

        var source = new StringBuilder("""
            package io.sqm.codegen.generated;

            import io.sqm.core.*;
            import java.util.*;
            import static io.sqm.dsl.Dsl.*;

            public final class EmittedDslSamples {
            """);
        for (int i = 0; i < statements.size(); i++) {
            source.append("    public static Statement s").append(i).append("() {\n")
                .append("        return ")
                .append(emitter.emit(statements.get(i)).replace("\n", "\n        "))
                .append(";\n")
                .append("    }\n");
        }
        source.append("}\n");

        var sourceFile = tempDir.resolve(Path.of("io", "sqm", "codegen", "generated", "EmittedDslSamples.java"));
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(sourceFile, source, StandardCharsets.UTF_8);

        var compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "System Java compiler is required to validate generated DSL.");
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            var units = fileManager.getJavaFileObjectsFromFiles(List.of(sourceFile.toFile()));
            var options = List.of(
                "-classpath", System.getProperty("java.class.path"),
                "-d", tempDir.resolve("classes").toString()
            );
            var success = compiler.getTask(null, fileManager, diagnostics, options, null, units).call();
            assertEquals(Boolean.TRUE, success, "Generated DSL failed to compile: " + diagnostics.getDiagnostics());
        }
    }
}

