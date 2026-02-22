package io.sqm.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sqm.core.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests JSON serialization/deserialization for all parameter expression types.
 * Covers AnonymousParamExpr, NamedParamExpr, and OrdinalParamExpr.
 */
public class ParamExprJsonTest {

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

    @Test
    @DisplayName("AnonymousParamExpr: serialize and deserialize anonymous parameter")
    void anonymousParam_roundTrip() throws Exception {
        var param = ParamExpr.anonymous();

        var back = roundTrip(param, AnonymousParamExpr.class);

        assertNotNull(back);
        assertInstanceOf(AnonymousParamExpr.class, back);

        JsonNode node = toTree(param);
        assertEquals("anonymous-param", node.path("kind").asText());
    }

    @Test
    @DisplayName("NamedParamExpr: serialize and deserialize named parameter")
    void namedParam_roundTrip() throws Exception {
        var param = ParamExpr.named("userId");

        var back = roundTrip(param, NamedParamExpr.class);

        assertNotNull(back);
        assertInstanceOf(NamedParamExpr.class, back);
        assertEquals("userId", back.name());

        JsonNode node = toTree(param);
        assertEquals("named-param", node.path("kind").asText());
        assertEquals("userId", node.path("name").asText());
    }

    @Test
    @DisplayName("OrdinalParamExpr: serialize and deserialize ordinal parameter")
    void ordinalParam_roundTrip() throws Exception {
        var param = ParamExpr.ordinal(1);

        var back = roundTrip(param, OrdinalParamExpr.class);

        assertNotNull(back);
        assertInstanceOf(OrdinalParamExpr.class, back);
        assertEquals(1, back.index());

        JsonNode node = toTree(param);
        assertEquals("ordinal-param", node.path("kind").asText());
        assertEquals(1, node.path("index").asInt());
    }

    @Test
    @DisplayName("ParamExpr as Expression: polymorphic deserialization of named param")
    void namedParam_asExpression() throws Exception {
        var param = ParamExpr.named("email");

        String json = mapper.writeValueAsString(param);
        Expression back = mapper.readValue(json, Expression.class);

        assertInstanceOf(NamedParamExpr.class, back);
        assertEquals("email", ((NamedParamExpr) back).name());
    }

    @Test
    @DisplayName("ParamExpr as Expression: polymorphic deserialization of ordinal param")
    void ordinalParam_asExpression() throws Exception {
        var param = ParamExpr.ordinal(2);

        String json = mapper.writeValueAsString(param);
        Expression back = mapper.readValue(json, Expression.class);

        assertInstanceOf(OrdinalParamExpr.class, back);
        assertEquals(2, ((OrdinalParamExpr) back).index());
    }

    @Test
    @DisplayName("ParamExpr as Expression: polymorphic deserialization of anonymous param")
    void anonymousParam_asExpression() throws Exception {
        var param = ParamExpr.anonymous();

        String json = mapper.writeValueAsString(param);
        Expression back = mapper.readValue(json, Expression.class);

        assertInstanceOf(AnonymousParamExpr.class, back);
    }

    @Test
    @DisplayName("Named parameter in WHERE clause")
    void namedParam_inWhereClause() throws Exception {
        var query = select(col("id"), col("name"))
            .from(tbl("users"))
            .where(col("id").eq(ParamExpr.named("userId")))
            .build();

        var back = roundTrip(query, SelectQuery.class);

        assertNotNull(back.where());
        var predicate = back.where().<ComparisonPredicate>matchPredicate().comparison(c -> c).orElse(null);
        assertNotNull(predicate);
        assertInstanceOf(NamedParamExpr.class, predicate.rhs());

        var paramExpr = (NamedParamExpr) predicate.rhs();
        assertEquals("userId", paramExpr.name());

        JsonNode node = toTree(query);
        assertEquals("named-param", node.path("where").path("rhs").path("kind").asText());
        assertEquals("userId", node.path("where").path("rhs").path("name").asText());
    }

