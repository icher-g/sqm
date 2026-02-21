package io.sqm.validate.postgresql;

import io.sqm.core.CteDef;
import io.sqm.core.Query;
import io.sqm.core.RegexPredicate;
import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.catalog.model.CatalogType;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.SchemaQueryValidator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PostgresValidationDialectVersionMatrixTest {
    private static final CatalogSchema SCHEMA = CatalogSchema.of(
        CatalogTable.of("public", "users",
            CatalogColumn.of("id", CatalogType.LONG),
            CatalogColumn.of("name", CatalogType.STRING)
        )
    );

    @ParameterizedTest(name = "{0}")
    @MethodSource("featureCases")
    void validate_respectsVersionedFeatures(
        String name,
        SqlDialectVersion version,
        Query query,
        boolean expectedUnsupported
    ) {
        var validator = SchemaQueryValidator.of(SCHEMA, PostgresValidationDialect.of(version));
        var result = validator.validate(query);

        var unsupportedCount = result.problems().stream()
            .filter(p -> p.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED)
            .count();
        assertEquals(expectedUnsupported ? 1L : 0L, unsupportedCount);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nestedCases")
    void validate_reportsUnsupportedFeatureOnlyOnceForNestedQueries(
        String name,
        Query query
    ) {
        var validator = SchemaQueryValidator.of(SCHEMA, PostgresValidationDialect.of(SqlDialectVersion.of(9, 0)));
        var result = validator.validate(query);

        var unsupportedCount = result.problems().stream()
            .filter(p -> p.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED)
            .count();
        assertEquals(1L, unsupportedCount);
    }

    private static Stream<Arguments> featureCases() {
        var lateral = select(star()).from(tbl("users").lateral());
        var withOrdinality = select(star())
            .from(tbl(func("unnest", arg(array(lit(1L))))).as("u").withOrdinality());
        var groupingSetsQuery = select(col("u", "id"))
            .from(tbl("users").as("u"))
            .groupBy(groupingSets(group("u", "id")));
        var cteMaterialized = with(
            cte(
                "u",
                select(col("id")).from(tbl("users")),
                List.of("id"),
                CteDef.Materialization.MATERIALIZED
            )
        ).body(select(col("u", "id")).from(tbl("u").as("u")));
        var keyShareLock = select(star())
            .from(tbl("users").as("u"))
            .lockFor(keyShare(), List.of(), false, false);
        var skipLocked = select(star())
            .from(tbl("users").as("u"))
            .lockFor(update(), List.of(), false, true);
        var customOperator = select(col("u", "id").op("~>", lit(1L)))
            .from(tbl("users").as("u"));
        var postgresTypecast = select(lit("1").cast(type("int")))
            .from(tbl("users").as("u"));
        var groupsFrame = select(
            func("sum", arg(col("u", "id")))
                .over(over(orderBy(order(col("u", "id"))), groups(unboundedPreceding(), currentRow())))
        ).from(tbl("users").as("u"));
        var excludeFrame = select(
            func("sum", arg(col("u", "id")))
                .over(over(orderBy(order(col("u", "id"))), rows(unboundedPreceding(), currentRow()), excludeTies()))
        ).from(tbl("users").as("u"));
        var ilike = select(star())
            .from(tbl("users").as("u"))
            .where(col("u", "name").ilike("%a%"));
        var similarTo = select(star())
            .from(tbl("users").as("u"))
            .where(col("u", "name").similarTo("%(a|b)%"));
        var regex = select(star())
            .from(tbl("users").as("u"))
            .where(RegexPredicate.of(col("u", "name"), lit("^a"), false));
        var tableOnly = select(star()).from(tbl("users").only().as("u"));
        var tableDescendants = select(star()).from(tbl("users").includingDescendants().as("u"));
        var arrayLiteral = select(array(lit(1L), lit(2L))).from(tbl("users").as("u"));
        var arraySubscript = select(col("u", "name").at(1)).from(tbl("users").as("u"));
        var arraySlice = select(col("u", "name").slice(1, 2)).from(tbl("users").as("u"));
        var collateExpr = select(col("u", "name").collate("C")).from(tbl("users").as("u"));
        var atTimeZoneExpr = select(col("u", "name").atTimeZone(lit("UTC"))).from(tbl("users").as("u"));
        var powerExpr = select(col("u", "id").pow(2)).from(tbl("users").as("u"));
        var isDistinctFromPredicate = select(star())
            .from(tbl("users").as("u"))
            .where(col("u", "name").isDistinctFrom(lit("x")));
        var rollupQuery = select(col("u", "id"))
            .from(tbl("users").as("u"))
            .groupBy(rollup(group("u", "id")));
        var cubeQuery = select(col("u", "id"))
            .from(tbl("users").as("u"))
            .groupBy(cube(group("u", "id")));

        return Stream.of(
            Arguments.of("ilike-unsupported-pg84", SqlDialectVersion.of(8, 4), ilike, true),
            Arguments.of("ilike-supported-pg90", SqlDialectVersion.of(9, 0), ilike, false),
            Arguments.of("similar-to-unsupported-pg84", SqlDialectVersion.of(8, 4), similarTo, true),
            Arguments.of("similar-to-supported-pg90", SqlDialectVersion.of(9, 0), similarTo, false),
            Arguments.of("regex-unsupported-pg84", SqlDialectVersion.of(8, 4), regex, true),
            Arguments.of("regex-supported-pg90", SqlDialectVersion.of(9, 0), regex, false),
            Arguments.of("table-only-unsupported-pg84", SqlDialectVersion.of(8, 4), tableOnly, true),
            Arguments.of("table-only-supported-pg90", SqlDialectVersion.of(9, 0), tableOnly, false),
            Arguments.of("table-descendants-unsupported-pg84", SqlDialectVersion.of(8, 4), tableDescendants, true),
            Arguments.of("table-descendants-supported-pg90", SqlDialectVersion.of(9, 0), tableDescendants, false),
            Arguments.of("array-literal-unsupported-pg84", SqlDialectVersion.of(8, 4), arrayLiteral, true),
            Arguments.of("array-literal-supported-pg90", SqlDialectVersion.of(9, 0), arrayLiteral, false),
            Arguments.of("array-subscript-unsupported-pg84", SqlDialectVersion.of(8, 4), arraySubscript, true),
            Arguments.of("array-subscript-supported-pg90", SqlDialectVersion.of(9, 0), arraySubscript, false),
            Arguments.of("array-slice-unsupported-pg84", SqlDialectVersion.of(8, 4), arraySlice, true),
            Arguments.of("array-slice-supported-pg90", SqlDialectVersion.of(9, 0), arraySlice, false),
            Arguments.of("collate-expr-unsupported-pg84", SqlDialectVersion.of(8, 4), collateExpr, true),
            Arguments.of("collate-expr-supported-pg90", SqlDialectVersion.of(9, 0), collateExpr, false),
            Arguments.of("at-time-zone-unsupported-pg84", SqlDialectVersion.of(8, 4), atTimeZoneExpr, true),
            Arguments.of("at-time-zone-supported-pg90", SqlDialectVersion.of(9, 0), atTimeZoneExpr, false),
            Arguments.of("power-operator-unsupported-pg84", SqlDialectVersion.of(8, 4), powerExpr, true),
            Arguments.of("power-operator-supported-pg90", SqlDialectVersion.of(9, 0), powerExpr, false),
            Arguments.of("is-distinct-from-unsupported-pg84", SqlDialectVersion.of(8, 4), isDistinctFromPredicate, true),
            Arguments.of("is-distinct-from-supported-pg90", SqlDialectVersion.of(9, 0), isDistinctFromPredicate, false),
            Arguments.of("custom-operator-unsupported-pg84", SqlDialectVersion.of(8, 4), customOperator, true),
            Arguments.of("custom-operator-supported-pg90", SqlDialectVersion.of(9, 0), customOperator, false),
            Arguments.of("typecast-unsupported-pg84", SqlDialectVersion.of(8, 4), postgresTypecast, true),
            Arguments.of("typecast-supported-pg90", SqlDialectVersion.of(9, 0), postgresTypecast, false),
            Arguments.of("lateral-unsupported-pg90", SqlDialectVersion.of(9, 0), lateral, true),
            Arguments.of("lateral-supported-pg93", SqlDialectVersion.of(9, 3), lateral, false),
            Arguments.of("ordinality-unsupported-pg93", SqlDialectVersion.of(9, 3), withOrdinality, true),
            Arguments.of("ordinality-supported-pg94", SqlDialectVersion.of(9, 4), withOrdinality, false),
            Arguments.of("grouping-unsupported-pg94", SqlDialectVersion.of(9, 4), groupingSetsQuery, true),
            Arguments.of("grouping-supported-pg95", SqlDialectVersion.of(9, 5), groupingSetsQuery, false),
            Arguments.of("rollup-unsupported-pg94", SqlDialectVersion.of(9, 4), rollupQuery, true),
            Arguments.of("rollup-supported-pg95", SqlDialectVersion.of(9, 5), rollupQuery, false),
            Arguments.of("cube-unsupported-pg94", SqlDialectVersion.of(9, 4), cubeQuery, true),
            Arguments.of("cube-supported-pg95", SqlDialectVersion.of(9, 5), cubeQuery, false),
            Arguments.of("groups-frame-unsupported-pg10", SqlDialectVersion.of(10, 0), groupsFrame, true),
            Arguments.of("groups-frame-supported-pg11", SqlDialectVersion.of(11, 0), groupsFrame, false),
            Arguments.of("exclude-frame-unsupported-pg10", SqlDialectVersion.of(10, 0), excludeFrame, true),
            Arguments.of("exclude-frame-supported-pg11", SqlDialectVersion.of(11, 0), excludeFrame, false),
            Arguments.of("cte-materialization-unsupported-pg95", SqlDialectVersion.of(9, 5), cteMaterialized, true),
            Arguments.of("cte-materialization-supported-pg12", SqlDialectVersion.of(12, 0), cteMaterialized, false),
            Arguments.of("key-share-unsupported-pg90", SqlDialectVersion.of(9, 0), keyShareLock, true),
            Arguments.of("key-share-supported-pg93", SqlDialectVersion.of(9, 3), keyShareLock, false),
            Arguments.of("skip-locked-unsupported-pg94", SqlDialectVersion.of(9, 4), skipLocked, true),
            Arguments.of("skip-locked-supported-pg95", SqlDialectVersion.of(9, 5), skipLocked, false)
        );
    }

    private static Stream<Arguments> nestedCases() {
        var nestedAsQueryExpr = select(expr(
            select(col("i", "id"))
                .from(tbl("users").as("i"))
                .groupBy(groupingSets(group("i", "id")))
        )).from(tbl("users").as("u"));

        var nestedAsQueryTable = select(star())
            .from(tbl(
                select(col("i", "id"))
                    .from(tbl("users").as("i"))
                    .groupBy(groupingSets(group("i", "id")))
            ).as("x"));

        var nestedAsExists = select(star())
            .from(tbl("users").as("u"))
            .where(exists(
                select(col("i", "id"))
                    .from(tbl("users").as("i"))
                    .groupBy(groupingSets(group("i", "id")))
            ));

        return Stream.of(
            Arguments.of("nested-queryexpr-no-duplicate", nestedAsQueryExpr),
            Arguments.of("nested-querytable-no-duplicate", nestedAsQueryTable),
            Arguments.of("nested-exists-no-duplicate", nestedAsExists)
        );
    }
}
