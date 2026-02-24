package io.sqm.core.transform;

import io.sqm.core.*;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IdentifierNormalizationTransformerTest {

    @Test
    void normalizes_unquoted_table_column_and_alias_identifiers() {
        SelectQuery query = select(col("U", "ID").as("CNT"))
            .from(tbl("Public", "Users").as("U"))
            .build();

        var transformed = new IdentifierNormalizationTransformer().apply(query);
        var from = (Table) transformed.from();
        var item = (ExprSelectItem) transformed.items().getFirst();
        var itemCol = (io.sqm.core.ColumnExpr) item.expr();

        assertEquals("public", from.schema().value());
        assertEquals("users", from.name().value());
        assertEquals("u", from.alias().value());
        assertEquals("u", itemCol.tableAlias().value());
        assertEquals("id", itemCol.name().value());
        assertEquals("cnt", item.alias().value());
    }

    @Test
    void preserves_quoted_identifiers() {
        SelectQuery query = select(col(
            Identifier.of("U", QuoteStyle.DOUBLE_QUOTE),
            Identifier.of("Id", QuoteStyle.DOUBLE_QUOTE)
        ))
            .from(tbl(
                Identifier.of("Public", QuoteStyle.DOUBLE_QUOTE),
                Identifier.of("Users", QuoteStyle.DOUBLE_QUOTE)
            )
                .as(Identifier.of("U", QuoteStyle.DOUBLE_QUOTE)))
            .build();

        var transformed = new IdentifierNormalizationTransformer().apply(query);
        var from = (Table) transformed.from();
        var itemCol = (io.sqm.core.ColumnExpr) ((ExprSelectItem) transformed.items().getFirst()).expr();

        assertEquals("Public", from.schema().value());
        assertEquals(QuoteStyle.DOUBLE_QUOTE, from.schema().quoteStyle());
        assertEquals("Users", from.name().value());
        assertEquals("U", from.alias().value());
        assertEquals("U", itemCol.tableAlias().value());
        assertEquals("Id", itemCol.name().value());
    }

    @Test
    void normalizes_qualified_names_in_function_type_and_collation() {
        var typedAndCollated = cast(
            lit(1),
            type("PG_CATALOG", "INT4")
        ).collate("PG_CATALOG.C");
        FunctionExpr functionExpr = func("PG_CATALOG.LOWER", arg(col("NAME")));
        Query query = select(typedAndCollated, functionExpr).from(tbl("USERS")).build();

        var transformed = (SelectQuery) new IdentifierNormalizationTransformer().apply(query);
        var first = (ExprSelectItem) transformed.items().getFirst();
        var collateExpr = (io.sqm.core.CollateExpr) first.expr();
        var castExpr = (CastExpr) collateExpr.expr();
        var second = (ExprSelectItem) transformed.items().get(1);
        var func = (FunctionExpr) second.expr();

        assertEquals(java.util.List.of("pg_catalog", "int4"), castExpr.type().qualifiedName().values());
        assertEquals(java.util.List.of("pg_catalog", "c"), collateExpr.collation().values());
        assertEquals(java.util.List.of("pg_catalog", "lower"), func.name().values());
    }

    @Test
    void normalizes_operator_schema_names_but_not_symbols() {
        var binary = BinaryOperatorExpr.of(
            lit(1),
            op("PG_CATALOG", "+"),
            lit(2)
        );
        var unary = UnaryOperatorExpr.of(
            op("PG_CATALOG", "@"),
            lit(1)
        );

        var binaryOut = new IdentifierNormalizationTransformer().apply(binary);
        var unaryOut = new IdentifierNormalizationTransformer().apply(unary);

        assertEquals(java.util.List.of("pg_catalog"), binaryOut.operator().schemaName().values());
        assertEquals("+", binaryOut.operator().symbol());
        assertEquals(java.util.List.of("pg_catalog"), unaryOut.operator().schemaName().values());
        assertEquals("@", unaryOut.operator().symbol());
    }

    @Test
    void returns_same_instance_when_query_is_already_normalized() {
        SelectQuery query = select(col("u", "id")).from(tbl("public", "users").as("u")).build();

        var transformed = new IdentifierNormalizationTransformer().apply(query);

        assertSame(query, transformed);
    }

    @Test
    void supports_uppercase_mode_for_unquoted_identifiers() {
        SelectQuery query = select(col("u", "id").as("cnt"))
            .from(tbl("public", "users").as("u"))
            .build();

        var transformed = new IdentifierNormalizationTransformer(
            IdentifierNormalizationCaseMode.UPPER
        ).apply(query);
        var from = (Table) transformed.from();
        var item = (ExprSelectItem) transformed.items().getFirst();
        var itemCol = (io.sqm.core.ColumnExpr) item.expr();

        assertEquals("PUBLIC", from.schema().value());
        assertEquals("USERS", from.name().value());
        assertEquals("U", from.alias().value());
        assertEquals("U", itemCol.tableAlias().value());
        assertEquals("ID", itemCol.name().value());
        assertEquals("CNT", item.alias().value());
    }

    @Test
    void supports_unchanged_mode_and_validates_constructor_argument() {
        SelectQuery query = select(col("MiXeD", "Id").as("Cnt"))
            .from(tbl("Public", "Users").as("U"))
            .build();

        var transformed = new IdentifierNormalizationTransformer(
            IdentifierNormalizationCaseMode.UNCHANGED
        ).apply(query);

        assertSame(query, transformed);
        assertThrows(NullPointerException.class, () -> new IdentifierNormalizationTransformer(null));
    }

    @Test
    void normalizes_cte_windows_order_collation_and_lock_targets() {
        var cteBody = select(col("ID"))
            .from(tbl("USERS"))
            .build();
        var windowDef = window("W", over(orderBy(order(col("U", "ID")).collate("PG_CATALOG.C"))));
        var query = with(
            cte("C", cteBody, java.util.List.of("ID"))
        ).body(
            select(
                col("U", "ID").as("CNT"),
                func("SUM", arg(col("U", "ID"))).over("W")
            )
                .from(tbl("C").as("U"))
                .window(windowDef)
                .orderBy(order(col("U", "ID")).collate("PG_CATALOG.C"))
                .lockFor(update(), ofTables("U"), false, false)
                .build()
        );

        var transformed = new IdentifierNormalizationTransformer().apply(query);
        var normalizedCte = transformed.ctes().getFirst();
        var select = (SelectQuery) transformed.body();

        assertEquals("c", normalizedCte.name().value());
        assertEquals(java.util.List.of("id"), normalizedCte.columnAliases().stream().map(Identifier::value).toList());

        assertEquals("w", select.windows().getFirst().name().value());

        var overItem = (ExprSelectItem) select.items().get(1);
        var overFunc = (FunctionExpr) overItem.expr();
        assertEquals("w", ((OverSpec.Ref) overFunc.over()).windowName().value());

        var orderItem = select.orderBy().items().getFirst();
        assertEquals(java.util.List.of("pg_catalog", "c"), orderItem.collate().values());

        assertEquals("u", select.lockFor().ofTables().getFirst().identifier().value());
    }

    @Test
    void normalizes_query_values_function_tables_and_using_join_column_aliases() {
        var sub = select(col("ID")).from(tbl("USERS")).build();
        var main = select(col("SUB", "ID"), col("V", "X"), col("F", "VAL"))
            .from(tbl(sub).as("SUB").columnAliases("ID"))
            .join(inner(tbl(rows(row(1))).as("V").columnAliases("X")).using("ID"))
            .join(inner(tbl(func("PG_CATALOG.GENERATE_SERIES", arg(lit(1)), arg(lit(2)))).as("F").columnAliases("VAL")).on(unary(lit(true))))
            .build();

        var transformed = new IdentifierNormalizationTransformer().apply(main);

        var fromQuery = (QueryTable) transformed.from();
        assertEquals("sub", fromQuery.alias().value());
        assertEquals(java.util.List.of("id"), fromQuery.columnAliases().stream().map(Identifier::value).toList());

        var usingJoin = (UsingJoin) transformed.joins().getFirst();
        assertEquals(java.util.List.of("id"), usingJoin.usingColumns().stream().map(Identifier::value).toList());
        var valuesTable = (ValuesTable) usingJoin.right();
        assertEquals("v", valuesTable.alias().value());
        assertEquals(java.util.List.of("x"), valuesTable.columnAliases().stream().map(Identifier::value).toList());

        var onJoin = (OnJoin) transformed.joins().get(1);
        var functionTable = (FunctionTable) onJoin.right();
        assertEquals("f", functionTable.alias().value());
        assertEquals(java.util.List.of("val"), functionTable.columnAliases().stream().map(Identifier::value).toList());
        assertEquals(java.util.List.of("pg_catalog", "generate_series"), functionTable.function().name().values());
    }

    @Test
    void normalizes_misc_node_types_and_preserves_noop_nodes() {
        var tx = new IdentifierNormalizationTransformer();

        var qualifiedStar = star(Identifier.of("T"));
        var qualifiedStarOut = tx.apply(qualifiedStar);
        assertEquals("t", qualifiedStarOut.qualifier().value());

        var overDefWithoutBase = over(orderBy(order(col("T", "ID"))));
        var overDefOut = tx.apply(overDefWithoutBase);
        assertInstanceOf(OverSpec.Def.class, overDefOut);
        assertNull(overDefOut.baseWindow());

        var lockingNoTargets = LockingClause.of(update(), java.util.List.of(), false, false);
        assertSame(lockingNoTargets, tx.apply(lockingNoTargets));

        var keywordType = type(TypeKeyword.DOUBLE_PRECISION);
        assertSame(keywordType, tx.apply(keywordType));

        var bareBinary = BinaryOperatorExpr.of(lit(1), op("+"), lit(2));
        var bareUnary = UnaryOperatorExpr.of(op("-"), lit(1));
        assertSame(bareBinary, tx.apply(bareBinary));
        assertSame(bareUnary, tx.apply(bareUnary));

        var orderNoCollate = order(col("x"));
        assertSame(orderNoCollate, tx.apply(orderNoCollate));
    }

    @Test
    void normalizes_expr_select_item_alias_and_collate_expr_directly() {
        var tx = new IdentifierNormalizationTransformer();
        var item = col("T", "ID").add(lit(0)).as("CNT");
        var collated = col("NAME").collate("PG_CATALOG.C");

        var itemOut = tx.apply(item);
        var collateOut = tx.apply(collated);

        assertInstanceOf(ExprSelectItem.class, itemOut);
        assertEquals("cnt", ((ExprSelectItem) itemOut).alias().value());
        assertInstanceOf(io.sqm.core.CollateExpr.class, collateOut);
        assertEquals(java.util.List.of("pg_catalog", "c"), collateOut.collation().values());
    }
}
