package io.sqm.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.sqm.core.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * These tests validate:
 * - All MixIns can be registered together (no conflicts)
 * - FunctionColumn args use polymorphic typing (NAME) and round-trip
 * - We can at least serialize/deserialize minimal instances for other roots
 * <p>
 * Adjust concrete JSON payloads to match your Arg/Column/Filter shapes if needed.
 */
public class MixInsSmokeTest {

    @Test
    @DisplayName("Factory builds an ObjectMapper with all MixIns attached")
    void buildsMapper() {
        ObjectMapper m = SqmJsonMixins.createDefault();
        assertNotNull(m);
    }

    @Test
    @DisplayName("FunctionColumn args: polymorphic deserialization by NAME (expr.g., 'column', 'literal')")
    void functionArgs_polymorphic_roundTrip() throws Exception {
        ObjectMapper m = SqmJsonMixins.createPretty();

        // This JSON assumes your FunctionColumnArgMixIn uses @JsonTypeInfo(use = NAME, property = "type")
        // and @JsonSubTypes names like: column, literal, star, expr (modify if yours differ).
        String json =
            """
                {
                  "kind" : "expr",
                  "expr" : {
                    "kind" : "function",
                    "name" : "lower",
                    "args" : [ {
                      "kind" : "arg_expr",
                      "expr" : {
                        "kind" : "column",
                        "tableAlias" : "t",
                        "name" : "name"
                      }
                    }, {
                      "kind" : "arg_expr",
                      "expr" : {
                        "kind" : "literal",
                        "value" : "X"
                      }
                    } ],
                    "distinctArg" : false
                  },
                  "alias" : "l"
                }""";

        // 1) Can deserialize into FunctionExpr
        var esi = m.readValue(json, ExprSelectItem.class);
        var fe = esi.expr().<FunctionExpr>matchExpression().func(f -> f).orElse(null);

        assertEquals("lower", fe.name());
        assertEquals("l", esi.alias());
        assertNotNull(fe.args());
        assertEquals(2, fe.args().size());

        // 2) The Arg items are concrete subtypes; assert by class simple names to avoid package coupling
        var a0 = fe.args().get(0);
        var a1 = fe.args().get(1);
        assertInstanceOf(FunctionExpr.Arg.ExprArg.class, a0);
        assertInstanceOf(FunctionExpr.Arg.ExprArg.class, a1);
        assertInstanceOf(ColumnExpr.class, a0.matchExpression()
            .funcArg(f -> f.matchArg()
                .exprArg(a -> a.expr())
                .orElse(null)
            )
            .orElse(null)
        );
        assertInstanceOf(LiteralExpr.class, a1.matchExpression()
            .funcArg(f -> f.matchArg()
                .exprArg(a -> a.expr())
                .orElse(null)
            )
            .orElse(null)
        );

        // 3) Round-trip back to JSON and ensure type tags are preserved
        String back = m.writeValueAsString(esi);
        assertEquals(json.stripIndent(), back.stripIndent());
    }

    @Test
    @DisplayName("Column MixIn: serialize a simple concrete Column")
    void column_serialization_minimal() throws Exception {
        ObjectMapper m = SqmJsonMixins.createDefault();

        // Minimal JSON for a Named/Reference column (adapt the shape to your ColumnMixIn)
        String json =
            """
                {
                "kind" : "expr",
                  "expr" : {
                    "kind": "column",
                    "name": "u.name"
                  },
                  "alias": "n"
                }
                """;

        var esi = m.readValue(json, ExprSelectItem.class);
        var c = esi.expr().<ColumnExpr>matchExpression().column(col -> col).orElse(null);

        assertNotNull(c);
        String out = m.writeValueAsString(c);
        // We don’t over-assert structure; just ensure it writes back
        assertTrue(out.contains("name"));
    }

