package io.sqm.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sqm.core.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests JSON serialization/deserialization for Expression subtypes:
 * CastExpr, ArrayExpr, BinaryOperatorExpr, UnaryOperatorExpr.
 */
public class ExpressionSubtypesJsonTest {

    private final ObjectMapper mapper = SqmJsonMixins.createPretty();

    private <T> T roundTrip(T value, Class<T> type) throws Exception {
        String json = mapper.writeValueAsString(value);
        T back = mapper.readValue(json, type);
        assertEquals(value, back, "round-trip equality failed");
        return back;
    }

    private JsonNode toTree(Object value) throws Exception {
        return mapper.readTree(mapper.writeValueAsBytes(value));
    }

    /* ==================== CastExpr Tests ==================== */

    @Test
    @DisplayName("CastExpr: cast literal to type")
    void castExpr_literal() throws Exception {
        var expr = CastExpr.of(lit(123), type("bigint"));

        var back = roundTrip(expr, CastExpr.class);

        assertNotNull(back);
        assertEquals("bigint", back.type().qualifiedName().getFirst());
        assertInstanceOf(LiteralExpr.class, back.expr());
        assertEquals(123, ((LiteralExpr) back.expr()).value());

        JsonNode node = toTree(expr);
        assertEquals("cast", node.path("kind").asText());
        assertEquals("bigint", node.path("type").path("qualifiedName").get(0).asText());
    }

    @Test
    @DisplayName("CastExpr: cast column to array type")
    void castExpr_columnToArrayType() throws Exception {
        var expr = CastExpr.of(col("tags"), type("text").array());

        var back = roundTrip(expr, CastExpr.class);

        assertInstanceOf(ColumnExpr.class, back.expr());
        assertEquals("text", back.type().qualifiedName().getFirst());
        assertEquals(1, back.type().arrayDims());
    }

    @Test
    @DisplayName("CastExpr: polymorphic deserialization as Expression")
    void castExpr_asExpression() throws Exception {
        var expr = CastExpr.of(lit("test"), type("jsonb"));

        String json = mapper.writeValueAsString(expr);
        Expression back = mapper.readValue(json, Expression.class);

        assertInstanceOf(CastExpr.class, back);
        assertEquals("jsonb", ((CastExpr) back).type().qualifiedName().getFirst());
    }

    @Test
    @DisplayName("CastExpr in SELECT")
    void castExpr_inSelect() throws Exception {
        var query = select(
            col("id"),
            col("price").cast(TypeName.of(List.of("decimal"), null, List.of(lit(10), lit(2)), 0, TimeZoneSpec.NONE)).as("price_decimal")
        ).from(tbl("products"));

        var back = roundTrip(query, SelectQuery.class);

        var selectItem = (ExprSelectItem) back.items().get(1);
        assertEquals("price_decimal", selectItem.alias());
        assertInstanceOf(CastExpr.class, selectItem.expr());

        var castExpr = (CastExpr) selectItem.expr();
        assertEquals("decimal", castExpr.type().qualifiedName().getFirst());
    }

    @Test
    @DisplayName("CastExpr: nested cast")
    void castExpr_nested() throws Exception {
        // CAST(CAST('123' AS int) AS bigint)
        var inner = CastExpr.of(lit("123"), type("integer"));
        var outer = CastExpr.of(inner, type("bigint"));

        var back = roundTrip(outer, CastExpr.class);

        assertInstanceOf(CastExpr.class, back.expr());
        assertEquals("bigint", back.type().qualifiedName().getFirst());

        var innerCast = (CastExpr) back.expr();
        assertEquals("integer", innerCast.type().qualifiedName().getFirst());
    }

    /* ==================== ArrayExpr Tests ==================== */

    @Test
    @DisplayName("ArrayExpr: array of literals")
    void arrayExpr_literals() throws Exception {
        var expr = ArrayExpr.of(lit(1), lit(2), lit(3));

        var back = roundTrip(expr, ArrayExpr.class);

        assertNotNull(back);
        assertEquals(3, back.elements().size());
        assertEquals(1, ((LiteralExpr) back.elements().get(0)).value());
        assertEquals(2, ((LiteralExpr) back.elements().get(1)).value());
        assertEquals(3, ((LiteralExpr) back.elements().get(2)).value());

        JsonNode node = toTree(expr);
        assertEquals("array", node.path("kind").asText());
        assertEquals(3, node.path("elements").size());
    }

