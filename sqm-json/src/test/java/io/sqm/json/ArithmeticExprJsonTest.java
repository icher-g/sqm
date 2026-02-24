package io.sqm.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sqm.core.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests JSON serialization/deserialization for all arithmetic expression types.
 * Covers Add, Sub, Mul, Div, Mod, Negative, and Power arithmetic operations.
 */
public class ArithmeticExprJsonTest {

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
    @DisplayName("AddArithmeticExpr: serialize and deserialize addition operation")
    void addArithmeticExpr_roundTrip() throws Exception {
        // price + tax
        var expr = col("price").add(col("tax"));

        var back = roundTrip(expr, AddArithmeticExpr.class);

        assertNotNull(back);
        assertInstanceOf(ColumnExpr.class, back.lhs());
        assertInstanceOf(ColumnExpr.class, back.rhs());
        assertEquals("price", back.lhs().matchExpression().column(c -> c.name().value()).orElse(null));
        assertEquals("tax", back.rhs().matchExpression().column(c -> c.name().value()).orElse(null));

        // Check JSON contains "kind": "add"
        JsonNode node = toTree(expr);
        assertEquals("add", node.path("kind").asText());
    }

    @Test
    @DisplayName("SubArithmeticExpr: serialize and deserialize subtraction operation")
    void subArithmeticExpr_roundTrip() throws Exception {
        // total - discount
        var expr = col("total").sub(col("discount"));

        var back = roundTrip(expr, SubArithmeticExpr.class);

        assertNotNull(back);
        assertInstanceOf(ColumnExpr.class, back.lhs());
        assertInstanceOf(ColumnExpr.class, back.rhs());
        assertEquals("total", back.lhs().matchExpression().column(c -> c.name().value()).orElse(null));
        assertEquals("discount", back.rhs().matchExpression().column(c -> c.name().value()).orElse(null));

        JsonNode node = toTree(expr);
        assertEquals("sub", node.path("kind").asText());
    }

    @Test
    @DisplayName("MulArithmeticExpr: serialize and deserialize multiplication operation")
    void mulArithmeticExpr_roundTrip() throws Exception {
        // quantity * price
        var expr = col("quantity").mul(col("price"));

        var back = roundTrip(expr, MulArithmeticExpr.class);

        assertNotNull(back);
        assertInstanceOf(ColumnExpr.class, back.lhs());
        assertInstanceOf(ColumnExpr.class, back.rhs());
        assertEquals("quantity", back.lhs().matchExpression().column(c -> c.name().value()).orElse(null));
        assertEquals("price", back.rhs().matchExpression().column(c -> c.name().value()).orElse(null));

        JsonNode node = toTree(expr);
        assertEquals("mul", node.path("kind").asText());
    }

    @Test
    @DisplayName("DivArithmeticExpr: serialize and deserialize division operation")
    void divArithmeticExpr_roundTrip() throws Exception {
        // total / count
        var expr = col("total").div(col("count"));

        var back = roundTrip(expr, DivArithmeticExpr.class);

        assertNotNull(back);
        assertInstanceOf(ColumnExpr.class, back.lhs());
        assertInstanceOf(ColumnExpr.class, back.rhs());
        assertEquals("total", back.lhs().matchExpression().column(c -> c.name().value()).orElse(null));
        assertEquals("count", back.rhs().matchExpression().column(c -> c.name().value()).orElse(null));

        JsonNode node = toTree(expr);
        assertEquals("div", node.path("kind").asText());
    }

    @Test
    @DisplayName("ModArithmeticExpr: serialize and deserialize modulo operation")
    void modArithmeticExpr_roundTrip() throws Exception {
        // value % 10
        var expr = col("value").mod(lit(10));

        var back = roundTrip(expr, ModArithmeticExpr.class);

        assertNotNull(back);
        assertInstanceOf(ColumnExpr.class, back.lhs());
        assertInstanceOf(LiteralExpr.class, back.rhs());
        assertEquals("value", back.lhs().matchExpression().column(c -> c.name().value()).orElse(null));
        assertEquals(10, back.rhs().matchExpression().literal(l -> l.value()).orElse(null));

        JsonNode node = toTree(expr);
        assertEquals("mod", node.path("kind").asText());
    }

