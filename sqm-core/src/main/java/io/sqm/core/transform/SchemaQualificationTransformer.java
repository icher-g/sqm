package io.sqm.core.transform;

import io.sqm.core.*;

import java.util.*;

/**
 * Rewrites unqualified table references into schema-qualified ones using a resolver.
 *
 * <p>The transformer traverses a query tree and rewrites only {@link Table} nodes where
 * {@link Table#schema()} is {@code null}. Resolution is delegated to {@link TableSchemaResolver}.
 * If the resolver marks a table as ambiguous, transformation fails with
 * {@link AmbiguousTableQualificationException}.</p>
 *
 * <p>CTE names are preserved as-is and are never schema-qualified.</p>
 */
public final class SchemaQualificationTransformer extends RecursiveNodeTransformer {
    private final TableSchemaResolver resolver;
    private final Deque<Set<String>> cteScopes = new ArrayDeque<>();

    private SchemaQualificationTransformer(TableSchemaResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * Creates a new schema qualification transformer.
     *
     * @param resolver table schema resolver.
     * @return transformer instance.
     */
    public static SchemaQualificationTransformer of(TableSchemaResolver resolver) {
        return new SchemaQualificationTransformer(Objects.requireNonNull(resolver, "resolver"));
    }

    private static String normalize(Identifier name) {
        return name == null ? "" : name.value().toLowerCase(Locale.ROOT);
    }

    /**
     * Applies schema qualification to a query tree.
     *
     * @param query query to transform.
     * @return transformed query or original instance when no change is needed.
     */
    public Query apply(Query query) {
        return (Query) transform(query);
    }

    @Override
    public Node visitWithQuery(WithQuery q) {
        cteScopes.push(new LinkedHashSet<>());
        try {
            var scope = cteScopes.peek();
            if (q.recursive()) {
                for (var cte : q.ctes()) {
                    scope.add(normalize(cte.name()));
                }
            }
            var changed = false;
            var transformedCtes = new ArrayList<CteDef>(q.ctes().size());
            for (var cte : q.ctes()) {
                var transformed = apply(cte);
                if (transformed != cte) {
                    changed = true;
                }
                transformedCtes.add(transformed);
                if (!q.recursive()) {
                    scope.add(normalize(cte.name()));
                }
            }
            var transformedBody = apply(q.body());
            if (transformedBody != q.body()) {
                changed = true;
            }
            if (!changed) {
                return q;
            }
            return WithQuery.of(transformedCtes, transformedBody, q.recursive());
        } finally {
            cteScopes.pop();
        }
    }

    @Override
    public Node visitTable(Table t) {
        if (t.schema() != null || isVisibleCte(t.name())) {
            return t;
        }
        var resolution = resolver.resolve(t.name().value());
        if (resolution instanceof TableQualification.Unresolved) {
            return t;
        }
        if (resolution instanceof TableQualification.Ambiguous) {
            throw new AmbiguousTableQualificationException(t.name().value());
        }
        var qualified = (TableQualification.Qualified) resolution;
        return t.inSchema(qualified.schema());
    }

    private boolean isVisibleCte(Identifier name) {
        var normalized = normalize(name);
        for (var scope : cteScopes) {
            if (scope.contains(normalized)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Thrown when a table cannot be qualified deterministically because its name is ambiguous.
     */
    public static final class AmbiguousTableQualificationException extends RuntimeException {
        /**
         * Creates exception for ambiguous table name.
         *
         * @param tableName ambiguous table name.
         */
        public AmbiguousTableQualificationException(String tableName) {
            super("Ambiguous unqualified table: " + tableName);
        }
    }
}
