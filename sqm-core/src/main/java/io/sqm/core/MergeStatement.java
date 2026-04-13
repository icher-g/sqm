package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Models a dialect-neutral {@code MERGE} statement.
 */
public non-sealed interface MergeStatement extends Statement {

    /**
     * Creates an immutable merge statement.
     *
     * @param target merge target table
     * @param source merge source relation
     * @param on merge join predicate
     * @param topSpec optional top specification
     * @param clauses merge clauses
     * @param result optional result clause
     * @param hints typed statement hints
     * @return immutable merge statement
     */
    static MergeStatement of(Table target, TableRef source, Predicate on, TopSpec topSpec, List<MergeClause> clauses, ResultClause result, List<StatementHint> hints) {
        return new Impl(target, source, on, topSpec, clauses, result, hints);
    }

    /**
     * Creates a mutable builder for constructing immutable merge statements.
     *
     * @param target merge target table
     * @return builder initialized with the target table
     */
    static Builder builder(Table target) {
        return new Builder(target);
    }

    /**
     * Creates a mutable builder initialized from an existing immutable merge statement.
     *
     * @param statement source statement
     * @return builder initialized from the statement
     */
    static Builder builder(MergeStatement statement) {
        return new Builder(statement);
    }

    /**
     * Returns the merge target table.
     *
     * @return target table
     */
    Table target();

    /**
     * Returns the merge source relation.
     *
     * @return source relation
     */
    TableRef source();

    /**
     * Returns the merge match predicate.
     *
     * @return merge join predicate
     */
    Predicate on();

    /**
     * Returns the optional merge top specification.
     *
     * @return top specification or {@code null}
     */
    TopSpec topSpec();

    /**
     * Returns merge clauses in source order.
     *
     * @return immutable clause list
     */
    List<MergeClause> clauses();

    /**
     * Returns the optional result clause.
     *
     * @return result clause or {@code null}
     */
    ResultClause result();

    /**
     * Accepts a {@link NodeVisitor}.
     *
     * @param visitor visitor instance
     * @param <R> result type
     * @return visitor result
     */
    @Override
    default <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitMergeStatement(this);
    }

    /**
     * Mutable builder for constructing immutable {@link MergeStatement} instances.
     */
    final class Builder {
        private final List<MergeClause> clauses = new ArrayList<>();
        private Table target;
        private TableRef source;
        private Predicate on;
        private TopSpec topSpec;
        private ResultClause result;
        private final List<StatementHint> hints = new ArrayList<>();

        /**
         * Creates a builder initialized with a target table.
         *
         * @param target merge target table
         */
        private Builder(Table target) {
            this.target = Objects.requireNonNull(target, "target");
        }

        /**
         * Creates a builder initialized from an existing immutable merge statement.
         *
         * @param statement source statement
         */
        private Builder(MergeStatement statement) {
            Objects.requireNonNull(statement, "statement");
            this.target = statement.target();
            this.source = statement.source();
            this.on = statement.on();
            this.topSpec = statement.topSpec();
            this.clauses.addAll(statement.clauses());
            this.result = statement.result();
            this.hints.addAll(statement.hints());
        }

        /**
         * Sets the merge target table.
         *
         * @param target merge target table
         * @return this builder
         */
        public Builder target(Table target) {
            this.target = Objects.requireNonNull(target, "target");
            return this;
        }

        /**
         * Sets the merge source relation.
         *
         * @param source merge source relation
         * @return this builder
         */
        public Builder source(TableRef source) {
            this.source = Objects.requireNonNull(source, "source");
            return this;
        }

        /**
         * Sets the merge match predicate.
         *
         * @param on merge predicate
         * @return this builder
         */
        public Builder on(Predicate on) {
            this.on = Objects.requireNonNull(on, "on");
            return this;
        }

        /**
         * Sets the optional merge top specification.
         *
         * @param topSpec top specification or {@code null}
         * @return this builder
         */
        public Builder top(TopSpec topSpec) {
            this.topSpec = topSpec;
            return this;
        }

        /**
         * Sets a plain {@code TOP (<count>)} specification.
         *
         * @param count top-count expression
         * @return this builder
         */
        public Builder top(Expression count) {
            return top(TopSpec.of(count));
        }

        /**
         * Sets a plain {@code TOP (<count>)} specification using an expression.
         *
         * @param count top count expression
         * @param percent whether {@code PERCENT} is present
         * @param withTies whether {@code WITH TIES} is present
         * @return this builder
         */
        public Builder top(Expression count, boolean percent, boolean withTies) {
            return top(TopSpec.of(count, percent, withTies));
        }

        /**
         * Sets a plain {@code TOP (<count>)} specification.
         *
         * @param count top-count literal value
         * @return this builder
         */
        public Builder top(long count) {
            return top(Expression.literal(count));
        }

        /**
         * Replaces typed statement hints attached to this statement.
         *
         * @param hints typed statement hints
         * @return this builder
         */
        public Builder hints(List<StatementHint> hints) {
            Objects.requireNonNull(hints, "hints");
            this.hints.clear();
            this.hints.addAll(hints);
            return this;
        }

        /**
         * Appends one typed statement hint.
         *
         * @param hint typed statement hint
         * @return this builder
         */
        public Builder hint(StatementHint hint) {
            this.hints.add(Objects.requireNonNull(hint, "hint"));
            return this;
        }

        /**
         * Appends one typed statement hint using convenience arguments.
         *
         * @param name hint name
         * @param args convenience hint arguments
         * @return this builder
         */
        public Builder hint(String name, Object... args) {
            return hint(StatementHint.of(name, args));
        }

        /**
         * Clears typed statement hints attached to this statement.
         *
         * @return this builder
         */
        public Builder clearHints() {
            this.hints.clear();
            return this;
        }

        /**
         * Appends a raw merge clause.
         *
         * @param clause merge clause
         * @return this builder
         */
        public Builder clause(MergeClause clause) {
            this.clauses.add(Objects.requireNonNull(clause, "clause"));
            return this;
        }

        /**
         * Appends a {@code WHEN MATCHED THEN UPDATE SET ...} clause.
         *
         * @param assignments update assignments
         * @return this builder
         */
        public Builder whenMatchedUpdate(List<Assignment> assignments) {
            return whenMatchedUpdate(null, assignments);
        }

        /**
         * Appends a {@code WHEN MATCHED AND ... THEN UPDATE SET ...} clause.
         *
         * @param condition optional clause predicate
         * @param assignments update assignments
         * @return this builder
         */
        public Builder whenMatchedUpdate(Predicate condition, List<Assignment> assignments) {
            return clause(MergeClause.of(MergeClause.MatchType.MATCHED, condition, MergeUpdateAction.of(assignments)));
        }

        /**
         * Appends a {@code WHEN MATCHED THEN UPDATE SET ...} clause.
         *
         * @param assignments update assignments
         * @return this builder
         */
        public Builder whenMatchedUpdate(Assignment... assignments) {
            Objects.requireNonNull(assignments, "assignments");
            return whenMatchedUpdate(List.of(assignments));
        }

        /**
         * Appends a {@code WHEN MATCHED AND ... THEN UPDATE SET ...} clause.
         *
         * @param condition optional clause predicate
         * @param assignments update assignments
         * @return this builder
         */
        public Builder whenMatchedUpdate(Predicate condition, Assignment... assignments) {
            Objects.requireNonNull(assignments, "assignments");
            return whenMatchedUpdate(condition, List.of(assignments));
        }

        /**
         * Appends a {@code WHEN MATCHED THEN DELETE} clause.
         *
         * @return this builder
         */
        public Builder whenMatchedDelete() {
            return whenMatchedDelete(null);
        }

        /**
         * Appends a {@code WHEN MATCHED AND ... THEN DELETE} clause.
         *
         * @param condition optional clause predicate
         * @return this builder
         */
        public Builder whenMatchedDelete(Predicate condition) {
            return clause(MergeClause.of(MergeClause.MatchType.MATCHED, condition, MergeDeleteAction.of()));
        }

        /**
         * Appends a {@code WHEN MATCHED THEN DO NOTHING} clause.
         *
         * @return this builder
         */
        public Builder whenMatchedDoNothing() {
            return whenMatchedDoNothing(null);
        }

        /**
         * Appends a {@code WHEN MATCHED AND ... THEN DO NOTHING} clause.
         *
         * @param condition optional clause predicate
         * @return this builder
         */
        public Builder whenMatchedDoNothing(Predicate condition) {
            return clause(MergeClause.of(MergeClause.MatchType.MATCHED, condition, MergeDoNothingAction.of()));
        }

        /**
         * Appends a {@code WHEN NOT MATCHED THEN INSERT ... VALUES (...)} clause.
         *
         * @param columns target columns, or empty when omitted
         * @param values inserted values row
         * @return this builder
         */
        public Builder whenNotMatchedInsert(List<Identifier> columns, RowExpr values) {
            return whenNotMatchedInsert(null, columns, values);
        }

        /**
         * Appends a {@code WHEN NOT MATCHED AND ... THEN INSERT ... VALUES (...)} clause.
         *
         * @param condition optional clause predicate
         * @param columns target columns, or empty when omitted
         * @param values inserted values row
         * @return this builder
         */
        public Builder whenNotMatchedInsert(Predicate condition, List<Identifier> columns, RowExpr values) {
            return clause(MergeClause.of(MergeClause.MatchType.NOT_MATCHED, condition, MergeInsertAction.of(columns, values)));
        }

        /**
         * Appends a {@code WHEN NOT MATCHED THEN INSERT ... VALUES (...)} clause.
         *
         * @param values inserted values row
         * @return this builder
         */
        public Builder whenNotMatchedInsert(RowExpr values) {
            return whenNotMatchedInsert(null, List.of(), values);
        }

        /**
         * Appends a {@code WHEN NOT MATCHED AND ... THEN INSERT ... VALUES (...)} clause.
         *
         * @param condition optional clause predicate
         * @param values inserted values row
         * @return this builder
         */
        public Builder whenNotMatchedInsert(Predicate condition, RowExpr values) {
            return whenNotMatchedInsert(condition, List.of(), values);
        }

        /**
         * Appends a {@code WHEN NOT MATCHED THEN DO NOTHING} clause.
         *
         * @return this builder
         */
        public Builder whenNotMatchedDoNothing() {
            return whenNotMatchedDoNothing(null);
        }

        /**
         * Appends a {@code WHEN NOT MATCHED AND ... THEN DO NOTHING} clause.
         *
         * @param condition optional clause predicate
         * @return this builder
         */
        public Builder whenNotMatchedDoNothing(Predicate condition) {
            return clause(MergeClause.of(MergeClause.MatchType.NOT_MATCHED, condition, MergeDoNothingAction.of()));
        }

        /**
         * Appends a {@code WHEN NOT MATCHED THEN INSERT ... VALUES (...)} clause.
         *
         * @param columns target columns
         * @param values inserted value expressions
         * @return this builder
         */
        public Builder whenNotMatchedInsert(List<Identifier> columns, List<Expression> values) {
            Objects.requireNonNull(values, "values");
            return whenNotMatchedInsert(columns, RowExpr.of(values));
        }

        /**
         * Appends a {@code WHEN NOT MATCHED AND ... THEN INSERT ... VALUES (...)} clause.
         *
         * @param condition optional clause predicate
         * @param columns target columns
         * @param values inserted value expressions
         * @return this builder
         */
        public Builder whenNotMatchedInsert(Predicate condition, List<Identifier> columns, List<Expression> values) {
            Objects.requireNonNull(values, "values");
            return whenNotMatchedInsert(condition, columns, RowExpr.of(values));
        }

        /**
         * Appends a {@code WHEN NOT MATCHED BY SOURCE THEN UPDATE SET ...} clause.
         *
         * @param assignments update assignments
         * @return this builder
         */
        public Builder whenNotMatchedBySourceUpdate(List<Assignment> assignments) {
            return whenNotMatchedBySourceUpdate(null, assignments);
        }

        /**
         * Appends a {@code WHEN NOT MATCHED BY SOURCE AND ... THEN UPDATE SET ...} clause.
         *
         * @param condition optional clause predicate
         * @param assignments update assignments
         * @return this builder
         */
        public Builder whenNotMatchedBySourceUpdate(Predicate condition, List<Assignment> assignments) {
            return clause(MergeClause.of(MergeClause.MatchType.NOT_MATCHED_BY_SOURCE, condition, MergeUpdateAction.of(assignments)));
        }

        /**
         * Appends a {@code WHEN NOT MATCHED BY SOURCE THEN UPDATE SET ...} clause.
         *
         * @param assignments update assignments
         * @return this builder
         */
        public Builder whenNotMatchedBySourceUpdate(Assignment... assignments) {
            Objects.requireNonNull(assignments, "assignments");
            return whenNotMatchedBySourceUpdate(List.of(assignments));
        }

        /**
         * Appends a {@code WHEN NOT MATCHED BY SOURCE AND ... THEN UPDATE SET ...} clause.
         *
         * @param condition optional clause predicate
         * @param assignments update assignments
         * @return this builder
         */
        public Builder whenNotMatchedBySourceUpdate(Predicate condition, Assignment... assignments) {
            Objects.requireNonNull(assignments, "assignments");
            return whenNotMatchedBySourceUpdate(condition, List.of(assignments));
        }

        /**
         * Appends a {@code WHEN NOT MATCHED BY SOURCE THEN DELETE} clause.
         *
         * @return this builder
         */
        public Builder whenNotMatchedBySourceDelete() {
            return whenNotMatchedBySourceDelete(null);
        }

        /**
         * Appends a {@code WHEN NOT MATCHED BY SOURCE AND ... THEN DELETE} clause.
         *
         * @param condition optional clause predicate
         * @return this builder
         */
        public Builder whenNotMatchedBySourceDelete(Predicate condition) {
            return clause(MergeClause.of(MergeClause.MatchType.NOT_MATCHED_BY_SOURCE, condition, MergeDeleteAction.of()));
        }

        /**
         * Appends a {@code WHEN NOT MATCHED BY SOURCE THEN DO NOTHING} clause.
         *
         * @return this builder
         */
        public Builder whenNotMatchedBySourceDoNothing() {
            return whenNotMatchedBySourceDoNothing(null);
        }

        /**
         * Appends a {@code WHEN NOT MATCHED BY SOURCE AND ... THEN DO NOTHING} clause.
         *
         * @param condition optional clause predicate
         * @return this builder
         */
        public Builder whenNotMatchedBySourceDoNothing(Predicate condition) {
            return clause(MergeClause.of(MergeClause.MatchType.NOT_MATCHED_BY_SOURCE, condition, MergeDoNothingAction.of()));
        }

        /**
         * Sets the optional result clause.
         *
         * @param result result clause or {@code null}
         * @return this builder
         */
        public Builder result(ResultClause result) {
            this.result = result;
            return this;
        }

        /**
         * Replaces the result projection list.
         *
         * @param items projection list
         * @return this builder
         */
        public Builder result(List<ResultItem> items) {
            Objects.requireNonNull(items, "items");
            return result(ResultClause.of(items));
        }

        /**
         * Adds projection items from expressions or result items.
         *
         * @param nodes nodes accepted in the result clause: OUTPUT/RETURNING
         * @return this builder
         */
        public Builder result(Node... nodes) {
            var items = ResultItem.fromNodes(nodes);
            return result(items);
        }

        /**
         * Adds projection items from expressions or result items.
         *
         * @param into result-into target
         * @param nodes nodes accepted in the result clause: OUTPUT/RETURNING
         * @return this builder
         */
        public Builder result(ResultInto into, Node... nodes) {
            var items = ResultItem.fromNodes(nodes);
            return result(ResultClause.of(items, into));
        }

        /**
         * Builds an immutable merge statement.
         *
         * @return immutable merge statement
         */
        public MergeStatement build() {
            if (target == null) {
                throw new IllegalStateException("target must be set");
            }
            if (source == null) {
                throw new IllegalStateException("source must be set");
            }
            if (on == null) {
                throw new IllegalStateException("on must be set");
            }
            if (clauses.isEmpty()) {
                throw new IllegalStateException("at least one merge clause is required");
            }
            return MergeStatement.of(target, source, on, topSpec, clauses, result, hints);
        }
    }

    /**
     * Default immutable implementation of {@link MergeStatement}.
     *
     * @param target merge target table
     * @param source merge source relation
     * @param on merge predicate
     * @param topSpec optional top specification
     * @param clauses merge clauses
     * @param result optional result clause
     * @param hints typed statement hints
     */
    record Impl(Table target,
                TableRef source,
                Predicate on,
                TopSpec topSpec,
                List<MergeClause> clauses,
                ResultClause result,
                List<StatementHint> hints) implements MergeStatement {
        /**
         * Creates an immutable merge statement implementation.
         */
        public Impl {
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(on, "on");
            clauses = List.copyOf(Objects.requireNonNull(clauses, "clauses"));
            hints = hints == null ? List.of() : List.copyOf(hints);
            if (clauses.isEmpty()) {
                throw new IllegalArgumentException("clauses must not be empty");
            }
        }
    }
}
