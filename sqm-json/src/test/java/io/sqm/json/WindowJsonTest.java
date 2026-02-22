package io.sqm.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sqm.core.*;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

public class WindowJsonTest {

    private final ObjectMapper mapper = SqmJsonMixins.createPretty();

    private <T> T roundTrip(T value, Class<T> type) throws Exception {
        String json = mapper.writeValueAsString(value);
        T back = mapper.readValue(json, type);
        // sanity: equality (records usually implement structural equals)
        assertEquals(value, back, "round-trip equality failed");
        return back;
    }

    private JsonNode toTree(Object value) throws Exception {
        return mapper.readTree(mapper.writeValueAsBytes(value));
    }

    /* ---------------- Tests ---------------- */

    @Test
    void windowDef_and_over_ref_are_serialized() throws Exception {
        // SELECT emp_name, RANK() OVER w AS r
        // FROM employees
        // WINDOW w AS (PARTITION BY dept ORDER BY salary DESC)
        var q = select(
            col("emp_name"),
            func("rank").over("w").as("r")
        )
            .from(tbl("employees"))
            .window(
                window("w", partition(col("dept")), orderBy(order(col("salary")).desc()))
            )
            .build();

        // Round-trip the whole SelectQuery
        var back = roundTrip(q, SelectQuery.class);

        // Inspect JSON shape a bit
        JsonNode root = toTree(q);
        // windows[0].name == "w"
        assertEquals("w", root.path("windows").get(0).path("name").asText());
        // selectItems[1].expr.over.kind is Ref (depending on your type hint; adapt "kind" name if different)
        String overNodeText = root.path("items").get(1).path("expr").path("over").toString();
        assertTrue(overNodeText.contains("Ref"), "Expected OverSpec.Ref in JSON: " + overNodeText);

        // Ensure the reference survived
        var si = (ExprSelectItem) back.items().get(1);
        var fn = (FunctionExpr) si.expr();
        assertNotNull(fn.over());
        assertInstanceOf(OverSpec.Ref.class, fn.over());
        assertEquals("w", ((OverSpec.Ref) fn.over()).windowName());
    }

    @Test
    void over_inline_with_rows_single_bound() throws Exception {
        // SUM(amount) OVER (PARTITION BY acct_id ORDER BY ts ROWS 5 PRECEDING)
        var fx = func("sum", arg(col("amount")))
            .over(
                partition(col("acct_id")),
                orderBy(order(col("ts")).asc()),
                rows(preceding(5))
            );

        var back = roundTrip(fx, FunctionExpr.class);

        JsonNode node = toTree(fx).path("over");
        // Expect Def + FrameSpec.Single with Unit.ROWS and BoundSpec.Preceding
        assertTrue(node.toString().contains("Def"));
        assertTrue(node.toString().contains("single"));
        assertTrue(node.toString().contains("ROWS"));
        assertTrue(node.toString().contains("preceding"));

        var over = back.over();
        assertInstanceOf(OverSpec.Def.class, over);
        var def = (OverSpec.Def) over;
        assertNotNull(def.partitionBy());
        assertNotNull(def.orderBy());
        assertNotNull(def.frame());
        assertNull(def.exclude());
        assertFalse(def.partitionBy().items().isEmpty());
    }

    @Test
    void over_inline_groups_between_with_exclude_ties_json() throws Exception {
        // RANK() OVER (PARTITION BY grp ORDER BY score DESC
        //              GROUPS BETWEEN 1 PRECEDING AND 1 FOLLOWING EXCLUDE TIES)
        var fx = func("rank")
            .over(
                partition(col("grp")),
                orderBy(order(col("score")).desc()),
                groups(preceding(1), following(1)),
                excludeTies()
            );

        var back = roundTrip(fx, FunctionExpr.class);

        JsonNode over = toTree(fx).path("over");
        assertTrue(over.toString().contains("between"));
        assertTrue(over.toString().contains("GROUPS"));
        assertTrue(over.toString().contains("preceding"));
        assertTrue(over.toString().contains("following"));
        assertTrue(over.toString().contains("TIES")); // exclude

        var def = (OverSpec.Def) back.over();
        assertNotNull(def.frame());
        assertEquals(OverSpec.Exclude.TIES, def.exclude());
    }

    @Test
    void extend_base_window_with_between_frame() throws Exception {
        // WINDOW w AS (PARTITION BY dept ORDER BY salary DESC)
        // SUM(salary) OVER (w ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW)
        var q = select(
            col("dept"),
            col("emp_name"),
            func("sum", arg(col("salary")))
                .over(over("w", rows(unboundedPreceding(), currentRow()))
            ).as("run_sum")
        )
            .from(tbl("employees"))
            .window(
                window("w", partition(col("dept")), orderBy(order(col("salary")).desc()))
            )
            .build();

        var back = roundTrip(q, SelectQuery.class);

        // JSON checks
        JsonNode root = toTree(q);
        assertEquals("w", root.path("windows").get(0).path("name").asText());
        String overJson = root.path("items").get(2).path("expr").path("over").toString();
        assertTrue(overJson.contains("Def"));
        assertTrue(overJson.contains("between"));
        assertTrue(overJson.contains("unbounded"));
        assertTrue(overJson.contains("currentRow") || overJson.contains("CURRENT")); // depending on mixin naming

        // AST checks
        var si = (ExprSelectItem) back.items().get(2);
        var fn = (FunctionExpr) si.expr();
        var def = (OverSpec.Def) fn.over();
        assertEquals("w", def.baseWindow());
        var between = (FrameSpec.Between) def.frame();
        assertInstanceOf(BoundSpec.UnboundedPreceding.class, between.start());
        assertInstanceOf(BoundSpec.CurrentRow.class, between.end());
    }

    @Test
    void aggregate_with_filter_and_over_partition_json() throws Exception {
        // COUNT(DISTINCT user_id) FILTER (WHERE active) OVER (PARTITION BY dept)
        var fx = func("count", arg(col("user_id"))).distinct()
                                                   .filter(col("active").eq(lit(true)))
                                                   .over(over(partition(col("dept"))));

        var back = roundTrip(fx, FunctionExpr.class);

        JsonNode n = toTree(fx);
        // distinct + filter + over present
        assertTrue(n.toString().contains("distinct"));
        assertTrue(n.toString().contains("filter"));
        assertTrue(n.toString().contains("over"));

        assertTrue(back.distinctArg());
        assertNotNull(back.filter());
        assertNotNull(back.over());
        assertInstanceOf(OverSpec.Def.class, back.over());
        assertNotNull(((OverSpec.Def) back.over()).partitionBy());
    }

    /* -------- Optional: window list JSON only (no SELECT text) -------- */

    @Test
    void windowDef_list_serialization_is_stable() throws Exception {
        var defs = java.util.List.of(
            window("w1", partition(col("k")), orderBy(order(col("ts")).asc())),
            window("w2", partition(), orderBy(order(col("v")).desc()))
        );

        String json = mapper.writeValueAsString(defs);
        JsonNode arr = mapper.readTree(json);

        assertEquals(2, arr.size());
        assertEquals("w1", arr.get(0).path("name").asText());
        assertTrue(arr.get(0).path("spec").toString().contains("Def"));
        assertEquals("w2", arr.get(1).path("name").asText());
    }
}
