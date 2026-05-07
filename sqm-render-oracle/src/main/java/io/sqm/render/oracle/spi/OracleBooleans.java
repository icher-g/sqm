package io.sqm.render.oracle.spi;

import io.sqm.render.spi.Booleans;

/**
 * Oracle SQL boolean literal and predicate behavior for the baseline dialect slice.
 */
public class OracleBooleans implements Booleans {
    /**
     * Creates Oracle boolean behavior definition.
     */
    public OracleBooleans() {
    }

    @Override
    public String trueLiteral() {
        return "1";
    }

    @Override
    public String falseLiteral() {
        return "0";
    }

    @Override
    public boolean requireExplicitPredicate() {
        return true;
    }
}
