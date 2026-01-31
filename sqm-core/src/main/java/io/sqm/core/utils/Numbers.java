package io.sqm.core.utils;

/**
 * A helper class for numbers manipulations.
 */
public class Numbers {

    private Numbers(){}

    /**
     * Checks if the provided string is a positive integer.
     * @param s a string to check.
     * @return {@code true} if the string is a positive number and {@link false} otherwise.
     */
    public static boolean isPositiveInteger(String s) {
        for (int i = 0, n = s.length(); i < n; i++) {
            char ch = s.charAt(i);
            if (ch < '0' || ch > '9') return false;
        }
        return !s.isEmpty();
    }
}
