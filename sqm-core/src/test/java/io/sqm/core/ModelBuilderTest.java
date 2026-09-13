package io.sqm.core;

import org.junit.jupiter.api.Test;
import java.util.*;
import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class ModelBuilderTest {
    private ModelRule rule() { return modelRule(cellTarget("sales", 2026), lit(10)); }

    @Test
    void conciseColumnDeclarationsDeriveNamesAndRetainQuotes() {
        var quoted = id("Year Key", QuoteStyle.DOUBLE_QUOTE);
        var queryColumn = col(id("s"), quoted);
        var model = model().partition("country").dimension("product")
            .dimension("source_year", "year").dimension(queryColumn)
            .measure("amount", "sales").rule(rule()).build();
        assertEquals(col("product"), model.main().dimensions().get(0).expression());
        assertEquals(id("product"), model.main().dimensions().get(0).name());
        assertEquals(col("source_year"), model.main().dimensions().get(1).expression());
        assertEquals(id("year"), model.main().dimensions().get(1).name());
        assertEquals(quoted, model.main().dimensions().get(2).name());
        assertEquals(queryColumn, model.main().dimensions().get(2).expression());
        assertEquals(ReturnRows.ALL, model.returnRows());
        assertEquals(NavigationMode.KEEP, model.main().navigationMode());
        assertEquals(UniquenessMode.DIMENSION, model.main().uniquenessMode());
        assertEquals(RuleMode.UPSERT, model.main().rules().defaultMode());
        assertEquals(RuleOrder.SEQUENTIAL, model.main().rules().order());
        assertNull(model.main().rules().iteration());
    }

    @Test
    void fullStateBuildersCopyOptionsAndDoNotShareMutableCollections() {
        var rules = modelRules().defaultMode(RuleMode.UPDATE).order(RuleOrder.AUTOMATIC)
            .iteration(iterationSpec(lit(10), null))
            .rules(List.of(rule())).rule(cellTarget("sales", 2027), lit(20)).build();
        assertEquals(rules, ModelRules.builder(rules).build());
        var main = mainModel().name("forecast").partitions(List.of(modelColumn("country")))
            .dimensions(List.of(modelColumn("year"))).measures(List.of(modelColumn("sales")))
            .navigationMode(NavigationMode.IGNORE).uniquenessMode(UniquenessMode.SINGLE_REFERENCE)
            .rules(rules).build();
        assertEquals(main, MainModel.builder(main).build());
        assertEquals(id("forecast"), main.name());
        var reference = referenceModel().name(id("rates")).query(select(lit(1)).build())
            .dimensions(List.of(modelColumn("year"))).measures(List.of(modelColumn("rate")))
            .navigationMode(NavigationMode.IGNORE).uniquenessMode(UniquenessMode.DIMENSION).build();
        assertEquals(reference, ReferenceModel.builder(reference).build());
        var clause = model().returnRows(ReturnRows.UPDATED).references(List.of(reference)).main(main).build();
        assertEquals(clause, ModelClause.builder(clause).build());
        var copy = ModelClause.builder(clause);
        copy.dimension("additional");
        assertEquals(1, clause.main().dimensions().size());
        assertEquals(2, copy.build().main().dimensions().size());
        assertThrows(UnsupportedOperationException.class, () -> clause.references().clear());
        assertThrows(UnsupportedOperationException.class, () -> main.dimensions().clear());
        assertThrows(UnsupportedOperationException.class, () -> rules.rules().clear());
    }

    @Test
    void modelClauseDeclarationOverloadsBuildEquivalentTypedColumns() {
        var builder = ModelClause.builder();
        builder.partition(modelColumn("a"))
            .partition("b")
            .partition("c", "renamed_c")
            .partition(col("t", "d"))
            .partition(lit(1), "computed")
            .partition(lit(2), id("Quoted", QuoteStyle.DOUBLE_QUOTE));
        builder.dimension(modelColumn("a"))
            .dimension("b")
            .dimension("c", "renamed_c")
            .dimension(col("t", "d"))
            .dimension(lit(1), "computed")
            .dimension(lit(2), id("Quoted", QuoteStyle.DOUBLE_QUOTE));
        builder.measure(modelColumn("a"))
            .measure("b")
            .measure("c", "renamed_c")
            .measure(col("t", "d"))
            .measure(lit(1), "computed")
            .measure(lit(2), id("Quoted", QuoteStyle.DOUBLE_QUOTE));
        builder.rule(cellTarget("sales", 1), lit(20));
        var built = builder.build();
        assertEquals(6, built.main().partitions().size());
        assertEquals(id("renamed_c"), built.main().partitions().get(2).name());
        assertEquals(col("t", "d"), built.main().partitions().get(3).expression());
        assertEquals(id("Quoted", QuoteStyle.DOUBLE_QUOTE), built.main().partitions().get(5).name());
        assertEquals(6, built.main().dimensions().size());
        assertEquals(id("renamed_c"), built.main().dimensions().get(2).name());
        assertEquals(col("t", "d"), built.main().dimensions().get(3).expression());
        assertEquals(id("Quoted", QuoteStyle.DOUBLE_QUOTE), built.main().dimensions().get(5).name());
        assertEquals(6, built.main().measures().size());
        assertEquals(id("renamed_c"), built.main().measures().get(2).name());
        assertEquals(col("t", "d"), built.main().measures().get(3).expression());
        assertEquals(id("Quoted", QuoteStyle.DOUBLE_QUOTE), built.main().measures().get(5).name());
    }

    @Test
    void mainModelDeclarationOverloadsBuildEquivalentTypedColumns() {
        var builder = MainModel.builder();
        builder.partition(modelColumn("a"))
            .partition("b")
            .partition("c", "renamed_c")
            .partition(col("t", "d"))
            .partition(lit(1), "computed")
            .partition(lit(2), id("Quoted", QuoteStyle.DOUBLE_QUOTE));
        builder.dimension(modelColumn("a"))
            .dimension("b")
            .dimension("c", "renamed_c")
            .dimension(col("t", "d"))
            .dimension(lit(1), "computed")
            .dimension(lit(2), id("Quoted", QuoteStyle.DOUBLE_QUOTE));
        builder.measure(modelColumn("a"))
            .measure("b")
            .measure("c", "renamed_c")
            .measure(col("t", "d"))
            .measure(lit(1), "computed")
            .measure(lit(2), id("Quoted", QuoteStyle.DOUBLE_QUOTE));
        builder.rule(cellTarget("sales", 1), lit(20));
        var built = builder.build();
        assertEquals(6, built.partitions().size());
        assertEquals(id("renamed_c"), built.partitions().get(2).name());
        assertEquals(col("t", "d"), built.partitions().get(3).expression());
        assertEquals(id("Quoted", QuoteStyle.DOUBLE_QUOTE), built.partitions().get(5).name());
        assertEquals(6, built.dimensions().size());
        assertEquals(id("renamed_c"), built.dimensions().get(2).name());
        assertEquals(col("t", "d"), built.dimensions().get(3).expression());
        assertEquals(id("Quoted", QuoteStyle.DOUBLE_QUOTE), built.dimensions().get(5).name());
        assertEquals(6, built.measures().size());
        assertEquals(id("renamed_c"), built.measures().get(2).name());
        assertEquals(col("t", "d"), built.measures().get(3).expression());
        assertEquals(id("Quoted", QuoteStyle.DOUBLE_QUOTE), built.measures().get(5).name());
    }

    @Test
    void referenceModelDeclarationOverloadsBuildEquivalentTypedColumns() {
        var builder = ReferenceModel.builder();
        builder.name(id("ref")).query(select(lit(1)).build());
        builder.dimension(modelColumn("a"))
            .dimension("b")
            .dimension("c", "renamed_c")
            .dimension(col("t", "d"))
            .dimension(lit(1), "computed")
            .dimension(lit(2), id("Quoted", QuoteStyle.DOUBLE_QUOTE));
        builder.measure(modelColumn("a"))
            .measure("b")
            .measure("c", "renamed_c")
            .measure(col("t", "d"))
            .measure(lit(1), "computed")
            .measure(lit(2), id("Quoted", QuoteStyle.DOUBLE_QUOTE));
        var built = builder.build();
        assertEquals(6, built.dimensions().size());
        assertEquals(id("renamed_c"), built.dimensions().get(2).name());
        assertEquals(col("t", "d"), built.dimensions().get(3).expression());
        assertEquals(id("Quoted", QuoteStyle.DOUBLE_QUOTE), built.dimensions().get(5).name());
        assertEquals(6, built.measures().size());
        assertEquals(id("renamed_c"), built.measures().get(2).name());
        assertEquals(col("t", "d"), built.measures().get(3).expression());
        assertEquals(id("Quoted", QuoteStyle.DOUBLE_QUOTE), built.measures().get(5).name());
    }

    @Test
    void clauseRulesConvenienceAndStandaloneDeclarationsAgree() {
        var rules = modelRules().rule(rule()).build();
        var clause = model().dimension("year").measure("sales").rules(rules).build();
        assertEquals(rules, clause.main().rules());
        assertEquals(modelColumn(col("year"), id("year")), modelColumn("year"));
        assertEquals(modelColumn(col("country"), id("country")), modelColumn(col("country")));
        assertEquals(modelColumn(col("amount"), id("sales")), modelColumn("amount", "sales"));
    }
}
