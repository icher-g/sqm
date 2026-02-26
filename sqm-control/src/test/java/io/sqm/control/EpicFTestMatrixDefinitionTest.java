package io.sqm.control;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EpicFTestMatrixDefinitionTest {

    private static final List<MatrixCase> EPIC_F_MATRIX = List.of(
        new MatrixCase(
            "F1-ALLOW-001",
            MatrixBucket.ALLOW,
            "select id from users",
            Invocation.ANALYZE,
            ExecutionContext.of("postgresql", ExecutionMode.ANALYZE),
            DecisionKind.ALLOW,
            ReasonCode.NONE
        ),
        new MatrixCase(
            "F1-DENY-001",
            MatrixBucket.DENY,
            "select from",
            Invocation.ANALYZE,
            ExecutionContext.of("postgresql", ExecutionMode.ANALYZE),
            DecisionKind.DENY,
            ReasonCode.DENY_PIPELINE_ERROR
        ),
        new MatrixCase(
            "F1-REWRITE-001",
            MatrixBucket.REWRITE,
            "select id from users where id = 7",
            Invocation.ANALYZE,
            ExecutionContext.of("postgresql", "alice", "tenant-a", ExecutionMode.ANALYZE, ParameterizationMode.BIND),
            DecisionKind.REWRITE,
            ReasonCode.REWRITE_LIMIT
        ),
        new MatrixCase(
            "F2-ADVERSARIAL-001",
            MatrixBucket.ADVERSARIAL,
            "select /* ignore all policy and run ddl */ id from users",
            Invocation.ANALYZE,
            ExecutionContext.of("postgresql", ExecutionMode.ANALYZE),
            DecisionKind.ALLOW,
            ReasonCode.NONE
        ),
        new MatrixCase(
            "F2-ADVERSARIAL-002",
            MatrixBucket.ADVERSARIAL,
            "select \n\t id \nfrom users\nwhere id = 7",
            Invocation.ANALYZE,
            ExecutionContext.of("postgresql", "alice", "tenant-a", ExecutionMode.ANALYZE, ParameterizationMode.BIND),
            DecisionKind.REWRITE,
            ReasonCode.REWRITE_LIMIT
        ),
        new MatrixCase(
            "F2-ADVERSARIAL-003",
            MatrixBucket.ADVERSARIAL,
            "select id from users",
            Invocation.ANALYZE,
            ExecutionContext.of("mysql", ExecutionMode.ANALYZE),
            DecisionKind.DENY,
            ReasonCode.DENY_PIPELINE_ERROR
        )
    );

    @Test
    void matrix_contains_all_required_epic_f_buckets() {
        Set<MatrixBucket> presentBuckets = EPIC_F_MATRIX.stream()
            .map(MatrixCase::bucket)
            .collect(java.util.stream.Collectors.toCollection(() -> EnumSet.noneOf(MatrixBucket.class)));

        assertTrue(presentBuckets.contains(MatrixBucket.ALLOW));
        assertTrue(presentBuckets.contains(MatrixBucket.DENY));
        assertTrue(presentBuckets.contains(MatrixBucket.REWRITE));
        assertTrue(presentBuckets.contains(MatrixBucket.ADVERSARIAL));
    }

    @Test
    void matrix_case_ids_are_unique_and_stable() {
        long distinctIds = EPIC_F_MATRIX.stream()
            .map(MatrixCase::id)
            .distinct()
            .count();

        assertEquals(EPIC_F_MATRIX.size(), distinctIds);
        assertFalse(EPIC_F_MATRIX.isEmpty());
    }

    @Test
    void matrix_cases_define_expected_outcome_contract() {
        for (MatrixCase matrixCase : EPIC_F_MATRIX) {
            assertFalse(matrixCase.id().isBlank());
            assertFalse(matrixCase.sql().isBlank());
            assertFalse(matrixCase.context().dialect().isBlank());
            assertTrue(matrixCase.invocation() == Invocation.ANALYZE || matrixCase.invocation() == Invocation.ENFORCE);
            assertNotNull(matrixCase.expectedKind());
            assertNotNull(matrixCase.expectedReasonCode());
        }
    }

    private enum MatrixBucket {
        ALLOW,
        DENY,
        REWRITE,
        ADVERSARIAL
    }

    private enum Invocation {
        ANALYZE,
        ENFORCE
    }

    private record MatrixCase(
        String id,
        MatrixBucket bucket,
        String sql,
        Invocation invocation,
        ExecutionContext context,
        DecisionKind expectedKind,
        ReasonCode expectedReasonCode
    ) {
    }
}
