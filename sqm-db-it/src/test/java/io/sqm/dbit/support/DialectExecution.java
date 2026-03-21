package io.sqm.dbit.support;

/**
 * Executes one live-database test case against a dialect harness.
 *
 * @param <H> harness type used by the test case
 */
@FunctionalInterface
public interface DialectExecution<H> {
    /**
     * Runs the live-database test case.
     *
     * @param harness dialect harness to use
     * @throws Exception when setup, rendering, or execution fails
     */
    void run(H harness) throws Exception;
}