    @Test
    @DisplayName("Ordinal parameter in WHERE clause")
    void ordinalParam_inWhereClause() throws Exception {
        var query = select(col("*"))
            .from(tbl("products"))
            .where(col("price").gt(ParamExpr.ordinal(1)))
            .build();

        var back = roundTrip(query, SelectQuery.class);

        var predicate = back.where().<ComparisonPredicate>matchPredicate().comparison(c -> c).orElse(null);
        assertInstanceOf(OrdinalParamExpr.class, predicate.rhs());

        var paramExpr = (OrdinalParamExpr) predicate.rhs();
        assertEquals(1, paramExpr.index());
    }

    @Test
    @DisplayName("Anonymous parameter in WHERE clause")
    void anonymousParam_inWhereClause() throws Exception {
        var query = select(col("*"))
            .from(tbl("orders"))
            .where(col("status").eq(ParamExpr.anonymous()))
            .build();

        var back = roundTrip(query, SelectQuery.class);

        var predicate = back.where().<ComparisonPredicate>matchPredicate().comparison(c -> c).orElse(null);
        assertInstanceOf(AnonymousParamExpr.class, predicate.rhs());
    }

    @Test
    @DisplayName("Multiple named parameters in query")
    void multipleNamedParams_inQuery() throws Exception {
        var query = select(col("*"))
            .from(tbl("users"))
            .where(
                col("username").eq(ParamExpr.named("username"))
                    .and(col("status").eq(ParamExpr.named("status")))
            )
            .build();

        var back = roundTrip(query, SelectQuery.class);

        var predicate = back.where().<CompositePredicate>matchPredicate().and(a -> a).orElse(null);
        assertNotNull(predicate);

        var left = ((AndPredicate) predicate).lhs().<ComparisonPredicate>matchPredicate().comparison(c -> c).orElse(null);
        var right = ((AndPredicate) predicate).rhs().<ComparisonPredicate>matchPredicate().comparison(c -> c).orElse(null);

        assertInstanceOf(NamedParamExpr.class, left.rhs());
        assertInstanceOf(NamedParamExpr.class, right.rhs());

        assertEquals("username", ((NamedParamExpr) left.rhs()).name());
        assertEquals("status", ((NamedParamExpr) right.rhs()).name());
    }

    @Test
    @DisplayName("Ordinal parameters in IN clause")
    void ordinalParams_inInClause() throws Exception {
        var query = select(col("*"))
            .from(tbl("products"))
            .where(col("category_id").in(
                ParamExpr.ordinal(1),
                ParamExpr.ordinal(2),
                ParamExpr.ordinal(3)
            ))
            .build();

        var back = roundTrip(query, SelectQuery.class);

        var predicate = back.where().<InPredicate>matchPredicate().in(i -> i).orElse(null);
        assertNotNull(predicate);

        var valueSet = predicate.rhs().<RowExpr>matchValueSet().row(r -> r).orElse(null);
        assertNotNull(valueSet);
        assertEquals(3, valueSet.items().size());

        for (int i = 0; i < 3; i++) {
            var item = valueSet.items().get(i);
            assertInstanceOf(OrdinalParamExpr.class, item);
            assertEquals(i + 1, ((OrdinalParamExpr) item).index());
        }
    }

    @Test
    @DisplayName("Named parameter in function argument")
    void namedParam_inFunctionArg() throws Exception {
        var query = select(
            func("CONCAT", arg(col("first_name")), arg(ParamExpr.named("separator")), arg(col("last_name"))).as("full_name")
        ).from(tbl("users")).build();

        var back = roundTrip(query, SelectQuery.class);

        var selectItem = (ExprSelectItem) back.items().getFirst();
        var funcExpr = (FunctionExpr) selectItem.expr();

        assertEquals(3, funcExpr.args().size());
        var arg1 = funcExpr.args().get(1).matchArg().exprArg(a -> a.expr()).orElse(null);
        assertInstanceOf(NamedParamExpr.class, arg1);
        assertEquals("separator", ((NamedParamExpr) arg1).name());
    }

