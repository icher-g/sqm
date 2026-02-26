package io.sqm.parser.core;

/**
 * Mutable look-ahead cursor used by lexer/parser helpers.
 */
public class Lookahead {

    private int pos;

    /**
     * Creates look-ahead state at a given position.
     *
     * @param pos initial token position
     */
    public Lookahead(int pos) {
        this.pos = pos;
    }

    /**
     * Creates look-ahead state at position {@code 0}.
     *
     * @return initial look-ahead state
     */
    public static Lookahead initial() {
        return new Lookahead(0);
    }

    /**
     * Creates look-ahead state at a specified position.
     *
     * @param pos initial token position
     * @return look-ahead state
     */
    public static Lookahead at(int pos) {
        return new Lookahead(pos);
    }

    /**
     * Returns current look-ahead position.
     *
     * @return current position
     */
    public int current() {
        return pos;
    }

    /**
     * Increments look-ahead position by one.
     */
    public void increment() {
        pos++;
    }

    /**
     * Increments look-ahead position by a specified amount.
     *
     * @param num increment amount
     */
    public void increment(int num) {
        pos += num;
    }

    /**
     * Decrements look-ahead position by one.
     */
    public void decrement() {
        pos--;
    }
}
