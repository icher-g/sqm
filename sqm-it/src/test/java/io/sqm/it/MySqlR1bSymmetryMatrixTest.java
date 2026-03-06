package io.sqm.it;

import io.sqm.core.Query;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MySqlR1bSymmetryMatrixTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("symmetryCases")
    void parseRenderParse_isStable_forR1bMustFeatures(String feature, String inputSql, String canonicalSql) {
        Query parsed = Utils.parseMySql(inputSql);
        String rendered = Utils.renderMySql(parsed);
        Query reparsed = Utils.parseMySql(rendered);

        assertEquals(Utils.canonicalJson(parsed), Utils.canonicalJson(reparsed), feature);
        assertEquals(canonicalSql, Utils.normalizeSql(rendered), feature);
    }

    private static Stream<Arguments> symmetryCases() {
        return Stream.of(
            Arguments.of(
                "NULL_SAFE_EQUALITY",
                "SELECT id FROM users WHERE deleted_at <=> NULL",
                "SELECT id FROM users WHERE deleted_at <=> NULL"
            ),
            Arguments.of(
                "REGEX_RLIKE_CANONICALIZATION",
                "SELECT id FROM users WHERE name RLIKE '^a'",
                "SELECT id FROM users WHERE name REGEXP '^a'"
            ),
            Arguments.of(
                "LOCKING_FOR_SHARE_SKIP_LOCKED",
                "SELECT id FROM jobs FOR SHARE SKIP LOCKED",
                "SELECT id FROM jobs FOR SHARE SKIP LOCKED"
            ),
            Arguments.of(
                "GROUP_BY_WITH_ROLLUP",
                "SELECT department, status FROM employees GROUP BY department, status WITH ROLLUP",
                "SELECT department, status FROM employees GROUP BY department, status WITH ROLLUP"
            )
        );
    }
}