    @Test
    @DisplayName("ArrayExpr: array of columns")
    void arrayExpr_columns() throws Exception {
        var expr = ArrayExpr.of(col("a"), col("b"), col("c"));

        var back = roundTrip(expr, ArrayExpr.class);

        assertEquals(3, back.elements().size());
        assertInstanceOf(ColumnExpr.class, back.elements().get(0));
        assertInstanceOf(ColumnExpr.class, back.elements().get(1));
        assertInstanceOf(ColumnExpr.class, back.elements().get(2));

        assertEquals("a", back.elements().get(0).matchExpression().column(c -> c.name()).orElse(null));
        assertEquals("b", back.elements().get(1).matchExpression().column(c -> c.name()).orElse(null));
        assertEquals("c", back.elements().get(2).matchExpression().column(c -> c.name()).orElse(null));
    }

    @Test
    @DisplayName("ArrayExpr: mixed expression types")
    void arrayExpr_mixed() throws Exception {
        var expr = ArrayExpr.of(
            lit("hello"),
            col("name"),
            func("lower", arg(col("email")))
        );

        var back = roundTrip(expr, ArrayExpr.class);

        assertEquals(3, back.elements().size());
        assertInstanceOf(LiteralExpr.class, back.elements().get(0));
        assertInstanceOf(ColumnExpr.class, back.elements().get(1));
        assertInstanceOf(FunctionExpr.class, back.elements().get(2));
    }

    @Test
    @DisplayName("ArrayExpr: nested arrays")
    void arrayExpr_nested() throws Exception {
        var inner = ArrayExpr.of(lit(1), lit(2));
        var outer = ArrayExpr.of(inner, lit(3));

        var back = roundTrip(outer, ArrayExpr.class);

        assertEquals(2, back.elements().size());
        assertInstanceOf(ArrayExpr.class, back.elements().get(0));
        assertInstanceOf(LiteralExpr.class, back.elements().get(1));
    }

    @Test
    @DisplayName("ArrayExpr in SELECT")
    void arrayExpr_inSelect() throws Exception {
        var query = select(
            col("id"),
            ArrayExpr.of(col("tag1"), col("tag2"), col("tag3")).as("tags")
        ).from(tbl("items"));

        var back = roundTrip(query, SelectQuery.class);

        var selectItem = (ExprSelectItem) back.items().get(1);
        assertEquals("tags", selectItem.alias());
        assertInstanceOf(ArrayExpr.class, selectItem.expr());
    }

    /* ==================== BinaryOperatorExpr Tests ==================== */

    @Test
    @DisplayName("BinaryOperatorExpr: JSON operator ->")
    void binaryOperator_jsonExtract() throws Exception {
        var expr = BinaryOperatorExpr.of(col("payload"), "->", lit("user"));

        var back = roundTrip(expr, BinaryOperatorExpr.class);

        assertNotNull(back);
        assertEquals("->", back.operator());
        assertInstanceOf(ColumnExpr.class, back.left());
        assertInstanceOf(LiteralExpr.class, back.right());

        JsonNode node = toTree(expr);
        assertEquals("binary-op", node.path("kind").asText());
        assertEquals("->", node.path("operator").asText());
    }

    @Test
    @DisplayName("BinaryOperatorExpr: JSON operator ->>")
    void binaryOperator_jsonExtractText() throws Exception {
        var expr = BinaryOperatorExpr.of(col("data"), "->>", lit("name"));

        var back = roundTrip(expr, BinaryOperatorExpr.class);

        assertEquals("->>", back.operator());
        assertEquals("data", back.left().matchExpression().column(c -> c.name()).orElse(null));
        assertEquals("name", ((LiteralExpr) back.right()).value());
    }

    @Test
    @DisplayName("BinaryOperatorExpr: array overlap &&")
    void binaryOperator_arrayOverlap() throws Exception {
        var expr = BinaryOperatorExpr.of(col("tags"), "&&", col("other_tags"));

        var back = roundTrip(expr, BinaryOperatorExpr.class);

        assertEquals("&&", back.operator());
        assertInstanceOf(ColumnExpr.class, back.left());
        assertInstanceOf(ColumnExpr.class, back.right());
    }

    @Test
    @DisplayName("BinaryOperatorExpr: contains @>")
    void binaryOperator_contains() throws Exception {
        var expr = BinaryOperatorExpr.of(col("data"), "@>", lit("{\"a\":1}"));

        var back = roundTrip(expr, BinaryOperatorExpr.class);

        assertEquals("@>", back.operator());
    }

