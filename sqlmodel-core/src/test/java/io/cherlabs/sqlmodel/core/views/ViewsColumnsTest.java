package io.cherlabs.sqlmodel.core.views;

import io.cherlabs.sqlmodel.core.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ViewsColumnsTest {

    @Test
    void named_column_fields_are_exposed() {
        NamedColumn c = new NamedColumn("id", "pid", "p");
        assertEquals(Optional.of("id"), Columns.name(c));
        assertEquals(Optional.of("pid"), Columns.alias(c));
        assertEquals(Optional.of("p"), Columns.table(c));
        assertTrue(Columns.expr(c).isEmpty());
        assertTrue(Columns.query(c).isEmpty());
    }

    @Test
    void expr_column_fields_are_exposed() {
        ExpressionColumn c = new ExpressionColumn("lower(name)", "lname");
        assertEquals(Optional.empty(), Columns.name(c));
        assertEquals(Optional.of("lname"), Columns.alias(c));
        assertTrue(Columns.table(c).isEmpty());
        assertEquals(Optional.of("lower(name)"), Columns.expr(c));
        assertTrue(Columns.query(c).isEmpty());
    }

    @Test
    void query_column_fields_are_exposed() {
        Query q = new Query();
        QueryColumn c = new QueryColumn(q, "q");
        assertEquals(Optional.empty(), Columns.name(c));
        assertEquals(Optional.of("q"), Columns.alias(c));
        assertTrue(Columns.table(c).isEmpty());
        assertTrue(Columns.expr(c).isEmpty());
        assertEquals(Optional.of(q), Columns.query(c));
    }

    @Test
    void func_column_fields_are_exposed() {
        FunctionColumn c = new FunctionColumn("COUNT", List.of(new FunctionColumn.Arg.Column("t", "id")), true, "c");
        assertEquals(Optional.of("COUNT"), Columns.name(c));
        assertEquals(Optional.of("c"), Columns.alias(c));
        assertTrue(Columns.table(c).isEmpty());
        assertTrue(Columns.expr(c).isEmpty());
        var args = Columns.functionArgs(c);
        assertTrue(args.isPresent());
        var arg = (FunctionColumn.Arg.Column)args.get().get(0);
        assertEquals("t", arg.table());
        assertEquals("id", arg.name());
    }
}