    @Test
    @DisplayName("Predicate MixIn: minimal polymorphic read/write")
    void predicate_serialization_minimal() throws Exception {
        ObjectMapper m = SqmJsonMixins.createPretty();

        // Minimal JSON for a binary filter column = literal (adjust to your FilterMixIn schema)
        String json =
            """
                {
                  "kind" : "comparison",
                  "lhs" : {
                    "kind" : "column",
                    "tableAlias" : "u",
                    "name" : "id"
                  },
                  "operator" : "EQ",
                  "rhs" : {
                    "kind" : "literal",
                    "value" : 123
                  }
                }""";

        var f = m.readValue(json, Predicate.class);

        assertNotNull(f);
        var p = f.<ComparisonPredicate>matchPredicate().comparison(cmp -> cmp).orElse(null);
        assertInstanceOf(ComparisonPredicate.class, f);
        assertInstanceOf(ColumnExpr.class, p.lhs());
        assertInstanceOf(LiteralExpr.class, p.rhs());

        String out = m.writeValueAsString(f);
        assertEquals(json.stripIndent(), out.stripIndent());
    }

    @Test
    @DisplayName("Join MixIn: minimal read/write")
    void join_serialization_minimal() throws Exception {
        ObjectMapper m = SqmJsonMixins.createPretty();

        // Minimal JSON for a table join (adjust keys to your JoinMixIn)
        String json =
            """
                {
                  "kind" : "on",
                  "right" : {
                    "kind": "table",
                    "name": "users",
                    "alias": "u"
                  },
                  "kind" : "INNER",
                  "on" : {
                    "kind": "comparison",
                    "lhs":  {
                      "kind" : "column",
                        "tableAlias" : "u",
                        "name" : "id"
                    },
                    "operator": "EQ",
                    "rhs": {
                        "kind" : "column",
                        "tableAlias" : "o",
                        "name" : "user_id"
                    }
                  }
                }
                """;

        var j = m.readValue(json, Join.class);

        assertNotNull(j);
        assertInstanceOf(OnJoin.class, j);
        assertInstanceOf(Table.class, j.right());
        var p = j.<ComparisonPredicate>matchJoin()
            .on(o -> o.on().<ComparisonPredicate>matchPredicate()
                .comparison(cmp -> cmp)
                .orElse(null)
            )
            .orElse(null);
        assertInstanceOf(ComparisonPredicate.class, p);
        assertEquals("users", j.right().matchTableRef().table(t -> t.name()).orElse(null));
        assertEquals("u", j.right().alias());
        assertEquals("id", p.lhs().matchExpression().column(c -> c.name()).orElse(null));
        assertEquals("user_id", p.rhs().matchExpression().column(c -> c.name()).orElse(null));
    }

    @Test
    @DisplayName("Table MixIn: minimal read/write for NamedTable")
    void table_serialization_minimal() throws Exception {
        ObjectMapper m = SqmJsonMixins.createDefault();

        String json =
            """
                {
                  "kind": "table",
                  "name": "users",
                  "schema": "public",
                  "alias": "u"
                }
                """;

        var t = m.readValue(json, Table.class);

        assertNotNull(t);
        assertTrue(m.writeValueAsString(t).contains("\"name\":\"users\""));
    }

    @Test
    @DisplayName("ValueSet MixIn: minimal read/write for tuples/list")
    void value_set_serialization_minimal() throws Exception {
        ObjectMapper m = SqmJsonMixins.createPretty();

        String json =
            """
                {
                  "kind" : "row_list",
                  "rows" : [ {
                    "kind" : "row",
                    "items" : [ {
                      "kind" : "literal",
                      "value" : "x"
                    }, {
                      "kind" : "literal",
                      "value" : 1
                    } ]
                  }, {
                    "kind" : "row",
                    "items" : [ {
                      "kind" : "literal",
                      "value" : "y"
                    }, {
                      "kind" : "literal",
                      "value" : 2
                    } ]
                  } ]
                }""";

        var v = m.readValue(json, RowListExpr.class);

        assertNotNull(v);
        assertInstanceOf(RowListExpr.class, v);
        assertEquals(2, v.rows().size());

        String out = m.writeValueAsString(v);
        assertEquals(json.stripIndent(), out.stripIndent());
    }
}
