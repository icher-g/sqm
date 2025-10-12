package io.cherlabs.sqm.lang;

/**
 * A helper class for Tuple2 and Tuple3 creation.
 */
public class Tuple {
    private Tuple() {
    }

    public static  <T1, T2> Tuple2<T1, T2> of(T1 f, T2 s) {
        return new Tuple2<>(f, s);
    }

    public static  <T1, T2, T3> Tuple3<T1, T2, T3> of(T1 f, T2 s, T3 t) {
        return new Tuple3<>(f, s, t);
    }
}
