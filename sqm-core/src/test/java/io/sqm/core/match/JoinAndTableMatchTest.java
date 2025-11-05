package io.sqm.core.match;

import io.sqm.core.Join;
import io.sqm.core.QueryTable;
import io.sqm.core.Table;
import io.sqm.core.TableRef;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class JoinAndTableMatchTest {

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
        String r1 = TableMatch
                        .<String>match(table)
                        .table(t -> "TABLE")
                        .query(q -> "QT")
                        .values(v -> "VT")
                        .otherwise(x -> "OTHER");
        assertEquals("TABLE", r1);

        QueryTable qt = TableRef.query(select(sel(lit(1))));
        String r2 = TableMatch
                        .<String>match(qt)
                        .table(t -> "TABLE")
                        .query(q -> "QT")
                        .values(v -> "VT")
                        .otherwise(x -> "OTHER");
        assertEquals("QT", r2);
    }
}
