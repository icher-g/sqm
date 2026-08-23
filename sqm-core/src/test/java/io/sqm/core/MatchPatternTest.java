package io.sqm.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class MatchPatternTest {
    @Test
    void dslReachesEveryPatternVariantAndPreservesQuantifierSemantics() {
        var a = patternVar("A");
        var b = patternVar("B");

        assertSame(a, patternSequence(a));
        assertEquals(List.of(a, b), ((MatchPattern.Sequence) patternSequence(a, b)).elements());
        assertEquals(List.of(a, b), patternAlternation(a, b).alternatives());
        assertEquals(List.of(a, b), patternPermute(a, b).elements());
        assertEquals(MatchPattern.Anchor.Kind.START, patternStart().kind());
        assertEquals(MatchPattern.Anchor.Kind.END, patternEnd().kind());
        assertSame(emptyPattern(), emptyPattern());
        assertEquals(a, excludePattern(a).pattern());

        assertQuantifier(zeroOrMore(a), 0, null, false);
        assertQuantifier(oneOrMore(a), 1, null, false);
        assertQuantifier(optionalPattern(a), 0, 1, false);
        assertQuantifier(repeatPattern(a, 3, 3, false), 3, 3, false);
        assertQuantifier(repeatPattern(a, 2, null, true), 2, null, true);
        assertQuantifier(repeatPattern(a, 0, 5, true), 0, 5, true);
    }

    @Test
    void compositePatternsDefensivelyCopyAndValidateArity() {
        var source = new ArrayList<MatchPattern>(List.of(patternVar("A"), patternVar("B")));
        var sequence = MatchPattern.Sequence.of(source);
        source.clear();

        assertEquals(2, sequence.elements().size());
        assertThrows(UnsupportedOperationException.class, () -> sequence.elements().clear());
        assertThrows(IllegalArgumentException.class, () -> MatchPattern.Sequence.of(List.of(patternVar("A"))));
        assertThrows(IllegalArgumentException.class, () -> MatchPattern.Alternation.of(List.of()));
        assertThrows(IllegalArgumentException.class, () -> MatchPattern.Permutation.of(List.of(patternVar("A"))));
    }

    @Test
    void quantifiedPatternsRejectInvalidBounds() {
        var pattern = patternVar("A");

        assertThrows(NullPointerException.class, () -> MatchPattern.Quantified.of(pattern, null, 1, false));
        assertThrows(IllegalArgumentException.class, () -> repeatPattern(pattern, -1, null, false));
        assertThrows(IllegalArgumentException.class, () -> repeatPattern(pattern, 0, -1, false));
        assertThrows(IllegalArgumentException.class, () -> repeatPattern(pattern, 3, 2, false));
    }

    @Test
    void patternMatcherSelectsEveryVariantAndHonorsFirstMatch() {
        assertEquals("variable", kind(patternVar("A")));
        assertEquals("sequence", kind(patternSequence(patternVar("A"), patternVar("B"))));
        assertEquals("alternation", kind(patternAlternation(patternVar("A"), patternVar("B"))));
        assertEquals("permutation", kind(patternPermute(patternVar("A"), patternVar("B"))));
        assertEquals("anchor", kind(patternStart()));
        assertEquals("empty", kind(emptyPattern()));
        assertEquals("exclusion", kind(excludePattern(patternVar("A"))));
        assertEquals("quantified", kind(oneOrMore(patternVar("A"))));

        var first = patternVar("A").<String>matchPattern()
            .variable(value -> "first")
            .variable(value -> "second")
            .orElse("other");
        assertEquals("first", first);
    }

    private static void assertQuantifier(
        MatchPattern.Quantified quantified,
        int minimum,
        Integer maximum,
        boolean reluctant
    ) {
        assertEquals(minimum, quantified.minimum());
        assertEquals(maximum, quantified.maximum());
        assertEquals(reluctant, quantified.reluctant());
    }

    private static String kind(MatchPattern pattern) {
        return pattern.<String>matchPattern()
            .variable(value -> "variable")
            .sequence(value -> "sequence")
            .alternation(value -> "alternation")
            .permutation(value -> "permutation")
            .anchor(value -> "anchor")
            .empty(value -> "empty")
            .exclusion(value -> "exclusion")
            .quantified(value -> "quantified")
            .orElse("other");
    }
}
