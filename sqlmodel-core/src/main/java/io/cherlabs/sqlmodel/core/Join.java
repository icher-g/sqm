package io.cherlabs.sqlmodel.core;

public interface Join extends Entity {
    enum JoinType {
        Inner,
        Left,
        Right,
        Full,
        Cross
    }

    static TableJoin inner(Table table) {
        return new TableJoin(JoinType.Inner, table, null);
    }

    static TableJoin left(Table table) {
        return new TableJoin(JoinType.Left, table, null);
    }

    static TableJoin right(Table table) {
        return new TableJoin(JoinType.Right, table, null);
    }

    static TableJoin full(Table table) {
        return new TableJoin(JoinType.Full, table, null);
    }

    static TableJoin cross(Table table) {
        return new TableJoin(JoinType.Cross, table, null);
    }

    static ExpressionJoin expr(String exp) {
        return new ExpressionJoin(exp);
    }
}
