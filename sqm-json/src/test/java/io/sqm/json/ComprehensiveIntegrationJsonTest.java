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
 * Comprehensive integration tests combining all modified mixin types:
 * ArithmeticExpr, ParamExpr, CastExpr, ArrayExpr, BinaryOperatorExpr,
 * UnaryOperatorExpr, LikePredicate, RegexPredicate, TypeName, and DistinctSpec.
 */
public class ComprehensiveIntegrationJsonTest {

    private final ObjectMapper mapper = SqmJsonMixins.createPretty();

    @SuppressWarnings("unchecked")
    private <T> T roundTrip(T value) throws Exception {
        String json = mapper.writeValueAsString(value);
        T back = mapper.readValue(json, (Class<T>) SelectQuery.class);
        assertEquals(value, back, "round-trip equality failed");
        return back;
    }

    private JsonNode toTree(Object value) throws Exception {
        return mapper.readTree(mapper.writeValueAsBytes(value));
    }

    @Test
    @DisplayName("Integration: E-commerce query with arithmetic, params, and casts")
    void ecommerceQuery() throws Exception {
        // SELECT DISTINCT
        //   id,
        //   name,
        //   (price * quantity) - discount AS total_amount,
        //   CAST(created_at AS date) AS order_date
        // FROM orders
        // WHERE
        //   status = :status
        //   AND (price * quantity) > ?1
        //   AND name LIKE '%Phone%'
        // ORDER BY total_amount DESC
        // LIMIT 100

        var query = select(
            col("id"),
            col("name"),
            col("price").mul(col("quantity")).sub(col("discount")).as("total_amount"),
            col("created_at").cast(type("date")).as("order_date")
        )
            .from(tbl("orders"))
            .distinct(DistinctSpec.TRUE)
            .where(
                col("status").eq(ParamExpr.named("status"))
                    .and(col("price").mul(col("quantity")).gt(ParamExpr.ordinal(1)))
                    .and(col("name").like("%Phone%"))
            )
            .orderBy(order("total_amount").desc())
            .limit(100L)
            .build();

        var back = roundTrip(query);

        // Verify DISTINCT
        assertNotNull(back.distinct());
        assertEquals(DistinctSpec.TRUE, back.distinct());

        // Verify SELECT items
        assertEquals(4, back.items().size());

        // Verify arithmetic in SELECT
        var totalAmountItem = (ExprSelectItem) back.items().get(2);
        assertEquals("total_amount", totalAmountItem.alias().value());
        assertInstanceOf(SubArithmeticExpr.class, totalAmountItem.expr());

        // Verify CAST in SELECT
        var orderDateItem = (ExprSelectItem) back.items().get(3);
        assertEquals("order_date", orderDateItem.alias().value());
        assertInstanceOf(CastExpr.class, orderDateItem.expr());

        // Verify WHERE clause predicates
        var wherePredicate = back.where().<CompositePredicate>matchPredicate().and(a -> a).orElse(null);
        assertNotNull(wherePredicate);

        // Verify JSON structure
        JsonNode node = toTree(query);
        String json = node.toString();
        assertTrue(json.contains("\"distinctSpec\""));
        assertTrue(json.contains("\"mul\""));
        assertTrue(json.contains("\"sub\""));
        assertTrue(json.contains("\"cast\""));
        assertTrue(json.contains("\"named-param\""));
        assertTrue(json.contains("\"ordinal-param\""));
        assertTrue(json.contains("\"like\""));
    }

