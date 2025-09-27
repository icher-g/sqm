package io.cherlabs.sqlmodel.render;

import io.cherlabs.sqlmodel.render.spi.RenderContext;

public final class PlaceholderResolver {
    public static String next(RenderContext ctx) {
        var ph = ctx.dialect().placeholders();
        var pos = ctx.params().size() + 1;      // 1-based
        var pref = ctx.placeholderPreference();

        switch (pref) {
            case ORDINAL -> {
                if (ph.supportsOrdinal()) return ph.ordinal(pos);
                // fallback
            }
            case NAMED -> {
                if (ph.supportsNamed()) return ph.named("p" + pos);
                // fallback
            }
            case POSITIONAL -> {
                return ph.marker(); // expect "?"
            }
            case AUTO -> {
                if (ph.supportsOrdinal()) return ph.ordinal(pos);
                if (ph.supportsNamed()) return ph.named("p" + pos);
                return ph.marker();
            }
        }
        // Fallbacks for unsupported preference:
        if (ph.supportsOrdinal()) return ph.ordinal(pos);
        if (ph.supportsNamed()) return ph.named("p" + pos);
        return ph.marker(); // ultimate fallback
    }
}
