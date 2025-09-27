package io.cherlabs.sqlmodel.render.spi;

public interface Booleans {
    String trueLiteral();
    String falseLiteral();
    boolean requireExplicitPredicate();
}
