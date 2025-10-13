package io.cherlabs.sqm.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.cherlabs.sqm.core.*;
import io.cherlabs.sqm.core.FunctionColumn.Arg;
import io.cherlabs.sqm.json.mixins.*;

/**
 * Central place to build an ObjectMapper with all sqm MixIns & subtype aliases.
 */
public final class SqmMapperFactory {

    private SqmMapperFactory() {
    }

    /**
     * Create a fresh ObjectMapper with all mixins and sensible modules.
     */
    public static ObjectMapper createDefault() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mapper.addMixIn(Column.class, ColumnMixIn.class);
        mapper.addMixIn(Filter.class, FilterMixIn.class);
        mapper.addMixIn(Arg.class, FunctionColumnArgMixIn.class);
        mapper.addMixIn(Join.class, JoinMixIn.class);
        mapper.addMixIn(Table.class, TableMixIn.class);
        mapper.addMixIn(Values.class, ValuesMixIn.class);
        mapper.addMixIn(Query.class, QueryMixIn.class);
        mapper.addMixIn(Group.class, GroupMixIn.class);

        return mapper;
    }

    /**
     * Convenience: pretty-printed mapper for debugging or fixtures.
     */
    public static ObjectMapper createPretty() {
        return createDefault().enable(SerializationFeature.INDENT_OUTPUT);
    }
}
