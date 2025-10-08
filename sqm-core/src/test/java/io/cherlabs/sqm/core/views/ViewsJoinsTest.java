package io.cherlabs.sqm.core.views;

import io.cherlabs.sqm.core.*;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ViewsJoinsTest {

    @Test
    void table_join_fields_are_exposed() {
        NamedTable t = new NamedTable("products", "p", null);
        TableJoin j = Join.left(t).on(new NamedColumn("id", null, "p").eq(new NamedColumn("id", null, "d")));

        assertEquals(Optional.of(Join.JoinType.Left), Joins.joinType(j));
        assertEquals(Optional.of(t), Joins.table(j));
        assertTrue(Joins.on(j).isPresent());
    }

    @Test
    void expr_join_fields_are_exposed() {
        ExpressionJoin j = Join.expr("some_expression");
        assertTrue(Joins.table(j).isEmpty());
        assertTrue(Joins.on(j).isEmpty());
        assertEquals(Optional.of("some_expression"), Joins.expr(j));
    }
}