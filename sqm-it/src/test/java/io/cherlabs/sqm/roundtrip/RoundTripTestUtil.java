package io.cherlabs.sqm.roundtrip;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.cherlabs.sqm.core.Query;
import io.cherlabs.sqm.json.SqmMapperFactory;
import io.cherlabs.sqm.parser.Parsers;
import io.cherlabs.sqm.render.ansi.Renderers;

public final class RoundTripTestUtil {
    private static final ObjectMapper MAPPER = SqmMapperFactory.createDefault()
        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

    private RoundTripTestUtil() {
    }

    public static String renderAnsi(Query q) {
        var r = Renderers.render(q);
        return normalizeSql(r.sql());
    }

    public static Query parse(String sql) {
        var pr = Parsers.defaultRepository().require(Query.class).parse(sql);
        if (!pr.ok()) {
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
