package io.sqm.core;

/**
 * Wait behavior for a row-locking clause.
 */
public enum LockWaitMode {
    /**
     * Use the dialect's default blocking behavior.
     */
    DEFAULT,
    /**
     * Fail immediately when a requested lock cannot be acquired.
     */
    NOWAIT,
    /**
     * Skip rows that cannot be locked immediately.
     */
    SKIP_LOCKED,
    /**
     * Wait for a bounded amount of time before failing.
     */
    WAIT
}
