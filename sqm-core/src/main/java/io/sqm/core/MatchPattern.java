package io.sqm.core;

import io.sqm.core.match.PatternMatch;
import io.sqm.core.walk.NodeVisitor;

import java.util.List;
import java.util.Objects;

/**
 * Typed grammar tree describing a row pattern recognized by
 * {@link PatternRecognitionTable}.
 */
public sealed interface MatchPattern extends Node permits MatchPattern.Variable,
    MatchPattern.Sequence,
    MatchPattern.Alternation,
    MatchPattern.Permutation,
    MatchPattern.Anchor,
    MatchPattern.Empty,
    MatchPattern.Exclusion,
    MatchPattern.Quantified {

    /**
     * Returns the singleton empty match pattern.
     *
     * @return empty match pattern
     */
    static Empty empty() {
        return Empty.INSTANCE;
    }

    /**
     * Creates a matcher for this pattern variant.
     *
     * @param <R> matcher result type
     * @return pattern matcher
     */
    default <R> PatternMatch<R> matchPattern() {
        return PatternMatch.match(this);
    }

    /**
     * Primary pattern-variable occurrence.
     *
     * @param name primary pattern-variable name
     */
    record Variable(Identifier name) implements MatchPattern {
        /**
         * Validates the pattern variable.
         *
         * @param name primary pattern-variable name
         */
        public Variable {
            Objects.requireNonNull(name, "name");
        }

        /**
         * Creates a primary pattern-variable occurrence.
         *
         * @param name primary pattern-variable name
         * @return pattern variable
         */
        public static Variable of(Identifier name) {
            return new Variable(name);
        }

        /**
         * Accepts a node visitor.
         *
         * @param visitor visitor to accept
         * @param <R> visitor result type
         * @return visitor result
         */
        @Override
        public <R> R accept(NodeVisitor<R> visitor) {
            return visitor.visitPatternVariable(this);
        }
    }

    /**
     * Ordered concatenation of two or more pattern elements.
     *
     * @param elements ordered pattern elements
     */
    record Sequence(List<MatchPattern> elements) implements MatchPattern {
        /**
         * Validates and defensively copies the sequence.
         *
         * @param elements ordered pattern elements
         */
        public Sequence {
            elements = requireAtLeastTwo(elements, "Pattern sequence");
        }

        /**
         * Creates an ordered pattern sequence.
         *
         * @param elements ordered pattern elements
         * @return pattern sequence
         */
        public static Sequence of(List<MatchPattern> elements) {
            return new Sequence(elements);
        }

        /**
         * Accepts a node visitor.
         *
         * @param visitor visitor to accept
         * @param <R> visitor result type
         * @return visitor result
         */
        @Override
        public <R> R accept(NodeVisitor<R> visitor) {
            return visitor.visitPatternSequence(this);
        }
    }

    /**
     * Ordered choice between two or more pattern alternatives.
     *
     * @param alternatives ordered alternatives
     */
    record Alternation(List<MatchPattern> alternatives) implements MatchPattern {
        /**
         * Validates and defensively copies the alternatives.
         *
         * @param alternatives ordered alternatives
         */
        public Alternation {
            alternatives = requireAtLeastTwo(alternatives, "Pattern alternation");
        }

        /**
         * Creates an ordered pattern alternation.
         *
         * @param alternatives ordered alternatives
         * @return pattern alternation
         */
        public static Alternation of(List<MatchPattern> alternatives) {
            return new Alternation(alternatives);
        }

        /**
         * Accepts a node visitor.
         *
         * @param visitor visitor to accept
         * @param <R> visitor result type
         * @return visitor result
         */
        @Override
        public <R> R accept(NodeVisitor<R> visitor) {
            return visitor.visitPatternAlternation(this);
        }
    }

    /**
     * Match of the supplied elements in any order.
     *
     * @param elements elements to permute
     */
    record Permutation(List<MatchPattern> elements) implements MatchPattern {
        /**
         * Validates and defensively copies the permutation.
         *
         * @param elements elements to permute
         */
        public Permutation {
            elements = requireAtLeastTwo(elements, "Pattern permutation");
        }

        /**
         * Creates a pattern permutation.
         *
         * @param elements elements to permute
         * @return pattern permutation
         */
        public static Permutation of(List<MatchPattern> elements) {
            return new Permutation(elements);
        }

        /**
         * Accepts a node visitor.
         *
         * @param visitor visitor to accept
         * @param <R> visitor result type
         * @return visitor result
         */
        @Override
        public <R> R accept(NodeVisitor<R> visitor) {
            return visitor.visitPatternPermutation(this);
        }
    }

    /**
     * Start or end anchor in a match pattern.
     *
     * @param kind anchor kind
     */
    record Anchor(Kind kind) implements MatchPattern {
        /**
         * Pattern anchor kind.
         */
        public enum Kind {
            /** Partition-start anchor. */
            START,
            /** Partition-end anchor. */
            END
        }

        /**
         * Validates the anchor.
         *
         * @param kind anchor kind
         */
        public Anchor {
            Objects.requireNonNull(kind, "kind");
        }

        /**
         * Creates a pattern anchor.
         *
         * @param kind anchor kind
         * @return pattern anchor
         */
        public static Anchor of(Kind kind) {
            return new Anchor(kind);
        }

        /**
         * Accepts a node visitor.
         *
         * @param visitor visitor to accept
         * @param <R> visitor result type
         * @return visitor result
         */
        @Override
        public <R> R accept(NodeVisitor<R> visitor) {
            return visitor.visitPatternAnchor(this);
        }
    }

    /**
     * Empty row pattern with match semantics distinct from grouping.
     */
    record Empty() implements MatchPattern {
        private static final Empty INSTANCE = new Empty();

        /**
         * Accepts a node visitor.
         *
         * @param visitor visitor to accept
         * @param <R> visitor result type
         * @return visitor result
         */
        @Override
        public <R> R accept(NodeVisitor<R> visitor) {
            return visitor.visitEmptyPattern(this);
        }
    }

    /**
     * Pattern whose matched rows are excluded from all-rows output.
     *
     * @param pattern excluded pattern
     */
    record Exclusion(MatchPattern pattern) implements MatchPattern {
        /**
         * Validates the exclusion.
         *
         * @param pattern excluded pattern
         */
        public Exclusion {
            Objects.requireNonNull(pattern, "pattern");
        }

        /**
         * Creates a pattern exclusion.
         *
         * @param pattern excluded pattern
         * @return pattern exclusion
         */
        public static Exclusion of(MatchPattern pattern) {
            return new Exclusion(pattern);
        }

        /**
         * Accepts a node visitor.
         *
         * @param visitor visitor to accept
         * @param <R> visitor result type
         * @return visitor result
         */
        @Override
        public <R> R accept(NodeVisitor<R> visitor) {
            return visitor.visitPatternExclusion(this);
        }
    }

    /**
     * Greedy or reluctant repetition of a child pattern.
     *
     * @param pattern repeated pattern
     * @param minimum non-negative minimum repetition count
     * @param maximum maximum repetition count, or {@code null} when unbounded
     * @param reluctant whether the quantifier is reluctant
     */
    record Quantified(MatchPattern pattern, Integer minimum, Integer maximum, boolean reluctant) implements MatchPattern {
        /**
         * Validates the quantified pattern.
         *
         * @param pattern repeated pattern
         * @param minimum non-negative minimum repetition count
         * @param maximum maximum repetition count, or {@code null} when unbounded
         * @param reluctant whether the quantifier is reluctant
         */
        public Quantified {
            Objects.requireNonNull(pattern, "pattern");
            Objects.requireNonNull(minimum, "minimum");
            if (minimum < 0) {
                throw new IllegalArgumentException("minimum must be non-negative");
            }
            if (maximum != null && maximum < 0) {
                throw new IllegalArgumentException("maximum must be non-negative");
            }
            if (maximum != null && minimum > maximum) {
                throw new IllegalArgumentException("minimum must not exceed maximum");
            }
        }

        /**
         * Creates a quantified pattern.
         *
         * @param pattern repeated pattern
         * @param minimum non-negative minimum repetition count
         * @param maximum maximum repetition count, or {@code null} when unbounded
         * @param reluctant whether the quantifier is reluctant
         * @return quantified pattern
         */
        public static Quantified of(MatchPattern pattern, Integer minimum, Integer maximum, boolean reluctant) {
            return new Quantified(pattern, minimum, maximum, reluctant);
        }

        /**
         * Accepts a node visitor.
         *
         * @param visitor visitor to accept
         * @param <R> visitor result type
         * @return visitor result
         */
        @Override
        public <R> R accept(NodeVisitor<R> visitor) {
            return visitor.visitQuantifiedPattern(this);
        }
    }

    private static List<MatchPattern> requireAtLeastTwo(List<MatchPattern> patterns, String role) {
        Objects.requireNonNull(patterns, "patterns");
        if (patterns.size() < 2) {
            throw new IllegalArgumentException(role + " requires at least two elements");
        }
        return List.copyOf(patterns);
    }
}
