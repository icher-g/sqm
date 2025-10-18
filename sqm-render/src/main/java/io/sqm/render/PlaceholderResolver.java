package io.sqm.render;

import io.sqm.render.spi.RenderContext;

/**
 * A helper class for providing a parameter placeholder according to the placeholder preferences provided by the dialect.
 */
public final class PlaceholderResolver {
    public static String next(RenderContext ctx) {
        var ph = ctx.dialect().placeholders();
        var pos = ctx.params().size() + 1;      // 1-based
        var pref = ctx.placeholderPreference();

        switch (pref) {
            case Ordinal -> {
                if (ph.supportsOrdinal()) return ph.ordinal(pos);
                // fallback
            }
            case Named -> {
                if (ph.supportsNamed()) return ph.named("p" + pos);
                // fallback
            }
            case Positional -> {
                return ph.marker(); // expect "?"
            }
            case Auto -> {
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
