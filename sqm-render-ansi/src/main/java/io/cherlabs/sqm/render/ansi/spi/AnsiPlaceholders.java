package io.cherlabs.sqm.render.ansi.spi;

import io.cherlabs.sqm.render.spi.Placeholders;

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
