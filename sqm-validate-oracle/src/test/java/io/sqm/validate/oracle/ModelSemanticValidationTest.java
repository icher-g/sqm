package io.sqm.validate.oracle;

import io.sqm.catalog.model.*;
import io.sqm.core.*;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.api.ValidationResult;
import io.sqm.validate.schema.SchemaStatementValidator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class ModelSemanticValidationTest {
    private static final CatalogSchema SCHEMA = CatalogSchema.of(
        CatalogTable.of("public", "sales_data", CatalogColumn.of("country", CatalogType.STRING),
            CatalogColumn.of("p", CatalogType.STRING), CatalogColumn.of("y", CatalogType.INTEGER),
            CatalogColumn.of("amount", CatalogType.DECIMAL)),
        CatalogTable.of("public", "rates", CatalogColumn.of("p", CatalogType.STRING), CatalogColumn.of("rate", CatalogType.DECIMAL)));

    @Test void separatesInputRulesAndClosedOutputAndPreservesTypes() {
        var query = select(col("p"), col("y"), col("sales")).from(tbl("sales_data"))
            .where(col("amount").gt(0)).model(basic(modelRules().rule(cellTarget("sales", "Bike", 2026),
                cellRef("sales", "Bike", 2025).mul(lit(1.1))).build())).orderBy(col("sales")).build();
        valid(query);
        var badOutput = select(col("amount")).from(tbl("sales_data")).model(query.model()).build();
        has(validate(badOutput), ValidationProblem.Code.COLUMN_NOT_FOUND, "amount");
        var badAssignment = basic(modelRules().rule(cellTarget("sales", "Bike", 2026), lit("text")).build());
        has(validate(query(badAssignment)), ValidationProblem.Code.TYPE_MISMATCH, "assignment");
        var badCoordinate = basic(modelRules().rule(cellTarget("sales", "Bike", "wrong"), lit(1)).build());
        has(validate(query(badCoordinate)), ValidationProblem.Code.TYPE_MISMATCH, "coordinate");
        has(validate(select(col("sales")).from(tbl("sales_data")).model(basic(modelRules()
            .rule(cellTarget("sales", "Bike", 2026), col("amount")).build())).build()),
            ValidationProblem.Code.COLUMN_NOT_FOUND, "amount");
        has(validate(select(col("sales")).from(tbl("sales_data")).model(query.model()).orderBy(col("amount")).build()),
            ValidationProblem.Code.COLUMN_NOT_FOUND, "amount");
    }

    @Test void validatesDeclarationsTargetsQualifiersAndQuoteAwareNames() {
        var duplicate = model().dimension("p").dimension("y").measure("amount", "P")
            .rules(modelRules().rule(cellTarget("P", "Bike", 1), lit(1)).build()).build();
        has(validate(query(duplicate)), "duplicate declared column");
        var quoted = model().dimension("p").dimension("y").measure(modelColumn(col("amount"), id("p", QuoteStyle.DOUBLE_QUOTE)))
            .rules(modelRules().rule(cellTarget(id("p", QuoteStyle.DOUBLE_QUOTE)).address("Bike").address(1).build(), lit(1)).build()).build();
        valid(select(col(id("p", QuoteStyle.DOUBLE_QUOTE))).from(tbl("sales_data")).model(quoted).build());
        has(validate(query(basic(modelRules().rule(cellTarget("y", "Bike", 1), lit(1)).build()))), "assignment target");
        has(validate(query(basic(modelRules().rule(cellTarget("sales", "Bike"), lit(1)).build()))), "every declared dimension");
        has(validate(query(basic(modelRules().rule(cellTarget("sales", "Bike", 1), cellRef("missing", "Bike", 1)).build()))), "declared measure");
        has(validate(query(basic(modelRules().rule(cellTarget("sales", "Bike", 1),
            cellRef(id("unknown"), id("sales"), cellValue("Bike"), cellValue(1))).build()))), "unknown model qualifier");
        var named = mainModel().name("forecast").dimension("p").dimension("y").measure("amount", "sales")
            .rules(modelRules().rule(cellTarget("sales", "Bike", 1), cellRef(id("forecast"), id("sales"), cellValue("Bike"), cellValue(1))).build()).build();
        valid(query(model().main(named).build()));
        var ref = referenceModel().name(id("forecast")).query(select(col("p"), col("rate")).from(tbl("rates")).build())
            .dimension("p").measure("rate").build();
        has(validate(query(model().reference(ref).main(named).build())), "duplicate model name");
    }

    @Test void resolvesReferenceDeclarationsAndReadOnlyCellNamespaces() {
        var ref = referenceModel().name(id("growth")).query(select(col("p"), col("rate")).from(tbl("rates")).build())
            .dimension("p").measure("rate").build();
        var main = mainModel().dimension("p").dimension("y").measure("amount", "sales")
            .rules(modelRules().rule(cellTarget("sales", "Bike", 1), cellRef(id("growth"), id("rate"), cellValue("Bike"))).build()).build();
        valid(query(model().reference(ref).main(main).build()));
        var bad = referenceModel().name(id("growth")).query(select(col("p")).from(tbl("rates")).build())
            .dimension("p").measure("rate").build();
        has(validate(query(model().reference(bad).main(main).build())), ValidationProblem.Code.COLUMN_NOT_FOUND, "rate");
        var referenceSource = with(cte("source_rates", select(col("p"), col("amount")).from(tbl("sales_data")).build())
            .columnAliases("p", "rate")).body(select(col("p"), col("rate")).from(tbl("source_rates")).build());
        var cteReference = referenceModel("growth", referenceSource).dimension("p").measure("rate").build();
        valid(query(model().reference(cteReference).main(main).build()));
        var outerSource = cte("source_rates", select(col("p"), col("amount")).from(tbl("sales_data")).build()).columnAliases("p", "rate");
        var outerReference = referenceModel("growth", select(col("p"), col("rate")).from(tbl("source_rates")).build())
            .dimension("p").measure("rate").build();
        valid(with(outerSource).body(query(model().reference(outerReference).main(main).build())));
    }

    @Test void isolatesUncorrelatedQueriesButAllowsLocalShadowingAndNestedCorrelation() {
        var outer = tbl("sales_data").as("s");
        var correlated = referenceModel().name(id("g")).query(select(col("r", "p"), col("r", "rate"))
            .from(tbl("rates").as("r")).where(col("r", "p").eq(col("s", "p"))).build()).dimension("p").measure("rate").build();
        var main = basic(modelRules().rule(cellTarget("sales", "Bike", 1), lit(1)).build()).main();
        has(validate(select(col("sales")).from(outer).model(model().reference(correlated).main(main).build()).build()), "correlated outer");
        var shadow = referenceModel().name(id("g")).query(select(col("s", "p"), col("s", "rate"))
            .from(tbl("rates").as("s")).where(exists(select(lit(1)).from(tbl("rates").as("r"))
                .where(col("r", "p").eq(col("s", "p"))).build())).build()).dimension("p").measure("rate").build();
        valid(select(col("sales")).from(outer).model(model().reference(shadow).main(main).build()).build());
        var forQuery = select(col("s", "y")).from(tbl("rates")).build();
        has(validate(select(col("sales")).from(outer).model(basic(modelRules().rule(cellTarget("sales", cellValue("Bike"),
            cellForValues("y", forQuery)), lit(1)).build())).build()), "correlated outer");
        var unqualified = referenceModel().name(id("g")).query(select(col("p"), col("rate")).from(tbl("rates"))
            .where(col("y").eq(1)).build()).dimension("p").measure("rate").build();
        has(validate(select(col("sales")).from(outer).model(model().reference(unqualified).main(main).build()).build()), "correlated outer");
    }

    @Test void checksSymbolicSelectorsGeneratorsAndSingleCellReads() {
        valid(query(basic(modelRules().rule(cellTarget("sales", cellAny(), cellCondition(col("y").between(lit(1), lit(3)))),
            modelAggregateExpr(func("sum", col("sales")), List.of(cellAny(), cellCondition(col("y").between(lit(1), lit(3)))))).build())));
        valid(query(basic(modelRules().rule(cellTarget("sales", cellValue("Bike"), cellForValues("y", 1, 2)), lit(1)).build())));
        has(validate(query(basic(modelRules().rule(cellTarget("sales", cellCondition(col("y").eq(1)), cellValue(1)), lit(1)).build()))), "own dimension");
        has(validate(query(basic(modelRules().rule(cellTarget("sales", cellForValues("y", 1), cellValue(1)), lit(1)).build()))), "dimension positions");
        has(validate(query(basic(modelRules().rule(cellTarget("sales", "Bike", 1), cellRef("sales", cellAny(), cellValue(1))).build()))), "single cell");
        valid(query(basic(modelRules().rule(cellTarget("sales", "Bike", 1), cellRef("sales", cellCondition(col("p").eq("Bike")), cellValue(1))).build())));
        has(validate(query(basic(modelRules().rule(cellTarget("sales", "Bike", 1), modelAggregateExpr(func("upper", col("sales")),
            List.of(cellAny(), cellAny()))).build()))), "aggregate function");
        has(validate(query(basic(modelRules().rule(cellTarget("sales", "Bike", 1), modelAggregateExpr(func("sum", col("y")),
            List.of(cellAny(), cellAny()))).build()))), "aggregate argument");
    }

    @Test void checksSpecialExpressionPlacementAndNestedCells() {
        var cell = cellRef("sales", "Bike", 1);
        valid(query(basic(modelRules().iteration(iterationSpec(lit(3), previousModelValueExpr(cell).eq(cell)))
            .rule(cellTarget("sales", "Bike", 2), cellRef("sales", "Bike", currentDimension()).add(iterationNumber())).build())));
        for (var expression : List.of(currentDimension(), currentDimension("sales"), previousModelValueExpr(cell), iterationNumber())) {
            assertFalse(validate(query(basic(modelRules().rule(cellTarget("sales", "Bike", 1), expression).build()))).ok(), expression.toString());
        }
        has(validate(query(basic(modelRules().rule(cellTarget("sales", "Bike", 1), currentDimension("p")).build()))), ValidationProblem.Code.TYPE_MISMATCH, "assignment");
        has(validate(query(basic(modelRules().rule(cellTarget("sales", currentDimension("p"), lit(1)), lit(1)).build()))), "CV is allowed only");
        has(validate(query(basic(modelRules().rule(cellTarget("sales", "Bike", 1), expr(select(lit(1)).build())).build()))), "subqueries are allowed only");
        var inner = cellRef("sales", "Bike", 1);
        valid(query(basic(modelRules().rule(cellTarget("sales", "Bike", 2), cellRef("sales", "Bike", inner)).build())));
        has(validate(query(basic(modelRules().rule(cellTarget("sales", "Bike", 2), cellRef("sales", "Bike", cellRef("sales", "Bike", inner))).build()))), "one level");
        has(validate(query(basic(modelRules().order(RuleOrder.AUTOMATIC).rule(cellTarget("sales", "Bike", inner), lit(1)).build()))), "static measures");
    }

    @Test void derivedAndCteOutputsAreClosedAndTyped() {
        var source = query(basic(modelRules().rule(cellTarget("sales", "Bike", 1), lit(1)).build()));
        valid(select(col("q", "sales")).from(tbl(source).as("q")).build());
        has(validate(select(col("q", "amount")).from(tbl(source).as("q")).build()), ValidationProblem.Code.COLUMN_NOT_FOUND, "amount");
        has(validate(select(lit(1)).from(tbl(source).as("q")).where(col("q", "sales").eq("text")).build()), ValidationProblem.Code.TYPE_MISMATCH, "comparison");
        valid(with(cte("forecast", source)).body(select(col("sales")).from(tbl("forecast")).build()));
        has(validate(with(cte("forecast", source)).body(select(col("amount")).from(tbl("forecast")).build())), ValidationProblem.Code.COLUMN_NOT_FOUND, "amount");
    }

    @Test void checksRuleOrderingAnalyticsAndOracleModeConversions() {
        var analytic = func("sum", col("sales")).over(over(partition(col("p"))));
        valid(query(basic(modelRules().defaultMode(RuleMode.UPDATE).rule(cellTarget("sales", "Bike", 1), analytic).build())));
        // UPSERT with symbolic predicates becomes UPDATE; UPSERT ALL with only symbolic predicates does too.
        for (var mode : List.of(RuleMode.UPSERT, RuleMode.UPSERT_ALL)) {
            valid(query(basic(modelRules().defaultMode(mode).rule(cellTarget("sales", cellAny(), cellAny()), analytic).build())));
        }
        has(validate(query(basic(modelRules().rule(cellTarget("sales", "Bike", 1), analytic).build()))), "analytic functions require UPDATE");
        has(validate(query(basic(modelRules().rule(modelRule(RuleMode.UPDATE, cellTarget("sales", "Bike", 1),
            orderBy(col("y")), analytic)).build()))), "analytic functions require UPDATE");
        has(validate(query(basic(modelRules().defaultMode(RuleMode.UPDATE).rule(cellTarget("sales", cellValue("Bike"),
            cellForValues("y", 1, 2)), analytic).build()))), "analytic functions require UPDATE");
        has(validate(query(basic(modelRules().defaultMode(RuleMode.UPDATE).rule(cellTarget("sales", "Bike", 1),
            func("sum", cellRef("sales", "Bike", 1)).over(over())).build()))), "before OVER");
        has(validate(query(basic(modelRules().defaultMode(RuleMode.UPDATE).rule(cellTarget("sales", "Bike", 1),
            func("sum", col("sales")).over(over(orderBy(func("sum", col("sales")))))).build()))), "OVER arguments");
        has(validate(query(basic(modelRules().rule(modelRule(null, cellTarget("sales", "Bike", 1), orderBy(1), lit(1))).build()))), "positional ordinals");
        var partitioned = model().partition("country").dimension("p").dimension("y").measure("amount", "sales")
            .rules(modelRules().rule(modelRule(null, cellTarget("sales", "Bike", 1), orderBy(col("country")), lit(1))).build()).build();
        has(validate(query(partitioned)), "ORDER BY must name a dimension or measure");
        has(validate(query(basic(modelRules().rule(modelRule(null, cellTarget("sales", "Bike", 1),
            orderBy(cellRef("sales", "Bike", 1)), lit(1))).build()))), "ORDER BY must resolve");
        has(validate(query(basic(modelRules().rule(cellTarget("sales", "Bike", 1), isAny()).build()))), "ANY requires");
    }

    @Test void checksSpecialExpressionsInAllPhasesAndNestedAggregateRules() {
        var cell = cellRef("sales", "Bike", 1);
        valid(query(basic(modelRules().rule(cellTarget("sales", "Bike", 1), presenceValue(cell, lit(1), lit(0))).build())));
        valid(query(basic(modelRules().rule(cellTarget("sales", "Bike", 1), presenceNonNullValue(cell, lit(1), lit(0))).build())));
        valid(query(basic(modelRules().rule(cellTarget("sales", "Bike", 1), cellIsPresent(cell)).build())));
        has(validate(query(basic(modelRules().rule(cellTarget("sales", "Bike", presenceValue(cell, lit(1), lit(0))), lit(1)).build()))), "presence functions");
        has(validate(query(basic(modelRules().rule(cellTarget("sales", "Bike", cellIsPresent(cell)), lit(1)).build()))), "IS PRESENT requires");
        var aggregate = modelAggregateExpr(func("sum", col("sales")), List.of(cellValue("Bike"), cellAny()));
        has(validate(query(basic(modelRules().rule(cellTarget("sales", "Bike", aggregate), lit(1)).build()))), "aggregate cell expressions");
        has(validate(query(basic(modelRules().rule(cellTarget("sales", "Bike", 1), cellRef("sales", "Bike", aggregate)).build()))), "nested cell references must be single");
        has(validate(query(basic(modelRules().rule(cellTarget("sales", cellCondition(func("sum", col("p")).eq(1)), cellValue(1)), lit(1)).build()))), "conditions cannot contain aggregates");
    }

    @Test void forSourcesRejectBindsNestedQueriesAndWithTables() {
        for (var query : List.of(select(param("y")).build(), select(expr(select(lit(1)).build())).build(),
            select(star()).from(tbl(select(lit(1)).build()).as("nested")).build(),
            with(cte("years", select(lit(1).as("y")).build())).body(select(col("y")).from(tbl("years")).build()))) {
            has(validate(query(basic(modelRules().rule(cellTarget("sales", cellValue("Bike"), cellForValues("y", query)), lit(1)).build()))), "FOR source queries cannot");
        }
        var forCte = query(basic(modelRules().rule(cellTarget("sales", cellValue("Bike"),
            cellForValues("y", select(col("y")).from(tbl("years")).build())), lit(1)).build()));
        has(validate(with(cte("years", select(lit(1).as("y")).build()).columnAliases("y")).body(forCte)), "cannot reference WITH tables");
    }

    @Test void checksRecursiveMembersAndLockingButAllowsModelAnchorsAndPagination() {
        var anchor = select(col("p"), col("y"), col("sales")).from(tbl("sales_data"))
            .model(basic(modelRules().rule(cellTarget("sales", "Bike", 1), lit(1)).build())).build();
        var member = select(col("p"), col("y"), col("sales")).from(tbl("forecast")).build();
        valid(with(cte("forecast", anchor.unionAll(member)).columnAliases("p", "y", "sales"))
            .recursive(true).body(select(star()).from(tbl("forecast")).build()));
        var modelMember = select(col("p"), col("y"), col("sales")).from(tbl("forecast"))
            .model(model().dimension("p").dimension("y").measure("sales").rule(cellTarget("sales", "Bike", 1), lit(1)).build()).build();
        has(validate(with(cte("forecast", anchor.unionAll(modelMember)).columnAliases("p", "y", "sales"))
            .recursive(true).body(select(star()).from(tbl("forecast")).build())), "recursive WITH member");
        valid(SelectQuery.builder(anchor).orderBy(col("y")).offset(1).limit(3).build());
        has(validate(SelectQuery.builder(anchor).lockFor(LockMode.UPDATE, List.of(), false, false).build()), "cannot be locked");
    }

    private static ModelClause basic(ModelRules rules) {
        return model().dimension("p").dimension("y").measure("amount", "sales").rules(rules).build();
    }
    private static SelectQuery query(ModelClause model) { return select(col("sales")).from(tbl("sales_data")).model(model).build(); }
    private static ValidationResult validate(Statement statement) { return SchemaStatementValidator.of(SCHEMA, OracleValidationDialect.of()).validate(statement); }
    private static void valid(Statement statement) { var result = validate(statement); assertTrue(result.ok(), () -> result.problems().toString()); }
    private static void has(ValidationResult result, String message) { assertTrue(result.problems().stream().anyMatch(p -> p.message().contains(message)), () -> result.problems().toString()); }
    private static void has(ValidationResult result, ValidationProblem.Code code, String message) { assertTrue(result.problems().stream().anyMatch(p -> p.code() == code && p.message().contains(message)), () -> result.problems().toString()); }
}
