package io.sqm.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.sqm.core.Query;
import io.sqm.json.SqmJsonMixins;
import io.sqm.parser.ansi.AnsiSpecs;
import io.sqm.parser.spi.ParseContext;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;

public final class Utils {
    private static final ObjectMapper MAPPER = SqmJsonMixins.createDefault()
        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

    private Utils() {
    }

    public static String renderAnsi(Query q) {
        var r = RenderContext.of(new AnsiDialect());
        var s = r.render(q);
        return normalizeSql(s.sql());
    }

    public static Query parse(String sql) {
        var ctx = ParseContext.of(new AnsiSpecs());
        var pr = ctx.parse(Query.class, sql);
        if (pr.isError()) {
            throw new RuntimeException(pr.errorMessage());
        }
        return pr.value();
    }

    public static String canonicalJson(Query q) {
        try {
            // Use your mixins if required to get stable JSON
            return MAPPER.writeValueAsString(q);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String normalizeSql(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
