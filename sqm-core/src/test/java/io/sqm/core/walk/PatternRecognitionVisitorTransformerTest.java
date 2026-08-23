package io.sqm.core.walk;

import io.sqm.core.*;
import io.sqm.core.transform.RecursiveNodeTransformer;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class PatternRecognitionVisitorTransformerTest {
    @Test
    void recursiveVisitorDispatchesEveryPatternRecognitionNodeAndTraversesChildren() {
        var table = fullTable();
        var seen = new LinkedHashSet<String>();
        var visitor = recordingVisitor(seen);

        table.accept(visitor);

        assertEquals(Set.of(
            "table", "measure", "definition", "subset", "rows", "skip",
            "variable", "sequence", "alternation", "permutation", "anchor", "empty",
            "exclusion", "quantified", "column", "classifier", "number", "navigation", "evaluation"
        ), seen);
    }

    @Test
    void recursiveTransformerReturnsSameInstancesWhenNothingChanges() {
        var table = fullTable();
        var transformer = new RecursiveNodeTransformer() {
        };

        assertSame(table, transformer.transform(table));
        assertSame(table.pattern(), transformer.transform(table.pattern()));
        assertSame(table.measures().getFirst(), transformer.transform(table.measures().getFirst()));
    }

    @Test
    void recursiveTransformerRebuildsOnlyChangedBranches() {
        var table = fullTable();
        var transformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitPatternColumnExpr(PatternColumnExpr expression) {
                if (expression.column().value().equals("amount")) {
                    return PatternColumnExpr.of(expression.variable(), id("value"));
                }
                return expression;
            }

            @Override
            public Node visitPatternVariable(MatchPattern.Variable pattern) {
                return pattern.name().value().equals("B") ? patternVar("C") : pattern;
            }
        };

        var transformed = (PatternRecognitionTable) transformer.transform(table);

        assertNotSame(table, transformed);
        assertSame(table.source(), transformed.source());
        assertNotSame(table.pattern(), transformed.pattern());
        assertNotSame(table.measures().getFirst(), transformed.measures().getFirst());
        assertEquals("value", patternColumnFromMeasure(transformed).column().value());
        assertEquals("amount", patternColumnFromMeasure(table).column().value());

        var sequence = (MatchPattern.Sequence) transformed.pattern();
        assertSame(((MatchPattern.Sequence) table.pattern()).elements().getFirst(), sequence.elements().getFirst());
        var alternation = (MatchPattern.Alternation) sequence.elements().get(1);
        var permutation = (MatchPattern.Permutation) alternation.alternatives().get(1);
        var quantified = (MatchPattern.Quantified) permutation.elements().get(1);
        assertEquals("C", ((MatchPattern.Variable) quantified.pattern()).name().value());
    }

    @Test
    void tableMatcherSelectsPatternRecognitionRelation() {
        var table = fullTable();

        var result = table.<String>matchTableRef()
            .table(value -> "base")
            .patternRecognition(value -> value.alias().value())
            .orElse("other");

        assertEquals("mr", result);
    }

    @Test
    void patternIdentifierHookRenamesDeclarationsAndScopedReferencesTogether() {
        var table = fullTable();
        var transformer = new RecursiveNodeTransformer() {
            @Override
            protected Identifier transformPatternIdentifier(Identifier identifier) {
                return identifier.value().equals("A") ? id("X") : identifier;
            }
        };

        var transformed = (PatternRecognitionTable) transformer.transform(table);
        var sequence = (MatchPattern.Sequence) transformed.pattern();
        var alternation = (MatchPattern.Alternation) sequence.elements().get(1);
        var exclusion = (MatchPattern.Exclusion) alternation.alternatives().getFirst();
        var permutation = (MatchPattern.Permutation) alternation.alternatives().get(1);

        assertEquals("X", ((MatchPattern.Variable) exclusion.pattern()).name().value());
        assertEquals("X", ((MatchPattern.Variable) permutation.elements().getFirst()).name().value());
        assertEquals("X", transformed.subsets().getFirst().variables().getFirst().value());
        assertEquals("X", transformed.definitions().getFirst().variable().value());
        assertEquals(
            "X",
            ((PatternColumnExpr) ((ComparisonPredicate) transformed.definitions().getFirst().condition()).lhs())
                .variable()
                .value()
        );
        assertEquals("X", patternColumnFromMeasure(transformed).variable().value());
        assertEquals("A", patternColumnFromMeasure(table).variable().value());
    }

    private static PatternRecognitionTable fullTable() {
        var expression = running(first(patternColumn("A", "amount"), 1));
        var pattern = patternSequence(
            patternStart(),
            patternAlternation(
                excludePattern(patternVar("A")),
                patternPermute(patternVar("A"), oneOrMore(patternVar("B")))
            ),
            emptyPattern(),
            patternEnd()
        );
        return matchRecognize(tbl("sales"))
            .partitionBy(col("account_id"))
            .orderBy(col("event_time").asc())
            .measure(expression, "amount_value")
            .measure(classifier(), "class_name")
            .measure(matchNumber(), "match_no")
            .rowsPerMatch(allRowsPerMatch())
            .afterMatchSkip(skipToLast("B"))
            .pattern(pattern)
            .subset(patternSubset("AB", "A", "B"))
            .define(patternDefinition("A", patternColumn("A", "amount").gt(0)))
            .define(patternDefinition("B", patternColumn("B", "amount").gt(prev(patternColumn("B", "amount")))))
            .as("mr")
            .build();
    }

    private static PatternColumnExpr patternColumnFromMeasure(PatternRecognitionTable table) {
        var evaluation = (PatternEvaluationExpr) table.measures().getFirst().expression();
        var navigation = (PatternNavigationExpr) evaluation.expression();
        return (PatternColumnExpr) navigation.expression();
    }

    private static RecursiveNodeVisitor<Void> recordingVisitor(Set<String> seen) {
        return new RecursiveNodeVisitor<>() {
            @Override
            protected Void defaultResult() {
                return null;
            }

            @Override
            public Void visitPatternRecognitionTable(PatternRecognitionTable table) {
                seen.add("table");
                return super.visitPatternRecognitionTable(table);
            }

            @Override
            public Void visitPatternMeasure(PatternMeasure measure) {
                seen.add("measure");
                return super.visitPatternMeasure(measure);
            }

            @Override
            public Void visitPatternDefinition(PatternDefinition definition) {
                seen.add("definition");
                return super.visitPatternDefinition(definition);
            }

            @Override
            public Void visitPatternSubset(PatternSubset subset) {
                seen.add("subset");
                return super.visitPatternSubset(subset);
            }

            @Override
            public Void visitRowsPerMatch(RowsPerMatch rowsPerMatch) {
                seen.add("rows");
                return super.visitRowsPerMatch(rowsPerMatch);
            }

            @Override
            public Void visitAfterMatchSkip(AfterMatchSkip afterMatchSkip) {
                seen.add("skip");
                return super.visitAfterMatchSkip(afterMatchSkip);
            }

            @Override
            public Void visitPatternVariable(MatchPattern.Variable pattern) {
                seen.add("variable");
                return super.visitPatternVariable(pattern);
            }

            @Override
            public Void visitPatternSequence(MatchPattern.Sequence pattern) {
                seen.add("sequence");
                return super.visitPatternSequence(pattern);
            }

            @Override
            public Void visitPatternAlternation(MatchPattern.Alternation pattern) {
                seen.add("alternation");
                return super.visitPatternAlternation(pattern);
            }

            @Override
            public Void visitPatternPermutation(MatchPattern.Permutation pattern) {
                seen.add("permutation");
                return super.visitPatternPermutation(pattern);
            }

            @Override
            public Void visitPatternAnchor(MatchPattern.Anchor pattern) {
                seen.add("anchor");
                return super.visitPatternAnchor(pattern);
            }

            @Override
            public Void visitEmptyPattern(MatchPattern.Empty pattern) {
                seen.add("empty");
                return super.visitEmptyPattern(pattern);
            }

            @Override
            public Void visitPatternExclusion(MatchPattern.Exclusion pattern) {
                seen.add("exclusion");
                return super.visitPatternExclusion(pattern);
            }

            @Override
            public Void visitQuantifiedPattern(MatchPattern.Quantified pattern) {
                seen.add("quantified");
                return super.visitQuantifiedPattern(pattern);
            }

            @Override
            public Void visitPatternColumnExpr(PatternColumnExpr expression) {
                seen.add("column");
                return super.visitPatternColumnExpr(expression);
            }

            @Override
            public Void visitClassifierExpr(ClassifierExpr expression) {
                seen.add("classifier");
                return super.visitClassifierExpr(expression);
            }

            @Override
            public Void visitMatchNumberExpr(MatchNumberExpr expression) {
                seen.add("number");
                return super.visitMatchNumberExpr(expression);
            }

            @Override
            public Void visitPatternNavigationExpr(PatternNavigationExpr expression) {
                seen.add("navigation");
                return super.visitPatternNavigationExpr(expression);
            }

            @Override
            public Void visitPatternEvaluationExpr(PatternEvaluationExpr expression) {
                seen.add("evaluation");
                return super.visitPatternEvaluationExpr(expression);
            }
        };
    }
}
