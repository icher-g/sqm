package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.List;

/**
 * Represents a SELECT locking clause.
 *
 * <p>A locking clause requests row-level locks to be acquired when executing
 * the SELECT statement. It is commonly used to coordinate concurrent workers
 * and to prevent conflicting updates.</p>
 *
 * <p>Not all SQL dialects support all lock modes or modifiers. Dialect-specific
 * parsers, renderers, and validators must decide which combinations are
 * supported.</p>
 *
 * <p>Examples (dialect-dependent):</p>
 * <pre>
 * FOR UPDATE
 * FOR SHARE
 * FOR UPDATE OF t1, t2
 * FOR UPDATE NOWAIT
 * FOR UPDATE WAIT 5
 * FOR UPDATE SKIP LOCKED
 * </pre>
 */
public non-sealed interface LockingClause extends Node {

    /**
     * Creates a locking clause instance.
     *
     * @param mode       lock mode
     * @param ofTables   lock targets specified in FOR ... OF clause, or empty for all tables
     * @param nowait     whether NOWAIT is specified
     * @param skipLocked whether SKIP LOCKED is specified
     * @return locking clause
     */
    static LockingClause of(
        LockMode mode,
        List<LockTarget> ofTables,
        boolean nowait,
        boolean skipLocked
    ) {
        if (nowait && skipLocked) {
            throw new IllegalArgumentException("NOWAIT and SKIP LOCKED are mutually exclusive");
        }
        return new Impl(
            mode,
            ofTables,
            nowait ? LockWaitMode.NOWAIT : skipLocked ? LockWaitMode.SKIP_LOCKED : LockWaitMode.DEFAULT,
            null
        );
    }

    /**
     * Creates a locking clause instance with explicit wait behavior.
     *
     * @param mode        lock mode
     * @param ofTables    lock targets specified in FOR ... OF clause, or empty for all tables
     * @param waitMode    wait behavior
     * @param waitSeconds wait timeout expression; required only for {@link LockWaitMode#WAIT}
     * @return locking clause
     */
    static LockingClause of(
        LockMode mode,
        List<LockTarget> ofTables,
        LockWaitMode waitMode,
        Expression waitSeconds
    ) {
        return new Impl(mode, ofTables, waitMode, waitSeconds);
    }

    /**
     * Lock mode used by the clause.
     *
     * @return lock mode
     */
    LockMode mode();

    /**
     * Tables affected by the lock as specified in FOR ... OF clause.
     *
     * <p>An empty list means that the lock applies to all tables referenced
     * by the SELECT statement.</p>
     *
     * @return list of lock targets
     */
    List<LockTarget> ofTables();

    /**
     * Indicates whether the NOWAIT modifier is specified.
     *
     * <p>If true, the query fails immediately if the required locks
     * cannot be acquired.</p>
     *
     * @return true if NOWAIT is used
     */
    default boolean nowait() {
        return waitMode() == LockWaitMode.NOWAIT;
    }

    /**
     * Indicates whether the SKIP LOCKED modifier is specified.
     *
     * <p>If true, rows that cannot be locked are skipped instead of
     * blocking the query.</p>
     *
     * @return true if SKIP LOCKED is used
     */
    default boolean skipLocked() {
        return waitMode() == LockWaitMode.SKIP_LOCKED;
    }

    /**
     * Returns the configured lock wait behavior.
     *
     * @return wait behavior
     */
    LockWaitMode waitMode();

    /**
     * Returns the wait timeout expression.
     *
     * @return wait timeout expression or {@code null} unless {@link #waitMode()} is {@link LockWaitMode#WAIT}
     */
    Expression waitSeconds();

    /**
     * Accepts a {@link NodeVisitor} and dispatches control to the
     * visitor method corresponding to the concrete subtype
     *
     * @param v   the visitor instance to accept (must not be {@code null})
     * @param <R> the result type returned by the visitor
     * @return the result produced by the visitor
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitLockingClause(this);
    }

    /**
     * Default immutable implementation.
     *
     * @param mode       lock mode
     * @param ofTables   lock targets specified in FOR ... OF clause, or empty for all tables
     * @param waitMode    wait behavior
     * @param waitSeconds wait timeout expression
     */
    record Impl(
        LockMode mode,
        List<LockTarget> ofTables,
        LockWaitMode waitMode,
        Expression waitSeconds
    ) implements LockingClause {

        /**
         * Creates a locking clause implementation.
         *
         * @param mode       lock mode
         * @param ofTables   lock targets
         * @param waitMode    wait behavior
         * @param waitSeconds wait timeout expression
         */
        public Impl {
            if (waitMode == null) {
                waitMode = LockWaitMode.DEFAULT;
            }
            if (waitMode == LockWaitMode.WAIT && waitSeconds == null) {
                throw new IllegalArgumentException("WAIT requires a timeout expression");
            }
            if (waitMode != LockWaitMode.WAIT && waitSeconds != null) {
                throw new IllegalArgumentException("waitSeconds is only valid with WAIT");
            }
            ofTables = ofTables == null ? List.of() : List.copyOf(ofTables);
        }
    }
}

