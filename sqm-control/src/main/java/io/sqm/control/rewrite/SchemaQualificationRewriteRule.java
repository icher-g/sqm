package io.sqm.control.rewrite;

import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.control.ExecutionContext;
import io.sqm.control.QueryRewriteResult;
import io.sqm.control.QueryRewriteRule;
import io.sqm.control.ReasonCode;
import io.sqm.core.Query;
import io.sqm.core.transform.QueryFingerprint;
import io.sqm.core.transform.SchemaQualificationTransformer;
import io.sqm.core.transform.TableQualification;
import io.sqm.core.transform.TableSchemaResolver;

import java.util.Objects;

/**
 * Middleware rewrite rule that schema-qualifies unqualified tables using catalog metadata.
 */
public final class SchemaQualificationRewriteRule implements QueryRewriteRule {
    private static final String RULE_ID = "schema-qualification";

    private final SchemaQualificationTransformer transformer;

    private SchemaQualificationRewriteRule(SchemaQualificationTransformer transformer) {
        this.transformer = transformer;
    }

    /**
     * Creates a schema-qualification rewrite rule backed by catalog schema metadata.
     *
     * @param schema catalog schema used for unqualified table resolution
     * @return rule instance
     */
    public static SchemaQualificationRewriteRule of(CatalogSchema schema) {
        Objects.requireNonNull(schema, "schema must not be null");
        return new SchemaQualificationRewriteRule(SchemaQualificationTransformer.of(catalogResolver(schema)));
    }

    /**
     * Returns a stable rule identifier.
     *
     * @return rule identifier
     */
    @Override
    public String id() {
        return RULE_ID;
    }

    /**
     * Applies schema qualification and reports a rewrite only when the AST actually changes.
     *
     * @param query parsed query model
     * @param context execution context
     * @return rewrite result
     */
    @Override
    public QueryRewriteResult apply(Query query, ExecutionContext context) {
        Objects.requireNonNull(query, "query must not be null");
        Objects.requireNonNull(context, "context must not be null");

        String before = QueryFingerprint.of(query, false);
        Query transformed = transformer.apply(query);
        String after = QueryFingerprint.of(transformed, false);

        if (before.equals(after)) {
            return QueryRewriteResult.unchanged(transformed);
        }
        return QueryRewriteResult.rewritten(transformed, id(), ReasonCode.REWRITE_QUALIFICATION);
    }

    private static TableSchemaResolver catalogResolver(CatalogSchema schema) {
        return tableName -> {
            var result = schema.resolve(null, tableName);
            return switch (result) {
                case CatalogSchema.TableLookupResult.Found found -> toQualification(found.table());
                case CatalogSchema.TableLookupResult.Ambiguous ignored -> TableQualification.ambiguous();
                case CatalogSchema.TableLookupResult.NotFound ignored -> TableQualification.unresolved();
            };
        };
    }

    private static TableQualification toQualification(CatalogTable table) {
        if (table.schema() == null || table.schema().isBlank()) {
            return TableQualification.unresolved();
        }
        return TableQualification.qualified(table.schema());
    }
}
