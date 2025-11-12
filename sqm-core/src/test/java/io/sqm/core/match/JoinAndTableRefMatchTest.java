package io.sqm.core.match;

import io.sqm.core.*;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class JoinAndTableRefMatchTest {

    @Test
    void join_dispatches_to_genericJoin() {
        Join join = Join.join(tbl("a"));

        String out = JoinMatch
            .<String>match(join)
            .on(j -> "ON")
            .otherwise(j -> "OTHER");

        assertEquals("ON", out);
    }

    @Test
    void table_dispatches_across_variants() {
        Table table = tbl("t");
        String r1 = Match
            .<String>tableRef(table)
            .table(t -> "TABLE")
            .query(q -> "QT")
            .values(v -> "VT")
            .table(t -> "TABLE")
            .otherwise(x -> "OTHER");
        assertEquals("TABLE", r1);

        QueryTable qt = TableRef.query(select(lit(1)));
        String r2 = Match
            .<String>tableRef(qt)
            .values(v -> "VT")
            .table(t -> "TABLE")
            .query(q -> "QT")
            .values(v -> "VT")
            .otherwise(x -> "OTHER");
        assertEquals("QT", r2);

        ValuesTable vt = TableRef.values(rows(row(1, 2)));
        String r3 = Match
            .<String>tableRef(vt)
            .table(t -> "TABLE")
            .query(q -> "QT")
            .values(v -> "VT")
            .otherwise(x -> "OTHER");
        assertEquals("VT", r3);

        String otherwise = Match
            .<String>tableRef(vt)
            .table(t -> "TABLE")
            .query(q -> "QT")
            .otherwise(x -> "OTHER");
        assertEquals("OTHER", otherwise);
    }
}
