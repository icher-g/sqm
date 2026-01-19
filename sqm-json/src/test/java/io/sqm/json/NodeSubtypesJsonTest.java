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
 * Tests JSON serialization/deserialization for Node subtypes:
 * TypeName and DistinctSpec.
 */
public class NodeSubtypesJsonTest {

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

    /* ==================== TypeName Tests ==================== */

    @Test
    @DisplayName("TypeName: simple type name")
    void typeName_simple() throws Exception {
        var typeName = type("integer");

        var back = roundTrip(typeName, TypeName.class);

        assertNotNull(back);
        assertEquals(1, back.qualifiedName().size());
        assertEquals("integer", back.qualifiedName().getFirst());
        assertEquals(0, back.arrayDims());
        assertFalse(back.keyword().isPresent());

        JsonNode node = toTree(typeName);
        assertEquals("typeName", node.path("kind").asText());
    }

    @Test
    @DisplayName("TypeName: qualified name (schema.type)")
    void typeName_qualified() throws Exception {
        var typeName = type("public", "custom_type");

        var back = roundTrip(typeName, TypeName.class);

        assertEquals(2, back.qualifiedName().size());
        assertEquals("public", back.qualifiedName().get(0));
        assertEquals("custom_type", back.qualifiedName().get(1));
    }

    @Test
    @DisplayName("TypeName: with modifiers")
    void typeName_withModifiers() throws Exception {
        var typeName = TypeName.of(List.of("decimal"), null, List.of(lit(10), lit(2)), 0, TimeZoneSpec.NONE);

        var back = roundTrip(typeName, TypeName.class);

        assertEquals("decimal", back.qualifiedName().getFirst());
        assertEquals(2, back.modifiers().size());
        assertEquals(10, ((LiteralExpr) back.modifiers().get(0)).value());
        assertEquals(2, ((LiteralExpr) back.modifiers().get(1)).value());
    }

    @Test
    @DisplayName("TypeName: single array dimension")
    void typeName_arraySingle() throws Exception {
        var typeName = type("text").array();

        var back = roundTrip(typeName, TypeName.class);

        assertEquals("text", back.qualifiedName().getFirst());
        assertEquals(1, back.arrayDims());
    }

    @Test
    @DisplayName("TypeName: multiple array dimensions")
    void typeName_arrayMultiple() throws Exception {
        var typeName = type("integer").array(3);

        var back = roundTrip(typeName, TypeName.class);

        assertEquals("integer", back.qualifiedName().getFirst());
        assertEquals(3, back.arrayDims());
    }

    @Test
    @DisplayName("TypeName: with time zone spec")
    void typeName_withTimeZone() throws Exception {
        var typeName = TypeName.of(List.of("timestamp"), null, List.of(), 0, TimeZoneSpec.WITH_TIME_ZONE);

        var back = roundTrip(typeName, TypeName.class);

        assertEquals("timestamp", back.qualifiedName().getFirst());
        assertEquals(TimeZoneSpec.WITH_TIME_ZONE, back.timeZoneSpec());
    }

    @Test
    @DisplayName("TypeName: without time zone spec")
    void typeName_withoutTimeZone() throws Exception {
        var typeName = TypeName.of(List.of("timestamp"), null, List.of(), 0, TimeZoneSpec.WITHOUT_TIME_ZONE);

        var back = roundTrip(typeName, TypeName.class);

        assertEquals("timestamp", back.qualifiedName().getFirst());
        assertEquals(TimeZoneSpec.WITHOUT_TIME_ZONE, back.timeZoneSpec());
    }

    @Test
    @DisplayName("TypeName: polymorphic deserialization as Node")
    void typeName_asNode() throws Exception {
        var typeName = type("varchar");

        String json = mapper.writeValueAsString(typeName);
        Node back = mapper.readValue(json, Node.class);

        assertInstanceOf(TypeName.class, back);
        assertEquals("varchar", ((TypeName) back).qualifiedName().getFirst());
    }

    @Test
    @DisplayName("TypeName in CastExpr")
    void typeName_inCastExpr() throws Exception {
        var castExpr = CastExpr.of(col("value"), type("bigint"));

        var back = roundTrip(castExpr, CastExpr.class);

        var typeName = back.type();
        assertEquals("bigint", typeName.qualifiedName().getFirst());
    }

