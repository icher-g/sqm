package io.sqm.render.spi;

/**
 * An interface to customize value formatting per dialect.
 */
public interface ValueFormatter {
    /**
     * Formats a value according to the dialect rules.
     *
     * @param value a value.
     * @return a formatted value.
     */
    String format(Object value);
}
