package io.cherlabs.sqlmodel.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cherlabs.sqlmodel.core.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MixInsGenerateJsonTest {

    private final ObjectMapper mapper = SqlModelMapperFactory.createPretty();

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
}