    @Test
    @DisplayName("NegativeArithmeticExpr: serialize and deserialize unary negation")
    void negativeArithmeticExpr_roundTrip() throws Exception {
        // -balance
        var expr = col("balance").neg();

        var back = roundTrip(expr, NegativeArithmeticExpr.class);

        assertNotNull(back);
        assertInstanceOf(ColumnExpr.class, back.expr());
        assertEquals("balance", back.expr().matchExpression().column(c -> c.name().value()).orElse(null));

        JsonNode node = toTree(expr);
        assertEquals("neg", node.path("kind").asText());
    }

    @Test
    @DisplayName("PowerArithmeticExpr: serialize and deserialize exponentiation")
    void powerArithmeticExpr_roundTrip() throws Exception {
        // base ^ exponent
        var expr = col("base").pow(lit(2));

        var back = roundTrip(expr, PowerArithmeticExpr.class);

        assertNotNull(back);
        assertInstanceOf(ColumnExpr.class, back.lhs());
        assertInstanceOf(LiteralExpr.class, back.rhs());
        assertEquals("base", back.lhs().matchExpression().column(c -> c.name().value()).orElse(null));
        assertEquals(2, back.rhs().matchExpression().literal(l -> l.value()).orElse(null));

        JsonNode node = toTree(expr);
        assertEquals("pow", node.path("kind").asText());
    }

    @Test
    @DisplayName("Arithmetic expression with literals: 5 + 3")
    void arithmetic_withLiterals() throws Exception {
        var expr = lit(5).add(lit(3));

        var back = roundTrip(expr, AddArithmeticExpr.class);

        assertEquals(5, back.lhs().matchExpression().literal(l -> l.value()).orElse(null));
        assertEquals(3, back.rhs().matchExpression().literal(l -> l.value()).orElse(null));
    }

    @Test
    @DisplayName("Complex nested arithmetic: (price + tax) * quantity")
    void arithmetic_nested() throws Exception {
        var expr = col("price").add(col("tax")).mul(col("quantity"));

        var back = roundTrip(expr, MulArithmeticExpr.class);

        assertNotNull(back);
        assertInstanceOf(AddArithmeticExpr.class, back.lhs());
        assertInstanceOf(ColumnExpr.class, back.rhs());

        var addExpr = (AddArithmeticExpr) back.lhs();
        assertEquals("price", addExpr.lhs().matchExpression().column(c -> c.name().value()).orElse(null));
        assertEquals("tax", addExpr.rhs().matchExpression().column(c -> c.name().value()).orElse(null));
        assertEquals("quantity", back.rhs().matchExpression().column(c -> c.name().value()).orElse(null));
    }

    @Test
    @DisplayName("Arithmetic in SELECT: SELECT price * quantity AS total")
    void arithmetic_inSelect() throws Exception {
        var query = select(
            col("price").mul(col("quantity")).as("total")
        ).from(tbl("orders")).build();

        var back = roundTrip(query, SelectQuery.class);

        var selectItem = (ExprSelectItem) back.items().getFirst();
        assertEquals("total", selectItem.alias().value());
        assertInstanceOf(MulArithmeticExpr.class, selectItem.expr());

        JsonNode node = toTree(query);
        assertEquals("mul", node.path("items").get(0).path("expr").path("kind").asText());
    }

    @Test
    @DisplayName("Arithmetic in WHERE: WHERE price - discount > 100")
    void arithmetic_inWhere() throws Exception {
        var query = select(col("id"))
            .from(tbl("products"))
            .where(col("price").sub(col("discount")).gt(lit(100)))
            .build();

        var back = roundTrip(query, SelectQuery.class);

        assertNotNull(back.where());
        var predicate = back.where().<ComparisonPredicate>matchPredicate().comparison(c -> c).orElse(null);
        assertNotNull(predicate);
        assertInstanceOf(SubArithmeticExpr.class, predicate.lhs());
    }

