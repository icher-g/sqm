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

        assertNotNull(plain);
        assertTrue(plain.items().isEmpty());
        assertEquals(2, onExpr.items().size());
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


