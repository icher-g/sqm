package io.sqm.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.sqm.core.Join;
import io.sqm.core.Table;
import org.junit.jupiter.api.Test;

import static io.sqm.core.Expression.funcArg;
import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MixInsGenerateJsonTest {

    private final ObjectMapper mapper = SqmJsonMixins.createPretty();

    @Test
    void functionColumn_withArgs_serializesWithTypeTags() throws Exception {
        var fc = func(
            "lower",
            funcArg(col("name", "u")), funcArg("X")
        ).as("l");

        String json = mapper.writeValueAsString(fc);

        // Should contain polymorphic Arg markers
        assertTrue(json.contains("\"kind\" : \"function\""));
        assertTrue(json.contains("\"arg_column\""));
        assertTrue(json.contains("\"arg_literal\""));
        assertTrue(json.contains("\"name\" : \"lower\""));
        assertTrue(json.contains("\"alias\" : \"l\""));

        System.out.println(json);
    }

    @Test
    void filter_binaryEq_serializesWithOpAndType() throws Exception {
        var predicate = col("u", "u.id").eq(col("o", "user_id"));

        String json = mapper.writeValueAsString(predicate);

        assertTrue(json.contains("\"kind\" : \"comparison\""));
        assertTrue(json.contains("\"operator\" : \"EQ\""));
        assertTrue(json.contains("u.id"));
        assertTrue(json.contains("user_id"));

        System.out.println(json);
    }

    @Test
    void join_innerJoin_serializesWithJoinType() throws Exception {
        var predicate = col("u", "u.id").eq(col("o", "user_id"));

        Join j = inner(tbl("orders").as("o"))
            .on(predicate);

        String json = mapper.writeValueAsString(j);

        assertTrue(json.contains("\"kind\" : \"INNER\""));
        assertTrue(json.contains("\"table\""));
        assertTrue(json.contains("\"on\""));

        System.out.println(json);
    }

    @Test
    void table_namedTable_serializesWithSchemaAlias() throws Exception {
        Table t = tbl("users").inSchema("public").as("u");

        String json = mapper.writeValueAsString(t);

        assertTrue(json.contains("\"kind\""));
        assertTrue(json.contains("\"name\" : \"users\""));
        assertTrue(json.contains("\"schema\" : \"public\""));
        assertTrue(json.contains("\"alias\" : \"u\""));

        System.out.println(json);
    }

    @Test
    void values_tuples_serializesWithRowList() throws Exception {
        var v = rows(row("x", 1), row("y", 2));

        String json = mapper.writeValueAsString(v);

        assertTrue(json.contains("\"kind\" : \"row_list\""));
        assertTrue(json.contains("\"value\" : \"x\""));
        assertTrue(json.contains("\"value\" : 1"));
        assertTrue(json.contains("\"value\" : \"y\""));
        assertTrue(json.contains("\"value\" : 2"));

        System.out.println(json);
    }

    @Test
    void query_select_serializes() throws Exception {
        var q = select(func("count", starArg()).as("cnt"))
            .from(tbl("orders").as("o"))
            .where(col("o", "status").in("A", "B"))
            .join(inner(tbl("users").as("u")).on(col("u", "id").eq(col("o", "user_id"))))
            .groupBy(group("u", "user_name"), group("o", "status"))
            .having(func("count", starArg()).gt(10));

        String json = mapper.writeValueAsString(q);

        assertTrue(json.contains("\"kind\" : \"select\""));
        assertTrue(json.contains("\"kind\" : \"function\""));
        assertTrue(json.contains("\"kind\" : \"INNER\""));
        assertTrue(json.contains("\"kind\" : \"comparison\""));
    }
}
