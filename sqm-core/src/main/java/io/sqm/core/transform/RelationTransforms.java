package io.sqm.core.transform;

import io.sqm.core.FunctionTable;
import io.sqm.core.Identifier;
import io.sqm.core.Lateral;
import io.sqm.core.Node;
import io.sqm.core.Query;
import io.sqm.core.QueryTable;
import io.sqm.core.Table;
import io.sqm.core.TableRef;
import io.sqm.core.ValuesTable;
import io.sqm.core.VariableTableRef;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Task-oriented helpers for relation-focused tree rewrites.
 * <p>
 * These utilities wrap the standard recursive transformer behavior so callers
 * can express table-reference rewrites directly.
 */
public final class RelationTransforms {
    private RelationTransforms() {
    }

    /**
     * Rewrites every visited {@link TableRef} using the provided mapping function.
     *
     * @param node     root node to rewrite
     * @param rewriter mapping function for table references
     * @param <T>      node type
     * @return rewritten node, or the original instance when nothing changes
     */
    public static <T extends Node> T rewriteTableRefs(T node, Function<TableRef, TableRef> rewriter) {
        Objects.requireNonNull(rewriter, "rewriter");
        return new RecursiveNodeTransformer() {
            @Override
            public Node visitTable(Table table) {
                return requireTableRefResult(rewriter.apply(table), table);
            }

            @Override
            public Node visitVariableTableRef(VariableTableRef table) {
                return requireTableRefResult(rewriter.apply(table), table);
            }

            @Override
            public Node visitQueryTable(QueryTable table) {
                var transformed = (QueryTable) super.visitQueryTable(table);
                return requireTableRefResult(rewriter.apply(transformed), transformed);
            }

            @Override
            public Node visitValuesTable(ValuesTable table) {
                var transformed = (ValuesTable) super.visitValuesTable(table);
                return requireTableRefResult(rewriter.apply(transformed), transformed);
            }

            @Override
            public Node visitFunctionTable(FunctionTable table) {
                var transformed = (FunctionTable) super.visitFunctionTable(table);
                return requireTableRefResult(rewriter.apply(transformed), transformed);
            }

            @Override
            public Node visitLateral(Lateral lateral) {
                var transformed = (Lateral) super.visitLateral(lateral);
                return requireTableRefResult(rewriter.apply(transformed), transformed);
            }
        }.apply(node);
    }

    /**
     * Rewrites every visited {@link Table} using the provided mapping function.
     *
     * @param node     root node to rewrite
     * @param rewriter mapping function for tables
     * @param <T>      node type
     * @return rewritten node, or the original instance when nothing changes
     */
    public static <T extends Node> T rewriteTables(T node, Function<Table, Table> rewriter) {
        Objects.requireNonNull(rewriter, "rewriter");
        return rewriteTableRefs(node, tableRef -> tableRef.<TableRef>matchTableRef()
            .table(table -> requireTableResult(rewriter.apply(table), table))
            .otherwise(ref -> ref)
        );
    }

    /**
     * Qualifies every unqualified table in a query with the same schema.
     * <p>
     * This is intended for runtime environment rewrites where a query is authored
     * against logical table names during development and later bound to a concrete
     * customer schema before execution.
     *
     * @param query  query to qualify
     * @param schema schema identifier to apply to unqualified tables
     * @return transformed query, or the original instance when nothing changes
     */
    public static Query qualifyUnqualifiedTables(Query query, Identifier schema) {
        Objects.requireNonNull(query, "query");
        Objects.requireNonNull(schema, "schema");
        return SchemaQualificationTransformer.of(tableName -> TableQualification.qualified(schema)).apply(query);
    }

    /**
     * Qualifies every unqualified table in a query with the same schema.
     *
     * @param query  query to qualify
     * @param schema schema name to apply to unqualified tables
     * @return transformed query, or the original instance when nothing changes
     */
    public static Query qualifyUnqualifiedTables(Query query, String schema) {
        return qualifyUnqualifiedTables(query, Identifier.of(Objects.requireNonNull(schema, "schema")));
    }

    /**
     * Remaps table names across a node tree while preserving schema, alias, inheritance, and hints.
     *
     * @param node       root node to rewrite
     * @param nameMapper mapping function for table identifiers
     * @param <T>        node type
     * @return rewritten node, or the original instance when nothing changes
     */
    public static <T extends Node> T remapTables(T node, Function<Identifier, Identifier> nameMapper) {
        Objects.requireNonNull(nameMapper, "nameMapper");
        return rewriteTables(node, table -> {
            var mapped = Objects.requireNonNullElse(nameMapper.apply(table.name()), table.name());
            if (mapped.equals(table.name())) {
                return table;
            }
            return Table.of(table.schema(), mapped, table.alias(), table.inheritance(), table.hints());
        });
    }

    /**
     * Remaps table names across a node tree using a simple string-to-string mapping.
     *
     * @param node        root node to rewrite
     * @param tableNames  source-to-target table name mapping
     * @param <T>         node type
     * @return rewritten node, or the original instance when nothing changes
     */
    public static <T extends Node> T remapTables(T node, Map<String, String> tableNames) {
        Objects.requireNonNull(tableNames, "tableNames");
        return remapTables(node, name -> {
            var mapped = tableNames.get(name.value());
            return mapped == null ? name : Identifier.of(mapped, name.quoteStyle());
        });
    }

    /**
     * Renames a table wherever it appears while preserving schema, alias, inheritance, and hints.
     *
     * @param node     root node to rewrite
     * @param fromName source table name
     * @param toName   replacement table name
     * @param <T>      node type
     * @return rewritten node, or the original instance when nothing changes
     */
    public static <T extends Node> T renameTable(T node, String fromName, String toName) {
        Objects.requireNonNull(fromName, "fromName");
        return renameTable(node, Identifier.of(fromName), Identifier.of(Objects.requireNonNull(toName, "toName")));
    }

    /**
     * Renames a table wherever it appears while preserving schema, alias, inheritance, and hints.
     *
     * @param node     root node to rewrite
     * @param fromName source table identifier
     * @param toName   replacement table identifier
     * @param <T>      node type
     * @return rewritten node, or the original instance when nothing changes
     */
    public static <T extends Node> T renameTable(T node, Identifier fromName, Identifier toName) {
        Objects.requireNonNull(fromName, "fromName");
        Objects.requireNonNull(toName, "toName");
        return remapTables(node, name -> name.equals(fromName) ? toName : name);
    }

    private static TableRef requireTableRefResult(TableRef rewritten, TableRef original) {
        if (rewritten == null) {
            throw new IllegalArgumentException("TableRef rewriter must not return null for: " + original);
        }
        return rewritten;
    }

    private static Table requireTableResult(Table rewritten, Table original) {
        if (rewritten == null) {
            throw new IllegalArgumentException("Table rewriter must not return null for: " + original);
        }
        return rewritten;
    }
}
