package io.sqm.dsl;

import io.sqm.core.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class DslAdditionalHelpersTest {

    @Test
    void orderOrdinalAndNullsHelpers() {
        var item = order(1).desc().nullsLast();
        assertEquals(1, item.ordinal());
        assertEquals(Nulls.LAST, item.nulls());

        var reset = item.nullsDefault();
        assertEquals(Nulls.DEFAULT, reset.nulls());
    }

    @Test
    void boundExpressionHelpers() {
        BoundSpec preceding = preceding(param("n"));
        BoundSpec following = following(param("m"));

        assertInstanceOf(BoundSpec.Preceding.class, preceding);
        assertInstanceOf(BoundSpec.Following.class, following);
    }

    @Test
    void overHelpersWithoutPartition() {
        var byOrder = over(orderBy(order(col("created_at")).desc()));
        assertInstanceOf(OverSpec.Def.class, byOrder);
        assertNotNull(byOrder.orderBy());

        var empty = over();
        assertInstanceOf(OverSpec.Def.class, empty);

        var baseOnly = overDef("w");
        assertInstanceOf(OverSpec.Def.class, baseOnly);
        assertEquals("w", baseOnly.baseWindow().value());

        var withExclude = over(orderBy(order(col("created_at"))), rows(preceding(1), currentRow()), excludeNoOthers());
        assertInstanceOf(OverSpec.Def.class, withExclude);
        assertEquals(OverSpec.Exclude.NO_OTHERS, withExclude.exclude());
    }

    @Test
    void distinctHelpers() {
        DistinctSpec plain = distinct();
        DistinctSpec onExpr = distinctOn(col("a"), col("b"));
        TopSpec percent = topPercent(lit(10));
        TopSpec withTies = topWithTies(lit(5));

        assertNotNull(plain);
        assertTrue(plain.items().isEmpty());
        assertEquals(2, onExpr.items().size());
        assertTrue(percent.percent());
        assertFalse(percent.withTies());
        assertFalse(withTies.percent());
        assertTrue(withTies.withTies());
    }

    @Test
    void limitOffsetHelpers() {
        LimitOffset pair = limitOffset(lit(10), lit(5));
        LimitOffset allOnly = limitAll();
        LimitOffset allWithOffset = limitAll(lit(3));

        assertNotNull(pair.limit());
        assertNotNull(pair.offset());
        assertTrue(allOnly.limitAll());
        assertTrue(allWithOffset.limitAll());
        assertNotNull(allWithOffset.offset());
    }

    @Test
    void typeAndTableWrapperHelpers() {
        assertEquals(List.of("int4"), type("int4").qualifiedName().values());
        assertEquals(List.of("PG_CATALOG", "INT4"),
            type(QualifiedName.of(Identifier.of("PG_CATALOG"), Identifier.of("INT4"))).qualifiedName().values());
        assertEquals(TypeKeyword.DOUBLE_PRECISION, type(TypeKeyword.DOUBLE_PRECISION).keyword().orElseThrow());

        QueryTable queryTable = tbl(select(star()).from(tbl("t")).build());
        assertNotNull(queryTable.query());

        ValuesTable valuesTable = tbl(rows(row(1, "a")));
        assertNotNull(valuesTable.values());

        FunctionTable functionTable = tbl(func("unnest", arg(array(lit(1), lit(2)))));
        assertNotNull(functionTable.function());
    }

    @Test
    void tableHintHelpersSupportStandaloneAndFluentTableUsage() {
        var standalone = tableHint("INDEX", "idx_users_name");
        var table = tbl("users")
            .hint(tableHint("NOLOCK"))
            .hint(tableHint("HOLDLOCK"));

        assertEquals("INDEX", standalone.name().value());
        assertEquals("idx_users_name", assertInstanceOf(IdentifierHintArg.class, standalone.args().getFirst()).value().value());
        assertEquals(List.of("NOLOCK", "HOLDLOCK"), table.hints().stream().map(h -> h.name().value()).toList());
    }

    @Test
    void typedIdentifierHelpersForTableColumnCastAndOperator() {
        var table = tbl(id("Users"));
        var qualifiedTable = tbl(id("Public"), id("Users"));
        var column = col(id("ID"));
        var qualifiedColumn = col(id("U"), id("ID"));
        var castExpr = cast(lit(1), type(QualifiedName.of("pg_catalog", "int4")));
        var interval = interval("1", "DAY");
        var bareOp = op("+");
        var schemaOp = op("pg_catalog", "@>");
        var typedSchemaOp = op(QualifiedName.of(id("pg_catalog")), "||");
        var concatExpr = concat(col("first_name"), lit(" "), col("last_name"));
        var concatListExpr = concat(List.of(col("first_name"), lit(" "), col("last_name")));

        assertEquals("Users", table.name().value());
        assertEquals("Public", qualifiedTable.schema().value());
        assertEquals("ID", column.name().value());
        assertEquals("U", qualifiedColumn.tableAlias().value());
        assertEquals(List.of("pg_catalog", "int4"), castExpr.type().qualifiedName().values());
        assertEquals("1", interval.value());
        assertEquals("DAY", interval.qualifier().orElseThrow());
        assertNull(bareOp.schemaName());
        assertEquals(List.of("pg_catalog"), schemaOp.schemaName().values());
        assertEquals(List.of("pg_catalog"), typedSchemaOp.schemaName().values());
        assertEquals(3, concatExpr.args().size());
        assertEquals(3, concatListExpr.args().size());

        var quotedId = id("U", QuoteStyle.BACKTICK);
        assertEquals(QuoteStyle.BACKTICK, quotedId.quoteStyle());
    }

    @Test
    void additionalDslOverloadsBuildExpectedModelShapes() {
        var aliasedTable = tbl("app", "users", "u");
        var inheritedAliasedTable = tbl("app", "users", "u", Table.Inheritance.ONLY);
        var qualified = qualify("app", "users");
        var quotedQualified = qualify(id("app", QuoteStyle.DOUBLE_QUOTE), id("users", QuoteStyle.BACKTICK));
        var bareTableHintById = tableHint(id("NOLOCK"));
        var tableHintById = tableHint(id("INDEX"), id("idx_users"));
        var bareStatementHintById = statementHint(id("RECOMPILE"));
        var statementHintById = statementHint(id("MAX_EXECUTION_TIME"), 1000);
        var resultIntoWithoutColumns = resultInto(tableVar("audit"));
        var resultIntoByIds = resultInto(tableVar("audit"), id("user_id"), id("changed_at"));
        var insertedColumn = inserted(id("id"));
        var deletedColumn = deleted(id("id"));
        var intervalWithoutQualifier = interval("1 day");
        var objectRows = rows(List.of(List.of(1, "a"), List.of(2, "b")));
        var rowList = rows(row(lit(1), lit("a")), row(lit(2), lit("b")));
        var assignmentById = set(id("name"), lit("alice"));
        var assignmentByQualifiedName = set(qualify("u", "name"), lit("bob"));
        var assignmentWithLiteral = set("name", "carol");
        var arrayFromList = array(List.of(lit(1), lit(2)));
        var typedWithModifiers = type(qualify("numeric"), List.of(lit(10), lit(2)));
        var typedArray = type(qualify("text"), List.of(lit(32)), 2);
        var typedFull = type(qualify("timestamp"), List.of(lit(3)), 1, TimeZoneSpec.WITH_TIME_ZONE);
        var typedArrayOnly = type(qualify("uuid"), 1);
        var typedWithTimeZone = type(qualify("timestamp"), TimeZoneSpec.WITH_TIME_ZONE);
        var typedArrayWithTimeZone = type(qualify("timestamp"), 1, TimeZoneSpec.WITHOUT_TIME_ZONE);
        var leftJoin = left(tbl("teams"));
        var rightJoin = right(tbl("teams"));
        var fullJoin = full(tbl("accounts"));
        var tableColumnOrder = order("u", "name");
        var orderBy = orderBy(order(col("name")));
        var namedWindow = window("w", partition(col("dept")), orderBy);
        var withFrameWindow = window("wf", partition(col("dept")), orderBy, rows(currentRow()));
        var withExcludeWindow = window("we", partition(col("dept")), orderBy, rows(currentRow()), excludeGroup());
        var partitionOver = over(partition(col("dept")));
        var partitionOrderOver = over(partition(col("dept")), orderBy);
        var partitionFrameOver = over(partition(col("dept")), rows(currentRow()));
        var partitionOrderFrameOver = over(partition(col("dept")), orderBy, rows(currentRow()));
        var partitionExcludeOver = over(partition(col("dept")), orderBy, rows(currentRow()), excludeTies());
        var baseFrameOver = over("base", rows(currentRow()));
        var orderFrameOver = over(orderBy, rows(currentRow()));
        var frameOver = over(rows(currentRow()));
        var frameExcludeOver = over(rows(currentRow()), excludeNoOthers());
        var groupsSingle = groups(currentRow());
        var rangeBetween = range(unboundedPreceding(), following(3));
        var groupsBetween = groups(preceding(1), following(1));
        var whenThen = when(col("active").eq(lit(true)), lit("yes"));
        var notExists = notExists(select(star()).from(tbl("archived")).build());
        var mergeBuilder = merge("users");
        var cteStatement = cte("changed", update("users").set(set("name", lit("alice"))).build());
        var cteStatementWithMaterialization = cte(
            "changed",
            update("users").set(set("name", lit("alice"))).build(),
            List.of("id"),
            CteDef.Materialization.NOT_MATERIALIZED
        );
        var cteStatementWithNullAliases = cte(
            "changed",
            update("users").set(set("name", lit("alice"))).build(),
            null,
            CteDef.Materialization.DEFAULT
        );
        var cteStatementWithAliases = cte(
            "changed",
            update("users").set(set("name", lit("alice"))).build(),
            List.of("id")
        );
        var cteStatementWithNullAliasesNoMaterialization = cte(
            "changed",
            update("users").set(set("name", lit("alice"))).build(),
            null
        );
        var cteQueryWithNullAliases = cte("q", select(star()).from(tbl("users")).build(), null, CteDef.Materialization.DEFAULT);
        var cteQueryWithNullAliasesNoMaterialization = cte("q", select(star()).from(tbl("users")).build(), null);
        var explicitTop = top(lit(10), true, true);
        var literalTop = top(3L);
        var limitOffset = limitOffset(lit(10), lit(5));

        assertEquals("u", aliasedTable.alias().value());
        assertEquals(Table.Inheritance.ONLY, inheritedAliasedTable.inheritance());
        assertEquals(List.of("app", "users"), qualified.values());
        assertEquals(QuoteStyle.DOUBLE_QUOTE, quotedQualified.parts().getFirst().quoteStyle());
        assertEquals("NOLOCK", bareTableHintById.name().value());
        assertTrue(bareTableHintById.args().isEmpty());
        assertEquals("INDEX", tableHintById.name().value());
        assertEquals("RECOMPILE", bareStatementHintById.name().value());
        assertTrue(bareStatementHintById.args().isEmpty());
        assertEquals("MAX_EXECUTION_TIME", statementHintById.name().value());
        assertTrue(resultIntoWithoutColumns.columns().isEmpty());
        assertEquals(List.of("user_id", "changed_at"), resultIntoByIds.columns().stream().map(Identifier::value).toList());
        assertEquals(OutputRowSource.INSERTED, insertedColumn.source());
        assertEquals(OutputRowSource.DELETED, deletedColumn.source());
        assertTrue(intervalWithoutQualifier.qualifier().isEmpty());
        assertEquals(2, objectRows.rows().size());
        assertEquals(2, rowList.rows().size());
        assertEquals(List.of("name"), assignmentById.column().values());
        assertEquals(List.of("u", "name"), assignmentByQualifiedName.column().values());
        assertEquals("carol", assignmentWithLiteral.value().matchExpression().literal(l -> l.value()).orElse(null));
        assertEquals(2, arrayFromList.elements().size());
        assertEquals(2, typedWithModifiers.modifiers().size());
        assertEquals(2, typedArray.arrayDims());
        assertEquals(1, typedFull.arrayDims());
        assertEquals(TimeZoneSpec.WITH_TIME_ZONE, typedFull.timeZoneSpec());
        assertEquals(1, typedArrayOnly.arrayDims());
        assertEquals(TimeZoneSpec.WITH_TIME_ZONE, typedWithTimeZone.timeZoneSpec());
        assertEquals(TimeZoneSpec.WITHOUT_TIME_ZONE, typedArrayWithTimeZone.timeZoneSpec());
        assertEquals(JoinKind.LEFT, leftJoin.kind());
        assertEquals(JoinKind.RIGHT, rightJoin.kind());
        assertEquals(JoinKind.FULL, fullJoin.kind());
        assertEquals("u", assertInstanceOf(ColumnExpr.class, tableColumnOrder.expr()).tableAlias().value());
        assertEquals(1, namedWindow.spec().orderBy().items().size());
        assertNotNull(withFrameWindow.spec().frame());
        assertEquals(OverSpec.Exclude.GROUP, withExcludeWindow.spec().exclude());
        assertNotNull(partitionOver.partitionBy());
        assertNotNull(partitionOrderOver.orderBy());
        assertNotNull(partitionFrameOver.frame());
        assertNotNull(partitionOrderFrameOver.orderBy());
        assertEquals(OverSpec.Exclude.TIES, partitionExcludeOver.exclude());
        assertEquals("base", baseFrameOver.baseWindow().value());
        assertNotNull(orderFrameOver.frame());
        assertNotNull(frameOver.frame());
        assertEquals(OverSpec.Exclude.NO_OTHERS, frameExcludeOver.exclude());
        assertEquals(FrameSpec.Unit.GROUPS, groupsSingle.unit());
        assertEquals(FrameSpec.Unit.RANGE, rangeBetween.unit());
        assertEquals(FrameSpec.Unit.GROUPS, groupsBetween.unit());
        assertEquals("yes", whenThen.then().matchExpression().literal(l -> l.value()).orElse(null));
        assertTrue(notExists.negated());
        assertNotNull(mergeBuilder);
        assertEquals("changed", cteStatement.name().value());
        assertEquals(CteDef.Materialization.NOT_MATERIALIZED, cteStatementWithMaterialization.materialization());
        assertNull(cteStatementWithNullAliases.columnAliases());
        assertEquals(List.of("id"), cteStatementWithAliases.columnAliases().stream().map(Identifier::value).toList());
        assertNull(cteStatementWithNullAliasesNoMaterialization.columnAliases());
        assertNull(cteQueryWithNullAliases.columnAliases());
        assertNull(cteQueryWithNullAliasesNoMaterialization.columnAliases());
        assertTrue(explicitTop.percent());
        assertTrue(explicitTop.withTies());
        assertEquals(3L, literalTop.count().matchExpression().literal(l -> l.value()).orElse(null));
        assertNotNull(limitOffset.limit());
        assertThrows(IllegalArgumentException.class, () -> ofTables(QuoteStyle.DOUBLE_QUOTE, "ok", " "));
    }

    @Test
    void literalAndExtendedLiteralHelpersBuildExpectedValues() {
        assertEquals("abc", lit("abc").value());
        assertEquals("2026-04-19", date("2026-04-19").value());
        assertEquals("10:15:00", time("10:15:00", TimeZoneSpec.WITH_TIME_ZONE).value());
        assertEquals(TimeZoneSpec.WITH_TIME_ZONE, time("10:15:00", TimeZoneSpec.WITH_TIME_ZONE).timeZoneSpec());
        assertEquals("2026-04-19 10:15:00", timestamp("2026-04-19 10:15:00", TimeZoneSpec.WITHOUT_TIME_ZONE).value());
        assertEquals(TimeZoneSpec.WITHOUT_TIME_ZONE, timestamp("2026-04-19 10:15:00", TimeZoneSpec.WITHOUT_TIME_ZONE).timeZoneSpec());
        assertEquals("1010", bit("1010").value());
        assertEquals("ff", hex("ff").value());
        assertEquals("tag", dollar("tag", "body").tag());
        assertEquals("body", dollar("tag", "body").value());
        assertEquals("line\\n", escape("line\\n").value());
    }

    @Test
    void dmlHelpersBuildInsertUpdateAndAssignments() {
        var insert = insert("users")
            .columns(id("id"))
            .values(row(lit(1)))
            .build();
        var assignment = set("u", "name", "alice");
        var straightJoin = straight(tbl("orders").as("o")).on(col("o", "user_id").eq(col("users", "id")));
        var updateStatement = update("users")
            .set(assignment)
            .join(straightJoin)
            .where(col("id").eq(lit(1)))
            .build();

        assertEquals("users", insert.table().name().value());
        assertEquals(1, insert.columns().size());
        assertEquals(List.of("u", "name"), assignment.column().values());
        assertEquals(JoinKind.STRAIGHT, straightJoin.kind());
        assertEquals("users", updateStatement.table().name().value());
        assertEquals(JoinKind.STRAIGHT, assertInstanceOf(OnJoin.class, updateStatement.joins().getFirst()).kind());
        assertEquals(1, updateStatement.assignments().size());
        assertNotNull(updateStatement.where());
    }

    @Test
    void dmlDeleteHelperBuildsDeleteStatement() {
        var statement = delete("users")
            .where(col("id").eq(lit(1)))
            .build();

        assertEquals("users", statement.table().name().value());
        assertNotNull(statement.where());
    }

    @Test
    void sqlServerFunctionHelpersBuildExpectedFunctions() {
        var lenFn = len(col("name"));
        var dataLengthFn = dataLength(col("payload"));
        var getDateFn = getDate();
        var dateAddFn = dateAdd("day", lit(1), col("created_at"));
        var dateDiffFn = dateDiff("day", col("created_at"), col("updated_at"));
        var isNullFnExpr = isNullFn(col("name"), lit("unknown"));
        var stringAggFn = stringAgg(col("name"), lit(","));

        assertEquals("LEN", lenFn.name().values().getLast());
        assertEquals("DATALENGTH", dataLengthFn.name().values().getLast());
        assertEquals("GETDATE", getDateFn.name().values().getLast());
        assertEquals("DATEADD", dateAddFn.name().values().getLast());
        assertEquals("day", assertInstanceOf(LiteralExpr.class,
            assertInstanceOf(FunctionExpr.Arg.ExprArg.class, dateAddFn.args().getFirst()).expr()).value());
        assertEquals("DATEDIFF", dateDiffFn.name().values().getLast());
        assertEquals("ISNULL", isNullFnExpr.name().values().getLast());
        assertEquals("STRING_AGG", stringAggFn.name().values().getLast());
    }

    @Test
    void outputIntoHelpersAcceptTableVariablesWithOrWithoutSigil() {
        var withSigil = tableVar("@audit");
        var withoutSigil = tableVar("audit");
        var into = resultInto(withSigil, "user_id");

        assertEquals("audit", withSigil.name().value());
        assertEquals(withSigil, withoutSigil);
        assertEquals("audit", into.target().matchTableRef().variableTable(v -> v.name().value()).orElse(null));
        assertThrows(NullPointerException.class, () -> tableVar((String) null));
    }

    @Test
    void cteHelpersConvertStringNamesAndAliases() {
        var body = select(star()).from(tbl("t")).build();

        CteDef c1 = cte("c");
        assertEquals("c", c1.name().value());

        CteDef c2 = cte("c", body);
        assertEquals(body, c2.body());

        CteDef c3 = cte("c", body, List.of("id", "name"), CteDef.Materialization.MATERIALIZED);
        assertEquals(List.of("id", "name"), c3.columnAliases().stream().map(a -> a.value()).toList());
        assertEquals(CteDef.Materialization.MATERIALIZED, c3.materialization());

        CteDef c4 = cte("c", body, List.of("id"));
        assertEquals(List.of("id"), c4.columnAliases().stream().map(a -> a.value()).toList());
    }

    @Test
    void lockTargetHelpersValidateAndSupportQuoteStyle() {
        var targets = ofTables("u", "orders");
        assertEquals(List.of("u", "orders"), targets.stream().map(t -> t.identifier().value()).toList());

        var quoted = ofTables(QuoteStyle.BACKTICK, "U");
        assertEquals(QuoteStyle.BACKTICK, quoted.getFirst().identifier().quoteStyle());

        assertThrows(IllegalArgumentException.class, () -> ofTables("ok", " "));
        assertThrows(IllegalArgumentException.class, () -> ofTables(QuoteStyle.DOUBLE_QUOTE, (String) null));
    }

    @Test
    void overAndWindowConvenienceVariantsCoverStringBaseWindows() {
        var frame = rows(preceding(1), currentRow());

        assertEquals("base", overDef("base").baseWindow().value());
        assertEquals("base", over("base", orderBy(order(1))).baseWindow().value());
        assertEquals("base", over("base", frame).baseWindow().value());
        assertEquals("base", over("base", null, frame, excludeTies()).baseWindow().value());
        assertEquals("base", over("base", orderBy(order(1)), frame).baseWindow().value());
        assertEquals("base", over("base", orderBy(order(1)), frame, excludeCurrentRow()).baseWindow().value());

        assertEquals("wq", window("wq", over()).name().value());
    }
}


