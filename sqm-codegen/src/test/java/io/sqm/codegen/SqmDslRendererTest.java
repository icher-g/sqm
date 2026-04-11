package io.sqm.codegen;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static io.sqm.dsl.Dsl.param;
import static io.sqm.dsl.Dsl.select;
import static io.sqm.dsl.Dsl.star;
import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqmDslRendererTest {

    @Test
    void renderEmitsStatementMethodAndNamedParams() {
        var statement = select(star())
            .from(tbl("users"))
            .where(param("status").isNotNull())
            .build();

        var group = new SqlFolderGroup(
            Path.of("user"),
            "UserQueries",
            List.of(new SqlSourceFile(
                Path.of("user", "find_active.sql"),
                Path.of("user"),
                "findActive",
                statement,
                Set.of("status"),
                "hash-1"
            ))
        );

        var options = SqlFileCodegenOptions.of(
            Path.of("sql"),
            Path.of("generated"),
            "io.sqm.codegen.generated",
            SqlCodegenDialect.ANSI,
            false,
            false,
            null,
            true
        );

        var source = new SqmDslRenderer(options).render(group);

        assertTrue(source.contains("package io.sqm.codegen.generated;"));
        assertTrue(source.contains("import static io.sqm.dsl.Dsl.*;"));
        assertTrue(source.contains("public final class UserQueries"));
        assertTrue(source.contains("public static SelectQuery findActive()"));
        assertTrue(source.contains("Gets a generated statement."));
        assertTrue(source.contains("param(\"status\")"));
        assertTrue(source.contains("public static Set<String> findActiveParams()"));
        assertTrue(source.contains("return Set.of(\"status\")"));
        assertFalse(source.contains("@Generated("));
    }
}
