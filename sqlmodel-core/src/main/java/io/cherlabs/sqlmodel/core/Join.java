package io.cherlabs.sqlmodel.core;

/**
 * A base interface for all join implementations.
 */
public interface Join extends Entity {
    /**
     * Creates an {@link JoinType#Inner} join with the provided table.
     *
     * @param table a table to join with.
     * @return A newly created instance of a join.
     */
    static TableJoin inner(Table table) {
        return new TableJoin(JoinType.Inner, table, null);
    }

    /**
     * Creates a {@link JoinType#Left} join with the provided table.
     *
     * @param table a table to join with.
     * @return A newly created instance of a join.
     */
    static TableJoin left(Table table) {
        return new TableJoin(JoinType.Left, table, null);
    }

    /**
     * Creates a {@link JoinType#Right} join with the provided table.
     *
     * @param table a table to join with.
     * @return A newly created instance of a join.
     */
    static TableJoin right(Table table) {
        return new TableJoin(JoinType.Right, table, null);
    }

    /**
     * Creates a {@link JoinType#Full} join with the provided table.
     *
     * @param table a table to join with.
     * @return A newly created instance of a join.
     */
    static TableJoin full(Table table) {
        return new TableJoin(JoinType.Full, table, null);
    }

    /**
     * Creates a {@link JoinType#Cross} join with the provided table.
     *
     * @param table a table to join with.
     * @return A newly created instance of a join.
     */
    static TableJoin cross(Table table) {
        return new TableJoin(JoinType.Cross, table, null);
    }

    /**
     * Creates a join represented by string expr. This should be used only if there is no other implementations of the join that
     * meet the requirements.
     *
     * @param exp a string representation of the join.
     * @return A newly created instance of a join.
     */
    static ExpressionJoin expr(String exp) {
        return new ExpressionJoin(exp);
    }

    enum JoinType {
        Inner,
        Left,
        Right,
        Full,
        Cross
    }
}