    @Test
    @DisplayName("Parameter in BETWEEN predicate")
    void params_inBetweenPredicate() throws Exception {
        var query = select(col("*"))
            .from(tbl("orders"))
            .where(col("amount").between(ParamExpr.named("minAmount"), ParamExpr.named("maxAmount")))
            .build();

        var back = roundTrip(query, SelectQuery.class);

        var predicate = back.where().<BetweenPredicate>matchPredicate().between(b -> b).orElse(null);
        assertNotNull(predicate);

        assertInstanceOf(NamedParamExpr.class, predicate.lower());
        assertInstanceOf(NamedParamExpr.class, predicate.upper());

        assertEquals("minAmount", ((NamedParamExpr) predicate.lower()).name());
        assertEquals("maxAmount", ((NamedParamExpr) predicate.upper()).name());
    }

    @Test
    @DisplayName("Parameter JSON format verification")
    void param_jsonStringFormat() throws Exception {
        var param = ParamExpr.named("testParam");
        String json = mapper.writeValueAsString(param);

        assertTrue(json.contains("\"kind\" : \"named-param\""));
        assertTrue(json.contains("\"name\" : \"testParam\""));

        var ordinalParam = ParamExpr.ordinal(5);
        String ordinalJson = mapper.writeValueAsString(ordinalParam);

        assertTrue(ordinalJson.contains("\"kind\" : \"ordinal-param\""));
        assertTrue(ordinalJson.contains("\"index\" : 5"));
    }

    @Test
    @DisplayName("Parameter with complex names")
    void param_complexNames() throws Exception {
        var param1 = ParamExpr.named("user.email");
        var param2 = ParamExpr.named("order_item_id");
        var param3 = ParamExpr.named("maxDate2023");

        var back1 = roundTrip(param1, NamedParamExpr.class);
        var back2 = roundTrip(param2, NamedParamExpr.class);
        var back3 = roundTrip(param3, NamedParamExpr.class);

        assertEquals("user.email", back1.name());
        assertEquals("order_item_id", back2.name());
        assertEquals("maxDate2023", back3.name());
    }

    @Test
    @DisplayName("Ordinal parameters with various indices")
    void ordinalParam_variousIndices() throws Exception {
        for (int i = 1; i <= 10; i++) {
            var param = ParamExpr.ordinal(i);
            var back = roundTrip(param, OrdinalParamExpr.class);
            assertEquals(i, back.index());
        }
    }

    @Test
    @DisplayName("Parameter in UPDATE query (using JSON deserialization)")
    void param_inUpdateLikeStructure() throws Exception {
        // Create a predicate similar to UPDATE WHERE clause
        var predicate = col("id").eq(ParamExpr.named("id"))
            .and(col("version").eq(ParamExpr.ordinal(1)));

        var back = roundTrip(predicate, CompositePredicate.class);

        var andPred = back.<CompositePredicate>matchPredicate().and(a -> a).orElse(null);
        assertNotNull(andPred);

        var left = ((AndPredicate) andPred).lhs().<ComparisonPredicate>matchPredicate().comparison(c -> c).orElse(null);
        var right = ((AndPredicate) andPred).rhs().<ComparisonPredicate>matchPredicate().comparison(c -> c).orElse(null);

        assertInstanceOf(NamedParamExpr.class, left.rhs());
        assertInstanceOf(OrdinalParamExpr.class, right.rhs());
    }

    @Test
    @DisplayName("Mixed parameter types in complex query")
    void mixedParams_complexQuery() throws Exception {
        var query = select(col("*"))
            .from(tbl("transactions"))
            .where(
                col("user_id").eq(ParamExpr.named("userId"))
                    .and(col("amount").gt(ParamExpr.ordinal(1)))
                    .and(col("status").eq(ParamExpr.anonymous()))
            )
            .build();

        var back = roundTrip(query, SelectQuery.class);

        assertNotNull(back.where());
        // Verify it deserializes correctly
        String json = mapper.writeValueAsString(query);
        assertTrue(json.contains("\"named-param\""));
        assertTrue(json.contains("\"ordinal-param\""));
        assertTrue(json.contains("\"anonymous-param\""));
    }
}
