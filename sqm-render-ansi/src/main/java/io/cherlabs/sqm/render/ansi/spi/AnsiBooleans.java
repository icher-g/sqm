package io.cherlabs.sqm.render.ansi.spi;

import io.cherlabs.sqm.render.spi.Booleans;

public class AnsiBooleans implements Booleans {
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
