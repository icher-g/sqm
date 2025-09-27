package io.cherlabs.sqlmodel.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JoinFactoryAndTableJoinTest {

    @Test
    void join_factories_create_tablejoin_with_type() {
        NamedTable t = new NamedTable("products", "p", null);

        TableJoin j1 = Join.inner(t);
        assertEquals(Join.JoinType.Inner, j1.joinType());
        assertSame(t, j1.table());
        assertNull(j1.on());

        TableJoin j2 = Join.left(t);
        assertEquals(Join.JoinType.Left, j2.joinType());

        TableJoin j3 = Join.right(t);
        assertEquals(Join.JoinType.Right, j3.joinType());

        TableJoin j4 = Join.full(t);
        assertEquals(Join.JoinType.Full, j4.joinType());

        TableJoin j5 = Join.cross(t);
        assertEquals(Join.JoinType.Cross, j5.joinType());
    }

    @Test
    void tablejoin_on_overloads_build_filters() {
        NamedTable t = new NamedTable("dep", "d", null);
        TableJoin j = Join.inner(t);

        NamedColumn lc = new NamedColumn("id", null, "p");
        NamedColumn rc = new NamedColumn("id", null, "d");

        TableJoin withJoinFilter = j.on(lc, JoinFilter.Operator.Gte, rc);
        assertInstanceOf(JoinFilter.class, withJoinFilter.on());
        JoinFilter jf = (JoinFilter) withJoinFilter.on();
        assertEquals(JoinFilter.Operator.Gte, jf.operator());
        assertSame(lc, jf.left());
        assertSame(rc, jf.right());

        ColumnFilter cf = new ColumnFilter(new NamedColumn("active", null, "d"), ColumnFilter.Operator.Eq, Values.single(true));
        TableJoin withColumnFilter = j.on(cf);
        assertSame(cf, withColumnFilter.on());
    }
}