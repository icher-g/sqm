package io.sqm.core.transform;

import io.sqm.core.ColumnExpr;
import io.sqm.core.Identifier;
import io.sqm.core.Node;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Task-oriented helpers for identifier-focused tree rewrites.
 * <p>
 * These utilities keep the standard recursive transformer model visible while
 * removing repeated boilerplate for common identifier changes.
 */
public final class IdentifierTransforms {
    private IdentifierTransforms() {
    }

    /**
     * Rewrites every visited {@link ColumnExpr} using the provided mapping function.
     *
     * @param node     root node to rewrite
     * @param rewriter mapping function for column expressions
     * @param <T>      node type
     * @return rewritten node, or the original instance when nothing changes
     */
    public static <T extends Node> T rewriteColumns(T node, Function<ColumnExpr, ColumnExpr> rewriter) {
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(rewriter, "rewriter");
        return new RecursiveNodeTransformer() {
            @Override
            public Node visitColumnExpr(ColumnExpr column) {
                return requireColumnResult(rewriter.apply(column), column);
            }
        }.apply(node);
    }

    /**
     * Renames a specific column reference everywhere it appears.
     *
     * @param node       root node to rewrite
     * @param tableAlias optional table alias to match; may be {@code null}
     * @param fromName   source column name
     * @param toName     replacement column name
     * @param <T>        node type
     * @return rewritten node, or the original instance when nothing changes
     */
    public static <T extends Node> T renameColumn(T node, String tableAlias, String fromName, String toName) {
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(fromName, "fromName");
        return renameColumn(
            node,
            tableAlias == null ? null : Identifier.of(tableAlias),
            Identifier.of(fromName),
            Identifier.of(Objects.requireNonNull(toName, "toName"))
        );
    }

    /**
     * Renames a specific column reference everywhere it appears preserving identifier metadata.
     *
     * @param node       root node to rewrite
     * @param tableAlias optional table alias to match; may be {@code null}
     * @param fromName   source column identifier
     * @param toName     replacement column identifier
     * @param <T>        node type
     * @return rewritten node, or the original instance when nothing changes
     */
    public static <T extends Node> T renameColumn(T node, Identifier tableAlias, Identifier fromName, Identifier toName) {
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(fromName, "fromName");
        Objects.requireNonNull(toName, "toName");
        return rewriteColumns(node, column -> {
            if (!matches(column.tableAlias(), tableAlias) || !column.name().equals(fromName)) {
                return column;
            }
            return ColumnExpr.of(column.tableAlias(), toName);
        });
    }

    /**
     * Remaps column names using a qualifier-aware mapping function.
     * <p>
     * The mapper receives the visible column qualifier and column name for each
     * visited {@link ColumnExpr}. Returning {@code null} or the original identifier
     * preserves the existing column reference.
     *
     * @param node       root node to rewrite
     * @param nameMapper mapping function for qualifier-aware column renames
     * @param <T>        node type
     * @return rewritten node, or the original instance when nothing changes
     */
    public static <T extends Node> T remapColumns(T node, Function<ColumnRef, Identifier> nameMapper) {
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(nameMapper, "nameMapper");
        return rewriteColumns(node, column -> {
            var mapped = Objects.requireNonNullElse(
                nameMapper.apply(ColumnRef.of(column.tableAlias(), column.name())),
                column.name()
            );
            if (mapped.equals(column.name())) {
                return column;
            }
            return ColumnExpr.of(column.tableAlias(), mapped);
        });
    }

    /**
     * Remaps column names using a qualifier-aware mapping function.
     *
     * @param node       root node to rewrite
     * @param nameMapper mapping function that receives table qualifier and column name
     * @param <T>        node type
     * @return rewritten node, or the original instance when nothing changes
     */
    public static <T extends Node> T remapColumns(T node, BiFunction<Identifier, Identifier, Identifier> nameMapper) {
        Objects.requireNonNull(nameMapper, "nameMapper");
        return remapColumns(node, ref -> nameMapper.apply(ref.tableQualifier(), ref.columnName()));
    }

    private static boolean matches(Identifier actual, Identifier expected) {
        return Objects.equals(actual, expected);
    }

    private static ColumnExpr requireColumnResult(ColumnExpr rewritten, ColumnExpr original) {
        if (rewritten == null) {
            throw new IllegalArgumentException("Column rewriter must not return null for: " + original);
        }
        return rewritten;
    }

    /**
     * Visible column reference metadata exposed to runtime column remapping helpers.
     *
     * @param tableQualifier optional table qualifier or alias
     * @param columnName     referenced column name
     */
    public record ColumnRef(Identifier tableQualifier, Identifier columnName) {
        /**
         * Validates constructor arguments.
         *
         * @param tableQualifier optional table qualifier or alias
         * @param columnName     referenced column name
         */
        public ColumnRef {
            Objects.requireNonNull(columnName, "columnName");
        }

        /**
         * Creates a column reference descriptor.
         *
         * @param tableQualifier optional table qualifier or alias
         * @param columnName     referenced column name
         * @return column reference descriptor
         */
        public static ColumnRef of(Identifier tableQualifier, Identifier columnName) {
            return new ColumnRef(tableQualifier, columnName);
        }
    }
}