    @Test
    @DisplayName("Integration: JSON document query with operators and regex")
    void jsonDocumentQuery() throws Exception {
        // SELECT
        //   id,
        //   data -> 'user' ->> 'name' AS user_name,
        //   CAST(data -> 'metadata' -> 'score' AS integer) AS score,
        //   ARRAY[tag1, tag2, tag3] AS tags
        // FROM documents
        // WHERE
        //   data @> :filter
        //   AND email ~ '^[a-z]+@company\.com$'
        //   AND title ILIKE '%important%'

        var query = select(
            col("id"),
            col("data").op("->", lit("user")).op("->>", lit("name")).as("user_name"),
            col("data").op("->", lit("metadata")).op("->", lit("score")).cast(type("integer")).as("score"),
            ArrayExpr.of(col("tag1"), col("tag2"), col("tag3")).as("tags")
        )
            .from(tbl("documents"))
            .where(
                AndPredicate.of(
                    AndPredicate.of(
                        UnaryPredicate.of(col("data").op("@>", ParamExpr.named("filter"))),
                        RegexPredicate.of(col("email"), lit("^[a-z]+@company\\.com$"), false)
                    ),
                    col("title").ilike("%important%")
                )
            )
            .build();

        var back = roundTrip(query);

        // Verify SELECT items
        assertEquals(4, back.items().size());

        // Verify binary operators in SELECT
        var userNameItem = (ExprSelectItem) back.items().get(1);
        assertInstanceOf(BinaryOperatorExpr.class, userNameItem.expr());

        // Verify CAST with binary operator
        var scoreItem = (ExprSelectItem) back.items().get(2);
        assertInstanceOf(CastExpr.class, scoreItem.expr());

        // Verify ARRAY
        var tagsItem = (ExprSelectItem) back.items().get(3);
        assertInstanceOf(ArrayExpr.class, tagsItem.expr());

        // Verify JSON structure
        JsonNode node = toTree(query);
        String json = node.toString();
        assertTrue(json.contains("\"binary-op\""));
        assertTrue(json.contains("\"->\""));
        assertTrue(json.contains("\"->>\""));
        assertTrue(json.contains("\"cast\""));
        assertTrue(json.contains("\"array\""));
        assertTrue(json.contains("\"regex\""));
        assertTrue(json.contains("\"like\""));
        assertTrue(json.contains("\"ILIKE\""));
    }

    @Test
    @DisplayName("Integration: Financial calculations with negation and modulo")
    void financialCalculationsQuery() throws Exception {
        // SELECT
        //   account_id,
        //   -balance + adjustment AS adjusted_balance,
        //   amount % 100 AS cents,
        //   ~flags AS inverted_flags,
        //   CAST(total / count AS decimal(10,2)) AS average
        // FROM accounts
        // WHERE
        //   balance + adjustment BETWEEN :min AND :max
        //   AND account_type NOT LIKE 'TEST_%'

        var query = select(
            col("account_id"),
            col("balance").neg().add(col("adjustment")).as("adjusted_balance"),
            col("amount").mod(lit(100)).as("cents"),
            col("flags").unary("~").as("inverted_flags"),
            col("total").div(col("count")).cast(TypeName.of(QualifiedName.of(List.of("decimal")), null, List.of(lit(10), lit(2)), 0, TimeZoneSpec.NONE)).as("average")
        )
            .from(tbl("accounts"))
            .where(
                col("balance").add(col("adjustment")).between(ParamExpr.named("min"), ParamExpr.named("max"))
                    .and(col("account_type").notLike("TEST_%"))
            )
            .build();

        var back = roundTrip(query);

        // Verify SELECT items
        assertEquals(5, back.items().size());

        // Verify negation + addition
        var adjustedBalanceItem = (ExprSelectItem) back.items().get(1);
        assertInstanceOf(AddArithmeticExpr.class, adjustedBalanceItem.expr());
        var addExpr = (AddArithmeticExpr) adjustedBalanceItem.expr();
        assertInstanceOf(NegativeArithmeticExpr.class, addExpr.lhs());

        // Verify modulo
        var centsItem = (ExprSelectItem) back.items().get(2);
        assertInstanceOf(ModArithmeticExpr.class, centsItem.expr());

        // Verify unary operator
        var invertedFlagsItem = (ExprSelectItem) back.items().get(3);
        assertInstanceOf(UnaryOperatorExpr.class, invertedFlagsItem.expr());

        // Verify CAST with division
        var averageItem = (ExprSelectItem) back.items().get(4);
        assertInstanceOf(CastExpr.class, averageItem.expr());
        var castExpr = (CastExpr) averageItem.expr();
        assertInstanceOf(DivArithmeticExpr.class, castExpr.expr());

        // Verify JSON structure
        JsonNode node = toTree(query);
        String json = node.toString();
        assertTrue(json.contains("\"neg\""));
        assertTrue(json.contains("\"add\""));
        assertTrue(json.contains("\"mod\""));
        assertTrue(json.contains("\"unary-op\""));
        assertTrue(json.contains("\"div\""));
        assertTrue(json.contains("\"cast\""));
        assertTrue(json.contains("\"named-param\""));
        assertTrue(json.contains("\"like\""));
        assertTrue(json.contains("\"negated\":true"));
    }

