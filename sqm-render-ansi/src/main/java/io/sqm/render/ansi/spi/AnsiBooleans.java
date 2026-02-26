package io.sqm.render.ansi.spi;

import io.sqm.render.spi.Booleans;

/**
 * ANSI boolean literal and predicate behavior.
 */
public class AnsiBooleans implements Booleans {
    /**
     * Creates ANSI boolean behavior definition.
     */
    public AnsiBooleans() {
    }

    @Override
    public String trueLiteral() {
        return "TRUE";
    }

    @Override
    public String falseLiteral() {
        return "FALSE";
    }

    @Override
    public boolean requireExplicitPredicate() {
        // ANSI allows WHERE <boolean_column>. No need for "= TRUE".
        return false;
    }
}