    @Test
    @DisplayName("Arithmetic with negation: -balance + adjustment")
    void arithmetic_withNegation() throws Exception {
        var expr = col("balance").neg().add(col("adjustment"));

        var back = roundTrip(expr, AddArithmeticExpr.class);

        assertInstanceOf(NegativeArithmeticExpr.class, back.lhs());
        assertInstanceOf(ColumnExpr.class, back.rhs());

        var negExpr = (NegativeArithmeticExpr) back.lhs();
        assertEquals("balance", negExpr.expr().matchExpression().column(c -> c.name().value()).orElse(null));
        assertEquals("adjustment", back.rhs().matchExpression().column(c -> c.name().value()).orElse(null));
    }

    @Test
    @DisplayName("Arithmetic expression as Expression type: polymorphic deserialization")
    void arithmetic_asExpression() throws Exception {
        var expr = col("a").add(col("b"));

        // Serialize as specific type, deserialize as Expression base type
        String json = mapper.writeValueAsString(expr);
        Expression back = mapper.readValue(json, Expression.class);

        assertInstanceOf(AddArithmeticExpr.class, back);
        var addExpr = (AddArithmeticExpr) back;
        assertEquals("a", addExpr.lhs().matchExpression().column(c -> c.name().value()).orElse(null));
        assertEquals("b", addExpr.rhs().matchExpression().column(c -> c.name().value()).orElse(null));
    }

    @Test
    @DisplayName("Multiple arithmetic operations: a + b - c * d / e % f")
    void arithmetic_multipleOperations() throws Exception {
        // Build: a + (b - ((c * d) / (e % f)))
        var expr = col("a").add(
            col("b").sub(
                col("c").mul(col("d")).div(
                    col("e").mod(col("f"))
                )
            )
        );

        var back = roundTrip(expr, AddArithmeticExpr.class);

        assertInstanceOf(AddArithmeticExpr.class, back);
        assertInstanceOf(ColumnExpr.class, back.lhs());
        assertInstanceOf(SubArithmeticExpr.class, back.rhs());

        // Validate JSON structure
        JsonNode node = toTree(expr);
        assertEquals("add", node.path("kind").asText());
        assertEquals("a", node.path("lhs").path("name").path("value").asText());
        assertEquals("sub", node.path("rhs").path("kind").asText());
    }

    @Test
    @DisplayName("Arithmetic in function argument: SUM(price * quantity)")
    void arithmetic_inFunctionArg() throws Exception {
        var query = select(
            func("sum", arg(col("price").mul(col("quantity")))).as("total_value")
        ).from(tbl("orders")).build();

        var back = roundTrip(query, SelectQuery.class);

        var selectItem = (ExprSelectItem) back.items().getFirst();
        var funcExpr = (FunctionExpr) selectItem.expr();
        assertEquals("sum", funcExpr.name().values().getLast());
        assertEquals("total_value", selectItem.alias().value());

        var arg0 = funcExpr.args().getFirst();
        var argExpr = arg0.matchArg().exprArg(a -> a.expr()).orElse(null);
        assertInstanceOf(MulArithmeticExpr.class, argExpr);
    }

    @Test
    @DisplayName("ArithmeticExpr JSON string format verification")
    void arithmetic_jsonStringFormat() throws Exception {
        var expr = col("price").add(lit(10));
        String json = mapper.writeValueAsString(expr);

        assertTrue(json.contains("\"kind\" : \"add\""));
        assertTrue(json.contains("\"lhs\""));
        assertTrue(json.contains("\"rhs\""));
        assertTrue(json.contains("\"price\""));
        assertTrue(json.contains("10"));
    }
}