    @Test
    @DisplayName("Integration: Complex aggregation with all arithmetic operations")
    void complexAggregationQuery() throws Exception {
        // SELECT DISTINCT
        //   category,
        //   SUM(price * quantity) AS total_revenue,
        //   SUM((price + tax) * quantity - discount) AS total_with_tax,
        //   AVG(CAST(price AS decimal(10,2))) AS avg_price,
        //   COUNT(*) AS count
        // FROM sales
        // WHERE
        //   sale_date BETWEEN ?1 AND ?2
        //   AND category LIKE :category_pattern
        //   AND status NOT LIKE 'CANCELLED%'
        // GROUP BY category
        // HAVING SUM(price * quantity) > :min_revenue

        var query = select(
            col("category"),
            func("sum", arg(col("price").mul(col("quantity")))).as("total_revenue"),
            func("sum", arg(
                col("price").add(col("tax")).mul(col("quantity")).sub(col("discount"))
            )).as("total_with_tax"),
            func("avg", arg(col("price").cast(TypeName.of(QualifiedName.of(List.of("decimal")), null, List.of(lit(10), lit(2)), 0, TimeZoneSpec.NONE)))).as("avg_price"),
            func("count", starArg()).as("count")
        )
            .from(tbl("sales"))
            .distinct(DistinctSpec.TRUE)
            .where(
                col("sale_date").between(ParamExpr.ordinal(1), ParamExpr.ordinal(2))
                    .and(col("category").like(ParamExpr.named("category_pattern")))
                    .and(col("status").notLike("CANCELLED%"))
            )
            .groupBy(group(col("category")))
            .having(
                func("sum", arg(col("price").mul(col("quantity")))).gt(ParamExpr.named("min_revenue"))
            )
            .build();

        var back = roundTrip(query);

        // Verify DISTINCT
        assertNotNull(back.distinct());

        // Verify SELECT items
        assertEquals(5, back.items().size());

        // Verify function with arithmetic
        var totalRevenueItem = (ExprSelectItem) back.items().get(1);
        var funcExpr = (FunctionExpr) totalRevenueItem.expr();
        assertEquals("sum", funcExpr.name().values().getLast());

        // Verify complex arithmetic in function
        var totalWithTaxItem = (ExprSelectItem) back.items().get(2);
        var complexFuncExpr = (FunctionExpr) totalWithTaxItem.expr();
        var arg = complexFuncExpr.args().getFirst().matchArg().exprArg(a -> a.expr()).orElse(null);
        assertInstanceOf(SubArithmeticExpr.class, arg);

        // Verify CAST in function
        var avgPriceItem = (ExprSelectItem) back.items().get(3);
        var avgFuncExpr = (FunctionExpr) avgPriceItem.expr();
        var avgArg = avgFuncExpr.args().getFirst().matchArg().exprArg(a -> a.expr()).orElse(null);
        assertInstanceOf(CastExpr.class, avgArg);

        // Verify WHERE with BETWEEN and LIKE
        assertNotNull(back.where());

        // Verify GROUP BY and HAVING
        assertNotNull(back.groupBy());
        assertNotNull(back.having());

        // Verify JSON structure
        JsonNode node = toTree(query);
        String json = node.toString();
        assertTrue(json.contains("\"mul\""));
        assertTrue(json.contains("\"add\""));
        assertTrue(json.contains("\"sub\""));
        assertTrue(json.contains("\"cast\""));
        assertTrue(json.contains("\"ordinal-param\""));
        assertTrue(json.contains("\"named-param\""));
        assertTrue(json.contains("\"like\""));
        assertTrue(json.contains("\"between\""));
    }

