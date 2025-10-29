package io.sqm.parser.core;

/**
 * A helper class to return changed position to the caller.
 */
public class Lookahead {

    private int pos;

    public Lookahead(int pos) {
        this.pos = pos;
    }

    public static Lookahead initial() {
        return new Lookahead(0);
    }

    public static Lookahead at(int pos) {
        return new Lookahead(pos);
    }

    public int current() {
        return pos;
    }

    public void increment() {
        pos++;
    }

    public void increment(int num) {
        pos += num;
    }

    public void decrement() {
        pos--;
    }
}
