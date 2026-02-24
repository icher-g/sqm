package io.sqm.core.transform;

import io.sqm.core.*;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

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
}