    @Test
    @DisplayName("Integration: All expression types in single query")
    void allExpressionTypesQuery() throws Exception {
        var query = select(
            // Column
            col("id"),
            // Arithmetic: add, sub, mul, div, mod, neg
            col("a").add(col("b")).as("addition"),
            col("c").sub(col("d")).as("subtraction"),
            col("e").mul(col("f")).as("multiplication"),
            col("g").div(col("h")).as("division"),
            col("i").mod(col("j")).as("modulo"),
            col("k").neg().as("negation"),
            // Cast
            col("value").cast(type("bigint")).as("value_bigint"),
            // Array
            ArrayExpr.of(col("x"), col("y"), col("z")).as("coords"),
            // Binary operator
            col("data").op("->", lit("key")).as("json_field"),
            // Unary operator
            col("mask").unary("~").as("inverted_mask"),
            // Function with param
            func("concat", arg(col("first")), arg(ParamExpr.named("sep")), arg(col("last"))).as("full_name")
        )
            .from(tbl("test_table"))
            .distinct(DistinctSpec.TRUE)
            .where(
                // Comparison with param
                col("id").eq(ParamExpr.ordinal(1))
                    // LIKE
                    .and(col("name").like("%test%"))
                    // NOT LIKE
                    .and(col("email").notLike("%.test"))
                    // REGEX
                    .and(RegexPredicate.of(col("code"), lit("^[A-Z]{3}$"), false))
                    // Arithmetic in WHERE
                    .and(col("price").add(col("tax")).gt(lit(100)))
            )
            .orderBy(order(col("id")).asc())
            .limit(50L)
            .offset(10L)
            .build();

        var back = roundTrip(query);

        // Comprehensive verification
        assertNotNull(back.distinct());
        assertEquals(12, back.items().size());
        assertNotNull(back.from());
        assertNotNull(back.where());
        assertNotNull(back.orderBy());
        assertNotNull(back.limitOffset().limit());
        assertNotNull(back.limitOffset().offset());

        // Verify each SELECT item type
        assertInstanceOf(ColumnExpr.class, ((ExprSelectItem) back.items().get(0)).expr());
        assertInstanceOf(AddArithmeticExpr.class, ((ExprSelectItem) back.items().get(1)).expr());
        assertInstanceOf(SubArithmeticExpr.class, ((ExprSelectItem) back.items().get(2)).expr());
        assertInstanceOf(MulArithmeticExpr.class, ((ExprSelectItem) back.items().get(3)).expr());
        assertInstanceOf(DivArithmeticExpr.class, ((ExprSelectItem) back.items().get(4)).expr());
        assertInstanceOf(ModArithmeticExpr.class, ((ExprSelectItem) back.items().get(5)).expr());
        assertInstanceOf(NegativeArithmeticExpr.class, ((ExprSelectItem) back.items().get(6)).expr());
        assertInstanceOf(CastExpr.class, ((ExprSelectItem) back.items().get(7)).expr());
        assertInstanceOf(ArrayExpr.class, ((ExprSelectItem) back.items().get(8)).expr());
        assertInstanceOf(BinaryOperatorExpr.class, ((ExprSelectItem) back.items().get(9)).expr());
        assertInstanceOf(UnaryOperatorExpr.class, ((ExprSelectItem) back.items().get(10)).expr());
        assertInstanceOf(FunctionExpr.class, ((ExprSelectItem) back.items().get(11)).expr());

        // Verify JSON contains all kinds
        JsonNode node = toTree(query);
        String json = node.toString();
        assertTrue(json.contains("\"add\""));
        assertTrue(json.contains("\"sub\""));
        assertTrue(json.contains("\"mul\""));
        assertTrue(json.contains("\"div\""));
        assertTrue(json.contains("\"mod\""));
        assertTrue(json.contains("\"neg\""));
        assertTrue(json.contains("\"cast\""));
        assertTrue(json.contains("\"array\""));
        assertTrue(json.contains("\"binary-op\""));
        assertTrue(json.contains("\"unary-op\""));
        assertTrue(json.contains("\"named-param\""));
        assertTrue(json.contains("\"ordinal-param\""));
        assertTrue(json.contains("\"like\""));
        assertTrue(json.contains("\"regex\""));
        assertTrue(json.contains("\"distinctSpec\""));
        assertTrue(json.contains("\"typeName\""));
    }

