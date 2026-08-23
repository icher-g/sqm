package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Relation transform that recognizes ordered row patterns in a source relation.
 */
public non-sealed interface PatternRecognitionTable extends TableRef {
    /**
     * Creates a pattern-recognition relation with its complete semantic state.
     *
     * @param source source relation
     * @param partitionBy optional partition specification
     * @param orderBy optional ordering specification
     * @param measures measure definitions in source order
     * @param rowsPerMatch output-cardinality behavior
     * @param afterMatchSkip after-match resume behavior
     * @param pattern required typed match pattern
     * @param subsets subset definitions in source order
     * @param definitions non-empty variable definitions in source order
     * @param alias optional result alias
     * @return immutable pattern-recognition relation
     */
    static PatternRecognitionTable of(
        TableRef source,
        PartitionBy partitionBy,
        OrderBy orderBy,
        List<PatternMeasure> measures,
        RowsPerMatch rowsPerMatch,
        AfterMatchSkip afterMatchSkip,
        MatchPattern pattern,
        List<PatternSubset> subsets,
        List<PatternDefinition> definitions,
        Identifier alias
    ) {
        return new Impl(
            source,
            partitionBy,
            orderBy,
            measures,
            rowsPerMatch,
            afterMatchSkip,
            pattern,
            subsets,
            definitions,
            alias
        );
    }

    /**
     * Creates an empty builder.
     *
     * @return pattern-recognition builder
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a builder initialized from an existing relation.
     *
     * @param table relation to copy
     * @return initialized pattern-recognition builder
     */
    static Builder builder(PatternRecognitionTable table) {
        return new Builder(table);
    }

    /**
     * Returns the source relation.
     *
     * @return source relation
     */
    TableRef source();

    /**
     * Returns the optional partition specification.
     *
     * @return partition specification, or {@code null}
     */
    PartitionBy partitionBy();

    /**
     * Returns the optional ordering specification.
     *
     * @return ordering specification, or {@code null}
     */
    OrderBy orderBy();

    /**
     * Returns measure definitions in source order.
     *
     * @return immutable measure list
     */
    List<PatternMeasure> measures();

    /**
     * Returns the output-cardinality behavior.
     *
     * @return rows-per-match specification
     */
    RowsPerMatch rowsPerMatch();

    /**
     * Returns the after-match resume behavior.
     *
     * @return after-match skip specification
     */
    AfterMatchSkip afterMatchSkip();

    /**
     * Returns the typed match pattern.
     *
     * @return match pattern
     */
    MatchPattern pattern();

    /**
     * Returns subset definitions in source order.
     *
     * @return immutable subset list
     */
    List<PatternSubset> subsets();

    /**
     * Returns variable definitions in source order.
     *
     * @return immutable non-empty definition list
     */
    List<PatternDefinition> definitions();

    /**
     * Returns the optional result alias.
     *
     * @return result alias, or {@code null}
     */
    Identifier alias();

    /**
     * Creates a copy with the provided result alias.
     *
     * @param alias result alias, or {@code null}
     * @return copied relation
     */
    default PatternRecognitionTable as(String alias) {
        return as(alias == null ? null : Identifier.of(alias));
    }

    /**
     * Creates a copy with the provided result alias.
     *
     * @param alias result alias, or {@code null}
     * @return copied relation
     */
    default PatternRecognitionTable as(Identifier alias) {
        return of(
            source(),
            partitionBy(),
            orderBy(),
            measures(),
            rowsPerMatch(),
            afterMatchSkip(),
            pattern(),
            subsets(),
            definitions(),
            alias
        );
    }

    /**
     * Accepts a node visitor.
     *
     * @param visitor visitor to accept
     * @param <R> visitor result type
     * @return visitor result
     */
    @Override
    default <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitPatternRecognitionTable(this);
    }

    /**
     * Mutable builder for constructing immutable {@link PatternRecognitionTable} instances.
     */
    final class Builder {
        private TableRef source;
        private PartitionBy partitionBy;
        private OrderBy orderBy;
        private final List<PatternMeasure> measures = new ArrayList<>();
        private RowsPerMatch rowsPerMatch;
        private AfterMatchSkip afterMatchSkip;
        private MatchPattern pattern;
        private final List<PatternSubset> subsets = new ArrayList<>();
        private final List<PatternDefinition> definitions = new ArrayList<>();
        private Identifier alias;

        private Builder() {
        }

        private Builder(PatternRecognitionTable table) {
            Objects.requireNonNull(table, "table");
            source = table.source();
            partitionBy = table.partitionBy();
            orderBy = table.orderBy();
            measures.addAll(table.measures());
            rowsPerMatch = table.rowsPerMatch();
            afterMatchSkip = table.afterMatchSkip();
            pattern = table.pattern();
            subsets.addAll(table.subsets());
            definitions.addAll(table.definitions());
            alias = table.alias();
        }

        /**
         * Sets the source relation.
         *
         * @param source source relation
         * @return this builder
         */
        public Builder source(TableRef source) {
            this.source = Objects.requireNonNull(source, "source");
            return this;
        }

        /**
         * Sets the optional partition specification.
         *
         * @param partitionBy partition specification, or {@code null}
         * @return this builder
         */
        public Builder partitionBy(PartitionBy partitionBy) {
            this.partitionBy = partitionBy;
            return this;
        }

        /**
         * Sets the partition expressions.
         *
         * @param expressions partition expressions
         * @return this builder
         */
        public Builder partitionBy(Expression... expressions) {
            return partitionBy(PartitionBy.of(expressions));
        }

        /**
         * Sets the optional ordering specification.
         *
         * @param orderBy ordering specification, or {@code null}
         * @return this builder
         */
        public Builder orderBy(OrderBy orderBy) {
            this.orderBy = orderBy;
            return this;
        }

        /**
         * Sets the order items.
         *
         * @param items order items
         * @return this builder
         */
        public Builder orderBy(OrderItem... items) {
            return orderBy(OrderBy.of(items));
        }

        /**
         * Appends a measure.
         *
         * @param measure measure to append
         * @return this builder
         */
        public Builder measure(PatternMeasure measure) {
            measures.add(Objects.requireNonNull(measure, "measure"));
            return this;
        }

        /**
         * Appends a measure with a string alias.
         *
         * @param expression measure expression
         * @param alias required output alias
         * @return this builder
         */
        public Builder measure(Expression expression, String alias) {
            return measure(expression, Identifier.of(alias));
        }

        /**
         * Appends a measure with a quote-aware alias.
         *
         * @param expression measure expression
         * @param alias required output alias
         * @return this builder
         */
        public Builder measure(Expression expression, Identifier alias) {
            return measure(PatternMeasure.of(expression, alias));
        }

        /**
         * Sets the rows-per-match behavior.
         *
         * @param rowsPerMatch rows-per-match behavior
         * @return this builder
         */
        public Builder rowsPerMatch(RowsPerMatch rowsPerMatch) {
            this.rowsPerMatch = Objects.requireNonNull(rowsPerMatch, "rowsPerMatch");
            return this;
        }

        /**
         * Selects one output row per match.
         *
         * @return this builder
         */
        public Builder oneRowPerMatch() {
            return rowsPerMatch(RowsPerMatch.of(RowsPerMatch.Mode.ONE, RowsPerMatch.EmptyMatchHandling.DEFAULT));
        }

        /**
         * Selects all output rows per match with default empty-match handling.
         *
         * @return this builder
         */
        public Builder allRowsPerMatch() {
            return rowsPerMatch(RowsPerMatch.of(RowsPerMatch.Mode.ALL, RowsPerMatch.EmptyMatchHandling.DEFAULT));
        }

        /**
         * Sets the after-match resume behavior.
         *
         * @param afterMatchSkip after-match skip behavior
         * @return this builder
         */
        public Builder afterMatchSkip(AfterMatchSkip afterMatchSkip) {
            this.afterMatchSkip = Objects.requireNonNull(afterMatchSkip, "afterMatchSkip");
            return this;
        }

        /**
         * Resumes after the last row of the accepted match.
         *
         * @return this builder
         */
        public Builder skipPastLastRow() {
            return afterMatchSkip(AfterMatchSkip.of(
                AfterMatchSkip.Kind.PAST_LAST_ROW,
                AfterMatchSkip.Position.DEFAULT,
                null
            ));
        }

        /**
         * Resumes at the next row after the match start.
         *
         * @return this builder
         */
        public Builder skipToNextRow() {
            return afterMatchSkip(AfterMatchSkip.of(
                AfterMatchSkip.Kind.TO_NEXT_ROW,
                AfterMatchSkip.Position.DEFAULT,
                null
            ));
        }

        /**
         * Sets the required match pattern.
         *
         * @param pattern match pattern
         * @return this builder
         */
        public Builder pattern(MatchPattern pattern) {
            this.pattern = Objects.requireNonNull(pattern, "pattern");
            return this;
        }

        /**
         * Appends a subset definition.
         *
         * @param subset subset definition
         * @return this builder
         */
        public Builder subset(PatternSubset subset) {
            subsets.add(Objects.requireNonNull(subset, "subset"));
            return this;
        }

        /**
         * Appends a subset definition using string identifiers.
         *
         * @param name subset name
         * @param variables primary variables in the subset
         * @return this builder
         */
        public Builder subset(String name, String... variables) {
            return subset(PatternSubset.of(
                Identifier.of(name),
                Arrays.stream(variables).map(Identifier::of).toList()
            ));
        }

        /**
         * Appends a variable definition.
         *
         * @param definition variable definition
         * @return this builder
         */
        public Builder define(PatternDefinition definition) {
            definitions.add(Objects.requireNonNull(definition, "definition"));
            return this;
        }

        /**
         * Appends a variable definition using a string identifier.
         *
         * @param variable primary pattern variable
         * @param condition variable predicate
         * @return this builder
         */
        public Builder define(String variable, Predicate condition) {
            return define(PatternDefinition.of(Identifier.of(variable), condition));
        }

        /**
         * Sets the optional result alias.
         *
         * @param alias result alias, or {@code null}
         * @return this builder
         */
        public Builder as(String alias) {
            return as(alias == null ? null : Identifier.of(alias));
        }

        /**
         * Sets the optional quote-aware result alias.
         *
         * @param alias result alias, or {@code null}
         * @return this builder
         */
        public Builder as(Identifier alias) {
            this.alias = alias;
            return this;
        }

        /**
         * Builds an immutable relation using semantic defaults for omitted options.
         *
         * @return immutable pattern-recognition relation
         */
        public PatternRecognitionTable build() {
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(pattern, "pattern");
            if (definitions.isEmpty()) {
                throw new IllegalStateException("Pattern recognition requires at least one definition");
            }
            var effectiveRowsPerMatch = rowsPerMatch == null
                ? RowsPerMatch.of(RowsPerMatch.Mode.ONE, RowsPerMatch.EmptyMatchHandling.DEFAULT)
                : rowsPerMatch;
            var effectiveAfterMatchSkip = afterMatchSkip == null
                ? AfterMatchSkip.of(AfterMatchSkip.Kind.PAST_LAST_ROW, AfterMatchSkip.Position.DEFAULT, null)
                : afterMatchSkip;
            return PatternRecognitionTable.of(
                source,
                partitionBy,
                orderBy,
                List.copyOf(measures),
                effectiveRowsPerMatch,
                effectiveAfterMatchSkip,
                pattern,
                List.copyOf(subsets),
                List.copyOf(definitions),
                alias
            );
        }
    }

    /**
     * Immutable pattern-recognition relation implementation.
     *
     * @param source source relation
     * @param partitionBy optional partition specification
     * @param orderBy optional ordering specification
     * @param measures measure definitions
     * @param rowsPerMatch output-cardinality behavior
     * @param afterMatchSkip after-match resume behavior
     * @param pattern typed match pattern
     * @param subsets subset definitions
     * @param definitions non-empty variable definitions
     * @param alias optional result alias
     */
    record Impl(
        TableRef source,
        PartitionBy partitionBy,
        OrderBy orderBy,
        List<PatternMeasure> measures,
        RowsPerMatch rowsPerMatch,
        AfterMatchSkip afterMatchSkip,
        MatchPattern pattern,
        List<PatternSubset> subsets,
        List<PatternDefinition> definitions,
        Identifier alias
    ) implements PatternRecognitionTable {
        /**
         * Validates and defensively copies the relation state.
         *
         * @param source source relation
         * @param partitionBy optional partition specification
         * @param orderBy optional ordering specification
         * @param measures measure definitions
         * @param rowsPerMatch output-cardinality behavior
         * @param afterMatchSkip after-match resume behavior
         * @param pattern typed match pattern
         * @param subsets subset definitions
         * @param definitions non-empty variable definitions
         * @param alias optional result alias
         */
        public Impl {
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(measures, "measures");
            Objects.requireNonNull(rowsPerMatch, "rowsPerMatch");
            Objects.requireNonNull(afterMatchSkip, "afterMatchSkip");
            Objects.requireNonNull(pattern, "pattern");
            Objects.requireNonNull(subsets, "subsets");
            Objects.requireNonNull(definitions, "definitions");
            if (definitions.isEmpty()) {
                throw new IllegalArgumentException("Pattern recognition requires at least one definition");
            }
            measures = List.copyOf(measures);
            subsets = List.copyOf(subsets);
            definitions = List.copyOf(definitions);
        }
    }
}
