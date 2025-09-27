package io.cherlabs.sqlmodel.render.ansi.spi;

import io.cherlabs.sqlmodel.render.spi.Placeholders;

public class AnsiPlaceholders implements Placeholders {
    @Override
    public String marker() {
        return "?";
    }

    @Override
    public boolean supportsOrdinal() {
        return Placeholders.super.supportsOrdinal();
    }

    @Override
    public boolean supportsNamed() {
        return Placeholders.super.supportsNamed();
    }
}
