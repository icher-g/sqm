package io.sqm.control.rewrite;

import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.control.*;
import io.sqm.core.Query;
import io.sqm.core.transform.ColumnQualification;
import io.sqm.core.transform.ColumnQualificationResolver;
import io.sqm.core.transform.ColumnQualificationTransformer;
import io.sqm.core.transform.VisibleTableBinding;

import java.util.Objects;

/**
 * Middleware rewrite rule that qualifies unqualified columns using catalog metadata and visible table bindings.
 */
public final class ColumnQualificationRewriteRule implements QueryRewriteRule {
    private static final String RULE_ID = "column-qualification";

    private final ColumnQualificationTransformer transformer;

    private ColumnQualificationRewriteRule(ColumnQualificationTransformer transformer) {
        this.transformer = transformer;
    }

    /**
     * Creates a column-qualification rewrite rule backed by catalog schema metadata and qualification policy settings.
     *
     * @param schema   catalog schema used for resolution
     * @param settings built-in rewrite settings (qualification defaults/strictness are used)
     * @return rule instance
     */
    public static ColumnQualificationRewriteRule of(CatalogSchema schema, BuiltInRewriteSettings settings) {
        Objects.requireNonNull(schema, "schema must not be null");
        Objects.requireNonNull(settings, "settings must not be null");
        return new ColumnQualificationRewriteRule(ColumnQualificationTransformer.of(catalogResolver(schema, settings)));
    }

    private static ColumnQualificationResolver catalogResolver(CatalogSchema schema, BuiltInRewriteSettings settings) {
        return (columnName, visibleTables) -> {
            VisibleTableBinding chosenVisible = null;
            for (VisibleTableBinding visible : visibleTables) {
                CatalogTable table = resolveVisibleTable(schema, visible, settings);
                if (table == null || table.column(columnName).isEmpty()) {
                    continue;
                }
                if (chosenVisible != null && !chosenVisible.qualifier().equals(visible.qualifier())) {
                    return onQualificationFailure(
                        settings,
                        ReasonCode.DENY_COLUMN,
                        "Ambiguous unqualified column '" + columnName + "' cannot be qualified deterministically"
                    );
                }
                chosenVisible = visible;
            }
            if (chosenVisible == null) {
                return onQualificationFailure(
                    settings,
                    ReasonCode.DENY_COLUMN,
                    "Unqualified column '" + columnName + "' cannot be resolved for qualification"
                );
            }
            return ColumnQualification.qualified(chosenVisible.qualifier());
        };
    }

    private static CatalogTable resolveVisibleTable(CatalogSchema schema, VisibleTableBinding visible, BuiltInRewriteSettings settings) {
        if (visible.schema() != null) {
            return switch (schema.resolve(visible.schema(), visible.tableName())) {
                case CatalogSchema.TableLookupResult.Found found -> found.table();
                default -> null;
            };
        }

        String preferredSchema = settings.qualificationDefaultSchema();
        if (preferredSchema != null) {
            var preferred = schema.resolve(preferredSchema, visible.tableName());
            if (preferred instanceof CatalogSchema.TableLookupResult.Found(CatalogTable table)) {
                return table;
            }
        }

        return switch (schema.resolve(null, visible.tableName())) {
            case CatalogSchema.TableLookupResult.Found found -> found.table();
            default -> null;
        };
    }

    private static ColumnQualification onQualificationFailure(
        BuiltInRewriteSettings settings,
        ReasonCode reasonCode,
        String message
    ) {
        if (settings.qualificationFailureMode() == QualificationFailureMode.DENY) {
            throw new RewriteDenyException(reasonCode, message);
        }
        return ColumnQualification.unresolved();
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
     * Applies column qualification and reports a rewrite only when the AST actually changes.
     *
     * @param query   parsed query model
     * @param context execution context
     * @return rewrite result
     */
    @Override
    public QueryRewriteResult apply(Query query, ExecutionContext context) {
        Objects.requireNonNull(query, "query must not be null");
        Objects.requireNonNull(context, "context must not be null");

        Query transformed = transformer.apply(query);
        if (transformed == query) {
            return QueryRewriteResult.unchanged(transformed);
        }
        return QueryRewriteResult.rewritten(transformed, id(), ReasonCode.REWRITE_QUALIFICATION);
    }
}