    @Test
    @DisplayName("Integration: Nested expressions stress test")
    void nestedExpressionsStressTest() throws Exception {
        // ((a + b) * (c - d)) / ((e % f) + (-g))
        var complexExpr = col("a").add(col("b"))
            .mul(col("c").sub(col("d")))
            .div(
                col("e").mod(col("f")).add(col("g").neg())
            );

        var query = select(
            complexExpr.as("complex_result"),
            complexExpr.cast(TypeName.of(QualifiedName.of(List.of("decimal")), null, List.of(lit(20), lit(6)), 0, TimeZoneSpec.NONE)).as("complex_decimal")
        )
            .from(tbl("calculations"))
            .where(
                complexExpr.gt(ParamExpr.named("threshold"))
                    .and(col("description").like("%calculation%"))
            )
            .build();

        var back = roundTrip(query);

        // Verify nested structure
        var item1 = (ExprSelectItem) back.items().getFirst();
        assertInstanceOf(DivArithmeticExpr.class, item1.expr());

        var divExpr = (DivArithmeticExpr) item1.expr();
        assertInstanceOf(MulArithmeticExpr.class, divExpr.lhs());
        assertInstanceOf(AddArithmeticExpr.class, divExpr.rhs());

        var mulExpr = (MulArithmeticExpr) divExpr.lhs();
        assertInstanceOf(AddArithmeticExpr.class, mulExpr.lhs());
        assertInstanceOf(SubArithmeticExpr.class, mulExpr.rhs());

        // Verify JSON structure is deeply nested
        JsonNode node = toTree(query);
        JsonNode complexNode = node.path("items").get(0).path("expr");
        assertEquals("div", complexNode.path("kind").asText());
        assertEquals("mul", complexNode.path("lhs").path("kind").asText());
        assertEquals("add", complexNode.path("lhs").path("lhs").path("kind").asText());
    }

    @Test
    @DisplayName("Integration: Multiple parameters of different types")
    void multipleParameterTypesQuery() throws Exception {
        var query = select(col("*"))
            .from(tbl("users"))
            .where(
                col("id").eq(ParamExpr.ordinal(1))
                    .and(col("username").eq(ParamExpr.named("username")))
                    .and(col("email").eq(ParamExpr.ordinal(2)))
                    .and(col("role").eq(ParamExpr.anonymous()))
                    .and(col("status").eq(ParamExpr.named("status")))
                    .and(col("created_at").gt(ParamExpr.ordinal(3)))
            )
            .build();

        var back = roundTrip(query);

        // Verify JSON contains all parameter types
        JsonNode node = toTree(back);
        String json = node.toString();
        assertTrue(json.contains("\"ordinal-param\""));
        assertTrue(json.contains("\"named-param\""));
        assertTrue(json.contains("\"anonymous-param\""));
        assertTrue(json.contains("\"index\":1"));
        assertTrue(json.contains("\"index\":2"));
        assertTrue(json.contains("\"index\":3"));
        assertTrue(json.contains("\"name\":\"username\""));
        assertTrue(json.contains("\"name\":\"status\""));
    }

    @Test
    @DisplayName("Integration: JSON format consistency check")
    void jsonFormatConsistency() throws Exception {
        var query = select(
            col("id"),
            col("price").add(col("tax")).as("total"),
            col("data").cast(type("jsonb")).as("json_data")
        )
            .from(tbl("items"))
            .distinct(DistinctSpec.TRUE)
            .where(
                col("name").like("%test%")
                    .and(col("count").gt(ParamExpr.named("minCount")))
            )
            .build();

        String json = mapper.writeValueAsString(query);

        // Verify JSON is valid and well-formatted
        assertNotNull(json);
        assertTrue(json.contains("\"kind\""));

        // Verify it can be parsed back
        SelectQuery back = mapper.readValue(json, SelectQuery.class);
        assertNotNull(back);

        // Verify round-trip produces identical JSON
        String json2 = mapper.writeValueAsString(back);
        assertEquals(json, json2);
    }
}