    @Test
    @DisplayName("BinaryOperatorExpr: contained by <@")
    void binaryOperator_containedBy() throws Exception {
        var expr = BinaryOperatorExpr.of(lit("{\"a\":1}"), "<@", col("data"));

        var back = roundTrip(expr, BinaryOperatorExpr.class);

        assertEquals("<@", back.operator());
    }

    @Test
    @DisplayName("BinaryOperatorExpr: regex match ~")
    void binaryOperator_regexMatch() throws Exception {
        var expr = BinaryOperatorExpr.of(col("name"), "~", lit("^test"));

        var back = roundTrip(expr, BinaryOperatorExpr.class);

        assertEquals("~", back.operator());
    }

    @Test
    @DisplayName("BinaryOperatorExpr: polymorphic deserialization")
    void binaryOperator_asExpression() throws Exception {
        var expr = BinaryOperatorExpr.of(col("x"), "->", col("y"));

        String json = mapper.writeValueAsString(expr);
        Expression back = mapper.readValue(json, Expression.class);

        assertInstanceOf(BinaryOperatorExpr.class, back);
        assertEquals("->", ((BinaryOperatorExpr) back).operator());
    }

    @Test
    @DisplayName("BinaryOperatorExpr: chained operators (left-associative)")
    void binaryOperator_chained() throws Exception {
        // (a -> b) -> c
        var inner = BinaryOperatorExpr.of(col("a"), "->", lit("b"));
        var outer = BinaryOperatorExpr.of(inner, "->", lit("c"));

        var back = roundTrip(outer, BinaryOperatorExpr.class);

        assertInstanceOf(BinaryOperatorExpr.class, back.left());
        assertEquals("->", back.operator());
        assertEquals("c", ((LiteralExpr) back.right()).value());
    }

    @Test
    @DisplayName("BinaryOperatorExpr in WHERE")
    void binaryOperator_inWhere() throws Exception {
        var binaryExpr = col("metadata").op("@>", lit("{\"status\":\"active\"}"));
        var query = select(col("*"))
            .from(tbl("documents"))
            .where(UnaryPredicate.of(binaryExpr));

        var back = roundTrip(query, SelectQuery.class);

        var predicate = back.where().<UnaryPredicate>matchPredicate().unary(u -> u).orElse(null);
        assertNotNull(predicate);
        assertInstanceOf(BinaryOperatorExpr.class, predicate.expr());
    }

    /* ==================== UnaryOperatorExpr Tests ==================== */

    @Test
    @DisplayName("UnaryOperatorExpr: bitwise NOT ~")
    void unaryOperator_bitwiseNot() throws Exception {
        var expr = UnaryOperatorExpr.of("~", col("mask"));

        var back = roundTrip(expr, UnaryOperatorExpr.class);

        assertNotNull(back);
        assertEquals("~", back.operator());
        assertInstanceOf(ColumnExpr.class, back.expr());
        assertEquals("mask", back.expr().matchExpression().column(c -> c.name()).orElse(null));

        JsonNode node = toTree(expr);
        assertEquals("unary-op", node.path("kind").asText());
        assertEquals("~", node.path("operator").asText());
    }

    @Test
    @DisplayName("UnaryOperatorExpr: negation -")
    void unaryOperator_negation() throws Exception {
        var expr = UnaryOperatorExpr.of("-", col("amount"));

        var back = roundTrip(expr, UnaryOperatorExpr.class);

        assertEquals("-", back.operator());
        assertEquals("amount", back.expr().matchExpression().column(c -> c.name()).orElse(null));
    }

    @Test
    @DisplayName("UnaryOperatorExpr: polymorphic deserialization")
    void unaryOperator_asExpression() throws Exception {
        var expr = UnaryOperatorExpr.of("~", lit(5));

        String json = mapper.writeValueAsString(expr);
        Expression back = mapper.readValue(json, Expression.class);

        assertInstanceOf(UnaryOperatorExpr.class, back);
        assertEquals("~", ((UnaryOperatorExpr) back).operator());
    }

    @Test
    @DisplayName("UnaryOperatorExpr: nested unary operators")
    void unaryOperator_nested() throws Exception {
        var inner = UnaryOperatorExpr.of("-", col("value"));
        var outer = UnaryOperatorExpr.of("~", inner);

        var back = roundTrip(outer, UnaryOperatorExpr.class);

        assertEquals("~", back.operator());
        assertInstanceOf(UnaryOperatorExpr.class, back.expr());

        var innerBack = (UnaryOperatorExpr) back.expr();
        assertEquals("-", innerBack.operator());
    }

