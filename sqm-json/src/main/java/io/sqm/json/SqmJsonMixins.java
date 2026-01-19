package io.sqm.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.sqm.core.*;
import io.sqm.json.mixins.*;

/**
 * Central place to build an ObjectMapper with all sqm MixIns & subtype aliases.
 */
public final class SqmJsonMixins {

    private SqmJsonMixins() {
    }

    /**
     * Configures JACKSON to support SQM JSON serialization.
     *
     * @param mapper a mapper.
     * @return configured mapper.
     */
    public static ObjectMapper configure(ObjectMapper mapper) {
        // Node
        mapper.addMixIn(Node.class, NodeMixin.class);

        // Expression family
        mapper.addMixIn(Expression.class, ExpressionMixin.class);
        mapper.addMixIn(FunctionExpr.Arg.class, FunctionArgMixin.class);
        mapper.addMixIn(ParamExpr.class, ParamExprMixin.class);
        mapper.addMixIn(ArithmeticExpr.class, ArithmeticExprMixin.class);

        // Predicate family
        mapper.addMixIn(Predicate.class, PredicateMixin.class);
        mapper.addMixIn(CompositePredicate.class, CompositePredicateMixin.class);

        // Value sets
        mapper.addMixIn(ValueSet.class, ValueSetMixin.class);

        // Select items
        mapper.addMixIn(SelectItem.class, SelectItemMixin.class);
        mapper.addMixIn(ExprSelectItem.class, SelectItemMixin.class);
        mapper.addMixIn(StarSelectItem.class, SelectItemMixin.class);
        mapper.addMixIn(QualifiedStarSelectItem.class, SelectItemMixin.class);

        // Queries
        mapper.addMixIn(Query.class, QueryMixin.class);
        mapper.addMixIn(WithQuery.class, QueryMixin.class);         // safe, already in subtypes
        mapper.addMixIn(CompositeQuery.class, QueryMixin.class);
        mapper.addMixIn(SelectQuery.class, QueryMixin.class);

        // Joins
        mapper.addMixIn(Join.class, JoinMixin.class);

        // Tables
        mapper.addMixIn(TableRef.class, TableRefMixin.class);

        // Grouping / ordering
        mapper.addMixIn(GroupItem.class, GroupItemMixin.class);

        // Window
        mapper.addMixIn(BoundSpec.class, BoundSpecMixin.class);
        mapper.addMixIn(FrameSpec.class, FrameSpecMixin.class);
        mapper.addMixIn(OverSpec.class, OverSpecMixin.class);

        return mapper;
    }

    /**
     * Create a fresh ObjectMapper with all mixins and sensible modules.
     */
    public static ObjectMapper createDefault() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return configure(mapper);
    }

    /**
     * Convenience: pretty-printed mapper for debugging or fixtures.
     */
    public static ObjectMapper createPretty() {
        return createDefault().enable(SerializationFeature.INDENT_OUTPUT);
    }
}