    @Test
    @DisplayName("TypeName: complex with schema, modifiers, and array")
    void typeName_complex() throws Exception {
        var typeName = TypeName.of(
            List.of("pg_catalog", "varchar"),
            null,
            List.of(lit(255)),
            2,
            TimeZoneSpec.NONE
        );

        var back = roundTrip(typeName, TypeName.class);

        assertEquals(2, back.qualifiedName().size());
        assertEquals("pg_catalog", back.qualifiedName().get(0));
        assertEquals("varchar", back.qualifiedName().get(1));
        assertEquals(1, back.modifiers().size());
        assertEquals(255, ((LiteralExpr) back.modifiers().getFirst()).value());
        assertEquals(2, back.arrayDims());
    }

    @Test
    @DisplayName("TypeName: various common SQL types")
    void typeName_commonTypes() throws Exception {
        String[] types = {"integer", "bigint", "text", "varchar", "boolean", "timestamp", "date", "decimal", "json", "jsonb"};

        for (String typeName : types) {
            var type = type(typeName);
            var back = roundTrip(type, TypeName.class);
            assertEquals(typeName, back.qualifiedName().getFirst());
        }
    }

    @Test
    @DisplayName("TypeName JSON format verification")
    void typeName_jsonFormat() throws Exception {
        var typeName = type("integer").array();
        String json = mapper.writeValueAsString(typeName);

        assertTrue(json.contains("\"kind\" : \"typeName\""));
        assertTrue(json.contains("\"qualifiedName\""));
        assertTrue(json.contains("\"integer\""));
        assertTrue(json.contains("\"arrayDims\" : 1"));
    }

    /* ==================== DistinctSpec Tests ==================== */

    @Test
    @DisplayName("DistinctSpec.TRUE: serialize and deserialize")
    void distinctSpec_true() throws Exception {
        var distinctSpec = DistinctSpec.TRUE;

        var back = roundTrip(distinctSpec, DistinctSpec.class);

        assertNotNull(back);
        assertEquals(DistinctSpec.TRUE, back);

        JsonNode node = toTree(distinctSpec);
        assertEquals("distinctSpec", node.path("kind").asText());
    }

    @Test
    @DisplayName("DistinctSpec in SELECT query")
    void distinctSpec_inSelect() throws Exception {
        var query = select(col("id"), col("name"))
            .from(tbl("users"))
            .distinct(DistinctSpec.TRUE);

        var back = roundTrip(query, SelectQuery.class);

        assertNotNull(back.distinct());
        assertEquals(DistinctSpec.TRUE, back.distinct());
    }

    @Test
    @DisplayName("DistinctSpec: query without DISTINCT")
    void distinctSpec_noDistinct() throws Exception {
        var query = select(col("id"), col("name"))
            .from(tbl("users"));

        var back = roundTrip(query, SelectQuery.class);

        assertNull(back.distinct());
    }

    @Test
    @DisplayName("DistinctSpec: polymorphic deserialization as Node")
    void distinctSpec_asNode() throws Exception {
        var distinctSpec = DistinctSpec.TRUE;

        String json = mapper.writeValueAsString(distinctSpec);
        Node back = mapper.readValue(json, Node.class);

        assertInstanceOf(DistinctSpec.class, back);
        assertEquals(DistinctSpec.TRUE, back);
    }

    @Test
    @DisplayName("DistinctSpec JSON format verification")
    void distinctSpec_jsonFormat() throws Exception {
        var distinctSpec = DistinctSpec.TRUE;
        String json = mapper.writeValueAsString(distinctSpec);

        assertTrue(json.contains("\"kind\" : \"distinctSpec\""));
    }

    @Test
    @DisplayName("DistinctSpec in complex query")
    void distinctSpec_complexQuery() throws Exception {
        var query = select(col("category"), func("count", starArg()).as("cnt"))
            .from(tbl("products"))
            .distinct(DistinctSpec.TRUE)
            .where(col("price").gt(lit(100)))
            .groupBy(group(col("category")))
            .having(func("count", starArg()).gt(lit(10)))
            .orderBy(order(col("category")).asc());

        var back = roundTrip(query, SelectQuery.class);

        assertNotNull(back.distinct());
        assertEquals(DistinctSpec.TRUE, back.distinct());
        assertEquals(2, back.items().size());
        assertNotNull(back.where());
        assertNotNull(back.groupBy());
        assertNotNull(back.having());
        assertNotNull(back.orderBy());
    }

    /* ==================== Integration Tests ==================== */

    @Test
    @DisplayName("Complex query with TypeName in CAST and DistinctSpec")
    void integration_castAndDistinct() throws Exception {
        var query = select(
            col("id"),
            col("price").cast(TypeName.of(List.of("decimal"), null, List.of(lit(10), lit(2)), 0, TimeZoneSpec.NONE)).as("price_decimal")
        )
        .from(tbl("products"))
        .distinct(DistinctSpec.TRUE)
        .where(col("active").eq(lit(true)));

        var back = roundTrip(query, SelectQuery.class);

        assertNotNull(back.distinct());
        assertEquals(DistinctSpec.TRUE, back.distinct());

        var selectItem = (ExprSelectItem) back.items().get(1);
        assertInstanceOf(CastExpr.class, selectItem.expr());

        var castExpr = (CastExpr) selectItem.expr();
        assertEquals("decimal", castExpr.type().qualifiedName().getFirst());
        assertEquals(2, castExpr.type().modifiers().size());
    }