    @Test
    @DisplayName("UnaryOperatorExpr in SELECT")
    void unaryOperator_inSelect() throws Exception {
        var query = select(
            col("id"),
            col("flags").unary("~").as("inverted_flags")
        ).from(tbl("config"));

        var back = roundTrip(query, SelectQuery.class);

        var selectItem = (ExprSelectItem) back.items().get(1);
        assertEquals("inverted_flags", selectItem.alias());
        assertInstanceOf(UnaryOperatorExpr.class, selectItem.expr());
    }

    /* ==================== Complex Integration Tests ==================== */

    @Test
    @DisplayName("Mixed expressions: CAST with array")
    void mixed_castWithArray() throws Exception {
        var array = ArrayExpr.of(lit(1), lit(2), lit(3));
        var cast = CastExpr.of(array, type("integer").array());

        var back = roundTrip(cast, CastExpr.class);

        assertInstanceOf(ArrayExpr.class, back.expr());
        assertEquals("integer", back.type().qualifiedName().getFirst());
        assertEquals(1, back.type().arrayDims());
    }

    @Test
    @DisplayName("Mixed expressions: binary operator with cast")
    void mixed_binaryOpWithCast() throws Exception {
        var cast = CastExpr.of(col("value"), type("jsonb"));
        var expr = BinaryOperatorExpr.of(cast, "->", lit("key"));

        var back = roundTrip(expr, BinaryOperatorExpr.class);

        assertInstanceOf(CastExpr.class, back.left());
        assertEquals("->", back.operator());
    }

    @Test
    @DisplayName("Mixed expressions: array with cast elements")
    void mixed_arrayWithCasts() throws Exception {
        var cast1 = CastExpr.of(lit("123"), type("integer"));
        var cast2 = CastExpr.of(lit("456"), type("integer"));
        var array = ArrayExpr.of(cast1, cast2);

        var back = roundTrip(array, ArrayExpr.class);

        assertEquals(2, back.elements().size());
        assertInstanceOf(CastExpr.class, back.elements().get(0));
        assertInstanceOf(CastExpr.class, back.elements().get(1));
    }

    @Test
    @DisplayName("Complex query with all expression subtypes")
    void complex_allSubtypes() throws Exception {
        var query = select(
            col("id"),
            col("data").op("->", lit("user")).as("user_data"),
            col("price").cast(TypeName.of(List.of("decimal"), null, List.of(lit(10), lit(2)), 0, TimeZoneSpec.NONE)).as("price_decimal"),
            ArrayExpr.of(col("tag1"), col("tag2")).as("tags"),
            col("flags").unary("~").as("inverted")
        ).from(tbl("records"))
            .where(
                AndPredicate.of(
                    UnaryPredicate.of(col("metadata").op("@>", lit("{\"active\":true}"))),
                    col("amount").gt(lit(100))
                )
            );

        var back = roundTrip(query, SelectQuery.class);

        assertEquals(5, back.items().size());
        assertInstanceOf(BinaryOperatorExpr.class, ((ExprSelectItem) back.items().get(1)).expr());
        assertInstanceOf(CastExpr.class, ((ExprSelectItem) back.items().get(2)).expr());
        assertInstanceOf(ArrayExpr.class, ((ExprSelectItem) back.items().get(3)).expr());
        assertInstanceOf(UnaryOperatorExpr.class, ((ExprSelectItem) back.items().get(4)).expr());
    }

    @Test
    @DisplayName("JSON format verification for all expression subtypes")
    void jsonFormat_allSubtypes() throws Exception {
        var cast = CastExpr.of(lit(1), type("bigint"));
        var array = ArrayExpr.of(lit(1), lit(2));
        var binary = BinaryOperatorExpr.of(col("a"), "->", col("b"));
        var unary = UnaryOperatorExpr.of("~", col("c"));

        String castJson = mapper.writeValueAsString(cast);
        assertTrue(castJson.contains("\"kind\" : \"cast\""));

        String arrayJson = mapper.writeValueAsString(array);
        assertTrue(arrayJson.contains("\"kind\" : \"array\""));

        String binaryJson = mapper.writeValueAsString(binary);
        assertTrue(binaryJson.contains("\"kind\" : \"binary-op\""));

        String unaryJson = mapper.writeValueAsString(unary);
        assertTrue(unaryJson.contains("\"kind\" : \"unary-op\""));
    }
}
