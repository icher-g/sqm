package io.sqm.render.mysql.spi;

import io.sqm.render.defaults.DefaultOperators;

/**
 * MySQL operator tokens.
 */
public class MySqlOperators extends DefaultOperators {

    /**
     * Creates MySQL operator definitions.
     */
    public MySqlOperators() {
    }

    /**
     * Returns MySQL null-safe equality operator token.
     *
     * @return {@code <=>}
     */
    @Override
    public String nullSafeEq() {
        return "<=>";
    }
}


