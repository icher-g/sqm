package io.sqm.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.sqm.core.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MixInsGenerateJsonTest {

    private final ObjectMapper mapper = SqmMapperFactory.createPretty();

    @Test
    void functionColumn_withArgs_serializesWithTypeTags() throws Exception {
        FunctionColumn fc = new FunctionColumn(
            "lower",
            List.of(new FunctionColumn.Arg.Column("name", "u"), new FunctionColumn.Arg.Literal("X")),
            false,
            "l"
        );

        String json = mapper.writeValueAsString(fc);

        // Should contain polymorphic Arg markers
        assertTrue(json.contains("\"kind\" : \"func\""));
        assertTrue(json.contains("\"column\""));
        assertTrue(json.contains("\"literal\""));
        assertTrue(json.contains("\"name\" : \"lower\""));
        assertTrue(json.contains("\"alias\" : \"l\""));

        System.out.println(json);
    }

    @Test
    void filter_binaryEq_serializesWithOpAndType() throws Exception {
        Filter f = Filter
            .column(Column.of("u.id").from("u"))
            .eq(Column.of("user_id").from("o"));

        String json = mapper.writeValueAsString(f);

        assertTrue(json.contains("\"kind\" : \"column\""));
        assertTrue(json.contains("\"op\" : \"Eq\""));
        assertTrue(json.contains("u.id"));
        assertTrue(json.contains("user_id"));

        System.out.println(json);
    }

    @Test
    void join_innerJoin_serializesWithJoinType() throws Exception {
        Filter f = Filter
            .column(Column.of("u.id").from("u"))
            .eq(Column.of("user_id").from("o"));

        Join j = Join.inner(Table.of("orders").as("o"))
            .on(f);

        String json = mapper.writeValueAsString(j);

        assertTrue(json.contains("\"joinType\" : \"Inner\""));
        assertTrue(json.contains("\"table\""));
        assertTrue(json.contains("\"on\""));

        System.out.println(json);
    }

    @Test
    void table_namedTable_serializesWithSchemaAlias() throws Exception {
        Table t = Table.of("users").from("public").as("u");

        String json = mapper.writeValueAsString(t);

        assertTrue(json.contains("\"kind\""));
        assertTrue(json.contains("\"name\" : \"users\""));
        assertTrue(json.contains("\"schema\" : \"public\""));
        assertTrue(json.contains("\"alias\" : \"u\""));

        System.out.println(json);
    }

    @Test
    void values_tuples_serializesWithTypeTuples() throws Exception {
        Values v = Values.tuples(List.of(
            List.of("x", 1),
            List.of("y", 2)
        ));

        String json = mapper.writeValueAsString(v);

        assertTrue(json.contains("\"kind\" : \"tuples\""));
        assertTrue(json.contains("[ \"x\", 1 ]"));
        assertTrue(json.contains("[ \"y\", 2 ]"));

        System.out.println(json);
    }

    @Test
    void query_select_serializes() throws Exception {
        var q = query()
            .select(func("count", star()).as("cnt"))
            .from(tbl("orders").as("o"))
            .where(col("o", "status").in("A", "B"))
            .join(inner(tbl("users").as("u")).on(col("u", "id").eq(col("o", "user_id"))))
            .groupBy(group("u", "user_name"), group("o", "status"))
            .having(func("count", star()).gt(10));

        String json = mapper.writeValueAsString(q);

        assertTrue(json.contains("\"kind\" : \"select\""));
        assertTrue(json.contains("\"kind\" : \"func\""));
        assertTrue(json.contains("\"kind\" : \"table\""));
        assertTrue(json.contains("\"kind\" : \"column\""));
    }
}
