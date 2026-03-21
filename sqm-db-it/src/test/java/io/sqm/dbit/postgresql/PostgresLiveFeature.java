package io.sqm.dbit.postgresql;

enum PostgresLiveFeature {
    DISTINCT_ON,
    INNER_JOIN,
    AGGREGATE_FUNCTION,
    WHERE_PREDICATE,
    GROUP_BY,
    ORDER_BY
}
