package io.sqm.codegen;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.hierarchy;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.param;
import static io.sqm.dsl.Dsl.nextValue;
import static io.sqm.dsl.Dsl.orderBy;
import static io.sqm.dsl.Dsl.pivot;
import static io.sqm.dsl.Dsl.pivotMeasure;
import static io.sqm.dsl.Dsl.pivotValue;
import static io.sqm.dsl.Dsl.prior;
import static io.sqm.dsl.Dsl.select;
import static io.sqm.dsl.Dsl.star;
import static io.sqm.dsl.Dsl.tbl;
import static io.sqm.dsl.Dsl.unpivot;
import static io.sqm.dsl.Dsl.unpivotInput;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqmDslRendererTest {

    @Test
    void renderEmitsStatementMethodAndNamedParams() {
        var statement = select(star())
            .from(tbl("users"))
            .where(param("status").isNotNull())
            .build();

        var user = Path.of("user");
        var group = new SqlFolderGroup(
            user,
            "UserQueries",
            List.of(new SqlSourceFile(
                Path.of("user", "find_active.sql"),
                user,
                "findActive",
                Set.of("status"),
                "hash-1",
                List.of(statement)
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

    @Test
    void renderEmitsSequenceValueDslHelpers() {
        var statement = select(nextValue("users_seq")).from(tbl("dual")).build();
        var user = Path.of("sequence");
        var group = new SqlFolderGroup(
            user,
            "SequenceQueries",
            List.of(new SqlSourceFile(
                Path.of("sequence", "next_user_id.sql"),
                user,
                "nextUserId",
                Set.of(),
                "hash-2",
                List.of(statement)
            ))
        );

        var options = SqlFileCodegenOptions.of(
            Path.of("sql"),
            Path.of("generated"),
            "io.sqm.codegen.generated",
            SqlCodegenDialect.ORACLE,
            false,
            false,
            null,
            true
        );

        var source = new SqmDslRenderer(options).render(group);

        assertTrue(source.contains("nextValue(qualify(id(\"users_seq\")))"));
    }

    @Test
    void renderEmitsHierarchicalQueryDslHelpers() {
        var statement = select(star())
            .from(tbl("categories"))
            .hierarchical(hierarchy(col("parent_id").isNull(), prior(col("id")).eq(col("parent_id")), true, orderBy(col("name"))))
            .build();
        var user = Path.of("hierarchy");
        var group = new SqlFolderGroup(
            user,
            "HierarchyQueries",
            List.of(new SqlSourceFile(
                Path.of("hierarchy", "tree.sql"),
                user,
                "tree",
                Set.of(),
                "hash-3",
                List.of(statement)
            ))
        );

        var options = SqlFileCodegenOptions.of(
            Path.of("sql"),
            Path.of("generated"),
            "io.sqm.codegen.generated",
            SqlCodegenDialect.ORACLE,
            false,
            false,
            null,
            true
        );

        var source = new SqmDslRenderer(options).render(group);

        assertTrue(source.contains(".hierarchical("));
        assertTrue(source.contains("hierarchy("));
        assertTrue(source.contains("prior(col(\"id\"))"));
    }

    @Test
    void renderEmitsPivotAndUnpivotDslHelpers() {
        var pivotStatement = select(star())
            .from(pivot(
                tbl("sales"),
                List.of(pivotMeasure(io.sqm.dsl.Dsl.func("sum", col("amount")), "total")),
                col("quarter"),
                pivotValue(lit("Q1"), "q1")))
            .build();
        var unpivotStatement = select(star())
            .from(unpivot(
                tbl("sales"),
                "amount",
                "quarter",
                unpivotInput("q1", lit("Q1"))))
            .build();
        var user = Path.of("pivot");
        var group = new SqlFolderGroup(
            user,
            "PivotQueries",
            List.of(new SqlSourceFile(
                Path.of("pivot", "sales.sql"),
                user,
                "sales",
                Set.of(),
                "hash-4",
                List.of(pivotStatement, unpivotStatement)
            ))
        );

        var options = SqlFileCodegenOptions.of(
            Path.of("sql"),
            Path.of("generated"),
            "io.sqm.codegen.generated",
            SqlCodegenDialect.ORACLE,
            false,
            false,
            null,
            true
        );

        var source = new SqmDslRenderer(options).render(group);

        assertTrue(source.contains("pivot("));
        assertTrue(source.contains("pivotMeasure(func(\"sum\", col(\"amount\")), \"total\")"));
        assertTrue(source.contains("pivotValue(lit(\"Q1\"), \"q1\")"));
        assertTrue(source.contains("unpivot("));
        assertTrue(source.contains("unpivotInput(\"q1\", lit(\"Q1\"))"));
    }
}