    @Test
    @DisplayName("Query with multiple type casts")
    void integration_multipleCasts() throws Exception {
        var query = select(
            col("id").cast(type("bigint")).as("id_bigint"),
            col("amount").cast(TypeName.of(List.of("numeric"), null, List.of(lit(12), lit(4)), 0, TimeZoneSpec.NONE)).as("amount_numeric"),
            col("data").cast(type("jsonb")).as("data_json"),
            col("tags").cast(type("text").array()).as("tags_array")
        ).from(tbl("records"))
        .distinct(DistinctSpec.TRUE);

        var back = roundTrip(query, SelectQuery.class);

        assertEquals(4, back.items().size());
        assertNotNull(back.distinct());

        // Verify each cast expression
        for (int i = 0; i < 4; i++) {
            var item = (ExprSelectItem) back.items().get(i);
            assertInstanceOf(CastExpr.class, item.expr());
        }
    }

    @Test
    @DisplayName("TypeName with various time zone specs")
    void typeName_allTimeZoneSpecs() throws Exception {
        var none = TypeName.of(List.of("timestamp"), null, List.of(), 0, TimeZoneSpec.NONE);
        var withTz = TypeName.of(List.of("timestamp"), null, List.of(), 0, TimeZoneSpec.WITH_TIME_ZONE);
        var withoutTz = TypeName.of(List.of("timestamp"), null, List.of(), 0, TimeZoneSpec.WITHOUT_TIME_ZONE);

        var backNone = roundTrip(none, TypeName.class);
        var backWithTz = roundTrip(withTz, TypeName.class);
        var backWithoutTz = roundTrip(withoutTz, TypeName.class);

        assertEquals(TimeZoneSpec.NONE, backNone.timeZoneSpec());
        assertEquals(TimeZoneSpec.WITH_TIME_ZONE, backWithTz.timeZoneSpec());
        assertEquals(TimeZoneSpec.WITHOUT_TIME_ZONE, backWithoutTz.timeZoneSpec());
    }

    @Test
    @DisplayName("TypeName with keyword")
    void typeName_withKeyword() throws Exception {
        var typeName = TypeName.of(List.of(), TypeKeyword.DOUBLE_PRECISION, List.of(), 0, TimeZoneSpec.NONE);

        var back = roundTrip(typeName, TypeName.class);

        assertTrue(back.keyword().isPresent());
        assertEquals(TypeKeyword.DOUBLE_PRECISION, back.keyword().get());
        assertTrue(back.qualifiedName().isEmpty());
    }

    @Test
    @DisplayName("Complete SELECT with all node subtypes")
    void integration_allNodeSubtypes() throws Exception {
        var query = select(
            col("id"),
            col("name"),
            col("price").cast(TypeName.of(List.of("decimal"), null, List.of(lit(10), lit(2)), 0, TimeZoneSpec.NONE)).as("price_decimal"),
            func("upper", arg(col("status"))).as("upper_status")
        )
        .from(tbl("products"))
        .distinct(DistinctSpec.TRUE)
        .where(
            col("name").like("%Phone%")
                .and(col("price").gt(lit(100)))
        )
        .groupBy(group(col("id")), group(col("name")))
        .having(func("count", starArg()).gt(lit(0)))
        .orderBy(order(col("name")).asc())
        .limit(10L)
        .offset(0L);

        var back = roundTrip(query, SelectQuery.class);

        // Verify all components
        assertNotNull(back.distinct());
        assertEquals(4, back.items().size());
        assertNotNull(back.from());
        assertNotNull(back.where());
        assertNotNull(back.groupBy());
        assertNotNull(back.having());
        assertNotNull(back.orderBy());
        assertNotNull(back.limit());
        assertNotNull(back.offset());

        // Verify JSON structure
        JsonNode node = toTree(query);
        assertEquals("select", node.path("kind").asText());
        assertTrue(node.has("distinctSpec"));
        assertTrue(node.has("items"));
        assertTrue(node.has("tableRef"));
        assertTrue(node.has("where"));
        assertTrue(node.has("groupBy"));
        assertTrue(node.has("having"));
        assertTrue(node.has("orderBy"));
        assertTrue(node.has("limitOffset"));
    }
}
