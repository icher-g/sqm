package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Models a dialect-neutral {@code INSERT} statement.
 */
public non-sealed interface InsertStatement extends Statement {

    /**
     * Creates an immutable insert statement with optional {@code ON CONFLICT} and {@code RETURNING} clauses.
     *
     * @param insertMode insert mode
     * @param table target table
     * @param columns target columns, or empty when omitted
     * @param source insert source
     * @param conflictTarget conflict target columns, or empty when omitted
     * @param onConflictAction on-conflict action
     * @param conflictUpdateAssignments conflict-update assignments, or empty when omitted
     * @param conflictUpdateWhere optional conflict-update predicate
     * @param returning returning projection list, or empty when omitted
     * @return immutable insert statement
     */
    static InsertStatement of(InsertMode insertMode,
                              Table table,
                              List<Identifier> columns,
                              InsertSource source,
                              List<Identifier> conflictTarget,
                              OnConflictAction onConflictAction,
                              List<Assignment> conflictUpdateAssignments,
                              Predicate conflictUpdateWhere,
                              List<SelectItem> returning) {
        return new Impl(insertMode,
            table,
            columns,
            source,
            conflictTarget,
            onConflictAction,
            conflictUpdateAssignments,
            conflictUpdateWhere,
            returning);
    }

    /**
     * Creates a mutable builder for constructing immutable insert statements.
     *
     * @param table target table
     * @return builder initialized with the target table
     */
    static Builder builder(Table table) {
        return new Builder(table);
    }

    /**
     * Returns the insert target table.
     *
     * @return target table
     */
    Table table();

    /**
     * Returns the insert mode.
     *
     * @return insert mode
     */
    InsertMode insertMode();

    /**
     * Returns explicitly targeted columns.
     *
     * @return immutable target column list
     */
    List<Identifier> columns();

    /**
     * Returns the insert source.
     *
     * @return insert source
     */
    InsertSource source();

    /**
     * Returns optional conflict target columns used in {@code ON CONFLICT (...)}.
     *
     * @return immutable conflict target column list
     */
    List<Identifier> conflictTarget();

    /**
     * Returns optional {@code ON CONFLICT} action.
     *
     * @return on-conflict action
     */
    OnConflictAction onConflictAction();

    /**
     * Returns optional conflict-update assignments used in {@code DO UPDATE SET ...}.
     *
     * @return immutable assignment list
     */
    List<Assignment> conflictUpdateAssignments();

    /**
     * Returns optional conflict-update predicate used in {@code DO UPDATE ... WHERE ...}.
     *
     * @return predicate or {@code null}
     */
    Predicate conflictUpdateWhere();

    /**
     * Returns optional {@code RETURNING} projection items.
     *
     * @return immutable returning item list
     */
    List<SelectItem> returning();

    /**
     * Accepts a {@link NodeVisitor}.
     *
     * @param visitor visitor instance
     * @param <R>     result type
     * @return visitor result
     */
    @Override
    default <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitInsertStatement(this);
    }

    /**
     * Describes optional {@code ON CONFLICT} action.
     */
    enum OnConflictAction {
        /**
         * No {@code ON CONFLICT} clause.
         */
        NONE,
        /**
         * {@code ON CONFLICT ... DO NOTHING}.
         */
        DO_NOTHING,
        /**
         * {@code ON CONFLICT ... DO UPDATE SET ...}.
         */
        DO_UPDATE
    }

    /**
     * Describes the leading keyword variant used by the insert statement.
     */
    enum InsertMode {
        /**
         * Standard {@code INSERT INTO}.
         */
        STANDARD,
        /**
         * MySQL {@code INSERT IGNORE INTO}.
         */
        IGNORE,
        /**
         * MySQL {@code REPLACE INTO}.
         */
        REPLACE
    }

    /**
     * Mutable builder for constructing immutable {@link InsertStatement} instances.
     */
    final class Builder {
        private final List<Identifier> columns = new ArrayList<>();
        private final List<Identifier> conflictTarget = new ArrayList<>();
        private final List<Assignment> conflictUpdateAssignments = new ArrayList<>();
        private final List<SelectItem> returning = new ArrayList<>();
        private InsertMode insertMode = InsertMode.STANDARD;
        private Table table;
        private InsertSource source;
        private OnConflictAction onConflictAction = OnConflictAction.NONE;
        private Predicate conflictUpdateWhere;

        /**
         * Creates a builder initialized with a target table.
         *
         * @param table target table
         */
        private Builder(Table table) {
            this.table = Objects.requireNonNull(table, "table");
        }

        /**
         * Sets the target table.
         *
         * @param table target table
         * @return this builder
         */
        public Builder table(Table table) {
            this.table = Objects.requireNonNull(table, "table");
            return this;
        }

        /**
         * Sets the insert mode.
         *
         * @param insertMode insert mode
         * @return this builder
         */
        public Builder insertMode(InsertMode insertMode) {
            this.insertMode = Objects.requireNonNull(insertMode, "insertMode");
            return this;
        }

        /**
         * Configures standard {@code INSERT INTO} mode.
         *
         * @return this builder
         */
        public Builder standard() {
            return insertMode(InsertMode.STANDARD);
        }

        /**
         * Configures MySQL {@code INSERT IGNORE INTO} mode.
         *
         * @return this builder
         */
        public Builder ignore() {
            return insertMode(InsertMode.IGNORE);
        }

        /**
         * Configures MySQL {@code REPLACE INTO} mode.
         *
         * @return this builder
         */
        public Builder replace() {
            return insertMode(InsertMode.REPLACE);
        }

        /**
         * Replaces the target column list.
         *
         * @param columns target columns
         * @return this builder
         */
        public Builder columns(List<Identifier> columns) {
            Objects.requireNonNull(columns, "columns");
            this.columns.clear();
            this.columns.addAll(columns);
            return this;
        }

        /**
         * Replaces the target column list.
         *
         * @param columns target columns
         * @return this builder
         */
        public Builder columns(Identifier... columns) {
            Objects.requireNonNull(columns, "columns");
            return columns(List.of(columns));
        }

        /**
         * Sets the insert source.
         *
         * @param source insert source
         * @return this builder
         */
        public Builder source(InsertSource source) {
            this.source = Objects.requireNonNull(source, "source");
            return this;
        }

        /**
         * Sets a row-values insert source.
         *
         * @param values row values source
         * @return this builder
         */
        public Builder values(RowValues values) {
            return source(values);
        }

        /**
         * Sets a query insert source.
         *
         * @param query query source
         * @return this builder
         */
        public Builder query(Query query) {
            return source(query);
        }

        /**
         * Configures {@code ON CONFLICT DO NOTHING} without explicit target.
         *
         * @return this builder
         */
        public Builder onConflictDoNothing() {
            this.conflictTarget.clear();
            this.onConflictAction = OnConflictAction.DO_NOTHING;
            this.conflictUpdateAssignments.clear();
            this.conflictUpdateWhere = null;
            return this;
        }

        /**
         * Configures {@code ON CONFLICT (...)} target with {@code DO NOTHING} action.
         *
         * @param target conflict target columns
         * @return this builder
         */
        public Builder onConflictDoNothing(List<Identifier> target) {
            Objects.requireNonNull(target, "target");
            onConflictDoNothing();
            this.conflictTarget.addAll(target);
            return this;
        }

        /**
         * Configures {@code ON CONFLICT (...)} target with {@code DO NOTHING} action.
         *
         * @param target conflict target columns
         * @return this builder
         */
        public Builder onConflictDoNothing(Identifier... target) {
            Objects.requireNonNull(target, "target");
            return onConflictDoNothing(List.of(target));
        }

        /**
         * Configures {@code ON CONFLICT (...) DO UPDATE SET ...}.
         *
         * @param target      conflict target columns
         * @param assignments update assignments
         * @param where       optional predicate
         * @return this builder
         */
        public Builder onConflictDoUpdate(List<Identifier> target, List<Assignment> assignments, Predicate where) {
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(assignments, "assignments");
            this.conflictTarget.clear();
            this.conflictTarget.addAll(target);
            this.onConflictAction = OnConflictAction.DO_UPDATE;
            this.conflictUpdateAssignments.clear();
            this.conflictUpdateAssignments.addAll(assignments);
            this.conflictUpdateWhere = where;
            return this;
        }

        /**
         * Configures {@code ON CONFLICT DO UPDATE SET ...} without explicit target.
         *
         * @param assignments update assignments
         * @return this builder
         */
        public Builder onConflictDoUpdate(List<Assignment> assignments) {
            return onConflictDoUpdate(List.of(), assignments, null);
        }

        /**
         * Configures {@code ON CONFLICT (...) DO UPDATE SET ...} without update predicate.
         *
         * @param target      conflict target columns
         * @param assignments update assignments
         * @return this builder
         */
        public Builder onConflictDoUpdate(List<Identifier> target, List<Assignment> assignments) {
            return onConflictDoUpdate(target, assignments, null);
        }

        /**
         * Configures {@code ON CONFLICT DO UPDATE SET ...} using vararg assignments.
         *
         * @param assignments update assignments
         * @return this builder
         */
        public Builder onConflictDoUpdate(Assignment... assignments) {
            Objects.requireNonNull(assignments, "assignments");
            return onConflictDoUpdate(List.of(assignments));
        }

        /**
         * Replaces the {@code RETURNING} projection list.
         *
         * @param returning returning projection list
         * @return this builder
         */
        public Builder returning(List<SelectItem> returning) {
            Objects.requireNonNull(returning, "returning");
            this.returning.clear();
            this.returning.addAll(returning);
            return this;
        }

        /**
         * Replaces the {@code RETURNING} projection list.
         *
         * @param returning returning projection items
         * @return this builder
         */
        public Builder returning(SelectItem... returning) {
            Objects.requireNonNull(returning, "returning");
            return returning(List.of(returning));
        }

        /**
         * Builds an immutable insert statement.
         *
         * @return immutable insert statement
         */
        public InsertStatement build() {
            if (table == null) {
                throw new IllegalStateException("table must be set");
            }
            if (source == null) {
                throw new IllegalStateException("source must be set");
            }
            return InsertStatement.of(
                insertMode,
                table,
                columns,
                source,
                conflictTarget,
                onConflictAction,
                conflictUpdateAssignments,
                conflictUpdateWhere,
                returning);
        }
    }

    /**
     * Default immutable implementation of {@link InsertStatement}.
     *
     * @param insertMode                insert mode
     * @param table                     target table
     * @param columns                   target columns
     * @param source                    insert source
     * @param conflictTarget            conflict target columns
     * @param onConflictAction          optional on-conflict action
     * @param conflictUpdateAssignments conflict-update assignments
     * @param conflictUpdateWhere       optional conflict-update predicate
     * @param returning                 returning projection list
     */
    record Impl(InsertMode insertMode,
                Table table,
                List<Identifier> columns,
                InsertSource source,
                List<Identifier> conflictTarget,
                OnConflictAction onConflictAction,
                List<Assignment> conflictUpdateAssignments,
                Predicate conflictUpdateWhere,
                List<SelectItem> returning) implements InsertStatement {
        /**
         * Creates an immutable insert statement implementation.
         */
        public Impl {
            insertMode = insertMode == null ? InsertMode.STANDARD : insertMode;
            Objects.requireNonNull(table, "table");
            Objects.requireNonNull(source, "source");
            columns = columns == null ? List.of() : List.copyOf(columns);
            conflictTarget = conflictTarget == null ? List.of() : List.copyOf(conflictTarget);
            onConflictAction = onConflictAction == null ? OnConflictAction.NONE : onConflictAction;
            conflictUpdateAssignments = conflictUpdateAssignments == null ? List.of() : List.copyOf(conflictUpdateAssignments);
            returning = returning == null ? List.of() : List.copyOf(returning);

            if (onConflictAction == OnConflictAction.NONE) {
                if (!conflictTarget.isEmpty() || !conflictUpdateAssignments.isEmpty() || conflictUpdateWhere != null) {
                    throw new IllegalArgumentException("conflict details require ON CONFLICT action");
                }
            }
            if (onConflictAction == OnConflictAction.DO_NOTHING) {
                if (!conflictUpdateAssignments.isEmpty() || conflictUpdateWhere != null) {
                    throw new IllegalArgumentException("DO NOTHING cannot define update assignments or WHERE");
                }
            }
            if (onConflictAction == OnConflictAction.DO_UPDATE && conflictUpdateAssignments.isEmpty()) {
                throw new IllegalArgumentException("DO UPDATE requires at least one assignment");
            }
        }
    }
}
