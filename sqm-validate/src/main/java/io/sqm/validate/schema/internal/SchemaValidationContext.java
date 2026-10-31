package io.sqm.validate.schema.internal;

import io.sqm.catalog.access.CatalogAccessPolicies;
import io.sqm.catalog.access.CatalogAccessPolicy;
import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.catalog.model.CatalogType;
import io.sqm.core.*;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.function.DefaultFunctionCatalog;
import io.sqm.validate.schema.function.FunctionCatalog;
import io.sqm.validate.schema.model.CatalogTypeSemantics;

import java.util.*;

/**
 * Mutable validation state shared by schema validation rules.
 *
 * <p>This context owns scope stacks, CTE visibility, problem collection,
 * schema-backed symbol resolution, and basic type inference helpers.</p>
 */
public final class SchemaValidationContext {
    private final CatalogSchema schema;
    private final FunctionCatalog functionCatalog;
    private final CatalogAccessPolicy accessPolicy;
    private final String tenant;
    private final String principal;
    private final List<ValidationProblem> problems = new ArrayList<>();
    private final Deque<Scope> scopes = new ArrayDeque<>();
    private final Deque<QueryIsolation> queryIsolations = new ArrayDeque<>();
    private final Deque<ModelFrame> modelScopes = new ArrayDeque<>();
    private final Deque<Map<String, CteSource>> cteScopes = new ArrayDeque<>();
    private final Deque<PatternExpressionFrame> patternExpressionScopes = new ArrayDeque<>();

    /**
     * Creates a context for the provided schema.
     *
     * @param schema schema used for table and column resolution.
     */
    public SchemaValidationContext(CatalogSchema schema) {
        this(schema, DefaultFunctionCatalog.standard(), CatalogAccessPolicies.allowAll(), null, null);
    }

    /**
     * Creates a context for the provided schema and function catalog.
     *
     * @param schema          schema used for table and column resolution.
     * @param functionCatalog function signature catalog used for return-type inference.
     */
    public SchemaValidationContext(CatalogSchema schema, FunctionCatalog functionCatalog) {
        this(schema, functionCatalog, CatalogAccessPolicies.allowAll(), null, null);
    }

    /**
     * Creates a context for the provided schema, function catalog, and access policy.
     *
     * @param schema          schema used for table and column resolution.
     * @param functionCatalog function signature catalog used for return-type inference.
     * @param accessPolicy    schema access policy.
     * @param principal       principal identifier used for access checks, may be {@code null}.
     */
    public SchemaValidationContext(
        CatalogSchema schema,
        FunctionCatalog functionCatalog,
        CatalogAccessPolicy accessPolicy,
        String principal
    ) {
        this(schema, functionCatalog, accessPolicy, null, principal);
    }

    /**
     * Creates a context for the provided schema, function catalog, and access policy.
     *
     * @param schema          schema used for table and column resolution.
     * @param functionCatalog function signature catalog used for return-type inference.
     * @param accessPolicy    schema access policy.
     * @param tenant          tenant identifier used for access checks, may be {@code null}.
     * @param principal       principal identifier used for access checks, may be {@code null}.
     */
    public SchemaValidationContext(
        CatalogSchema schema,
        FunctionCatalog functionCatalog,
        CatalogAccessPolicy accessPolicy,
        String tenant,
        String principal
    ) {
        this.schema = Objects.requireNonNull(schema, "schema");
        this.functionCatalog = Objects.requireNonNull(functionCatalog, "functionCatalog");
        this.accessPolicy = Objects.requireNonNull(accessPolicy, "accessPolicy");
        this.tenant = tenant;
        this.principal = principal;
    }

    /**
     * Normalizes SQL identifiers to case-insensitive map keys.
     *
     * @param value raw identifier.
     * @return normalized identifier key.
     */
    static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    /**
     * Normalizes identifier to case-insensitive map key.
     *
     * @param value identifier.
     * @return normalized identifier key.
     */
    static String normalize(Identifier value) {
        return normalize(value.value());
    }

    /**
     * Promotes two numeric types to resulting arithmetic type.
     *
     * @param left  left type.
     * @param right right type.
     * @return promoted type.
     */
    private static CatalogType promoteNumeric(CatalogType left, CatalogType right) {
        if (left == CatalogType.DECIMAL || right == CatalogType.DECIMAL) {
            return CatalogType.DECIMAL;
        }
        if (left == CatalogType.LONG || right == CatalogType.LONG) {
            return CatalogType.LONG;
        }
        return CatalogType.INTEGER;
    }

    private static void copyOutputColumns(Map<String, CatalogColumn> output, ResolvedSource source) {
        if (source.quoteAware()) output.putAll(source.columns());
        else source.columns().values().forEach(column -> output.put(column.name().toUpperCase(Locale.ROOT), column));
    }

    private static boolean hasModelOutput(Query query) {
        return switch (query) {
            case SelectQuery select -> select.model() != null;
            case WithQuery with -> hasModelOutput(with.body());
            case CompositeQuery composite -> hasModelOutput(composite.terms().getFirst());
            default -> false;
        };
    }

    private static Map<String, CatalogColumn> renamedProjection(QueryOutput output, List<Identifier> aliases) {
        if (aliases == null || aliases.isEmpty()) return output.columns();
        var result = new LinkedHashMap<String, CatalogColumn>();
        for (var index = 0; index < aliases.size(); index++) {
            var alias = aliases.get(index);
            result.put(ModelScope.key(alias), CatalogColumn.of(alias.value(),
                output.types().size() == aliases.size() ? output.types().get(index) : CatalogType.UNKNOWN));
        }
        return result;
    }

    private static List<Identifier> jsonTableColumnNames(List<JsonTableColumn> columns) {
        var names = new ArrayList<Identifier>();
        collectJsonTableColumnNames(columns, names);
        return names;
    }

    private static void collectJsonTableColumnNames(List<JsonTableColumn> columns, List<Identifier> names) {
        for (var column : columns) {
            if (column instanceof JsonTableNestedPathColumn nested) {
                collectJsonTableColumnNames(nested.columns(), names);
            }
            else
                if (column.name() != null) {
                    names.add(column.name());
                }
        }
    }

    private static String functionName(FunctionExpr functionExpr) {
        if (functionExpr == null || functionExpr.name() == null) {
            return null;
        }
        return functionExpr.name().parts().getLast().value();
    }

    /**
     * Returns immutable snapshot of collected validation problems.
     *
     * @return validation problems.
     */
    public List<ValidationProblem> problems() {
        return List.copyOf(problems);
    }

    /**
     * Returns access policy used by policy-aware validation rules.
     *
     * @return schema access policy.
     */
    public CatalogAccessPolicy accessPolicy() {
        return accessPolicy;
    }

    /**
     * Returns true when table access is denied for the current principal.
     *
     * @param schemaName table schema, may be {@code null}.
     * @param tableName  table name.
     * @return true when denied.
     */
    public boolean isTableDenied(String schemaName, String tableName) {
        return accessPolicy.isTableDenied(tenant, principal, schemaName, tableName);
    }

    /**
     * Returns true when column access is denied for the current principal.
     *
     * @param sourceName source alias or table name, may be {@code null}.
     * @param columnName column name.
     * @return true when denied.
     */
    public boolean isColumnDenied(String sourceName, String columnName) {
        return accessPolicy.isColumnDenied(tenant, principal, sourceName, columnName);
    }

    /**
     * Returns true when the given column access is denied for the current principal.
     *
     * @param sourceName optional source/table alias identifier.
     * @param columnName column identifier.
     * @return true when denied.
     */
    public boolean isColumnDenied(Identifier sourceName, Identifier columnName) {
        return accessPolicy.isColumnDenied(
            tenant,
            principal,
            sourceName == null ? null : sourceName.value(),
            columnName == null ? null : columnName.value()
        );
    }

    /**
     * Returns true when function usage is allowed for the current principal.
     *
     * @param functionName function name.
     * @return true when allowed.
     */
    public boolean isFunctionAllowed(String functionName) {
        return accessPolicy.isFunctionAllowed(tenant, principal, functionName);
    }

    /**
     * Returns the function catalog configured for this validation run.
     *
     * @return configured function catalog.
     */
    public FunctionCatalog functionCatalog() {
        return functionCatalog;
    }

    /**
     * Appends a new validation problem.
     *
     * @param code    problem code.
     * @param message human-readable message.
     */
    public void addProblem(ValidationProblem.Code code, String message) {
        problems.add(new ValidationProblem(code, message));
    }

    /**
     * Appends a new validation problem with explicit structured context.
     *
     * @param code       problem code.
     * @param message    human-readable message.
     * @param nodeKind   node kind hint.
     * @param clausePath clause/path hint.
     */
    public void addProblem(ValidationProblem.Code code, String message, String nodeKind, String clausePath) {
        problems.add(new ValidationProblem(code, message, nodeKind, clausePath));
    }

    /**
     * Appends a new validation problem with derived node kind and clause path.
     *
     * @param code       problem code.
     * @param message    human-readable message.
     * @param node       source node.
     * @param clausePath clause/path hint.
     */
    public void addProblem(ValidationProblem.Code code, String message, Node node, String clausePath) {
        var nodeKind = NodeKinds.of(node);
        addProblem(code, message, nodeKind, clausePath);
    }

    /**
     * Opens a query boundary that cannot capture rows from enclosing query blocks.
     * Nested queries inside this boundary may still correlate to its own sources.
     *
     * @param path stable reference/FOR query diagnostic path
     */
    public void pushQueryIsolation(String path) {
        queryIsolations.push(new QueryIsolation(scopes.size(), path, true));
    }

    /**
     * Closes the current isolated-query boundary.
     */
    public void popQueryIsolation() {
        queryIsolations.pop();
    }

    /**
     * Opens a closed, quote-aware declaration namespace without exposing input-row columns.
     *
     * @param columns resolved declarations keyed by SQL identifier equality
     * @param path    stable scope diagnostic path
     */
    public void pushDeclaredColumns(Map<String, CatalogColumn> columns, String path) {
        pushDeclaredColumns(columns, path, true);
    }

    private void pushDeclaredColumns(Map<String, CatalogColumn> columns, String path, boolean strict) {
        queryIsolations.push(new QueryIsolation(scopes.size(), path, false));
        pushScope();
        var source = new ResolvedSource(Identifier.of("$MODEL"), Collections.unmodifiableMap(new LinkedHashMap<>(columns)), strict, true);
        scopes.peek().sources.add(source);
    }

    /**
     * Closes a declaration namespace and restores its input scope.
     */
    public void popDeclaredColumns() {
        popScope();
        queryIsolations.pop();
    }

    /**
     * Opens MODEL rule type resolution at the current query depth.
     *
     * @param model immutable declaration/type snapshot
     */
    public void pushModelScope(ModelScope model) {
        modelScopes.push(new ModelFrame(model, scopes.size()));
    }

    /**
     * Restores the enclosing MODEL rule context.
     */
    public void popModelScope() {
        modelScopes.pop();
    }

    /**
     * Returns the active MODEL namespace, excluding nested ordinary query scopes.
     *
     * @return active snapshot, or null outside MODEL rules
     */
    public ModelScope modelScope() {
        var frame = modelScopes.peek();
        return frame != null && frame.depth() == scopes.size() ? frame.model() : null;
    }

    /**
     * Infers declaration types against input rows and each reference query's own output.
     *
     * @param clause MODEL clause
     * @return immutable main/reference type snapshot
     */
    public ModelScope modelScope(ModelClause clause) {
        var columns = declarationColumns(clause.main().partitions(), clause.main().dimensions(), clause.main().measures());
        var references = new LinkedHashMap<String, Map<String, CatalogColumn>>();
        for (var reference : clause.references()) {
            pushQueryOutput(reference.query(), "model.reference.declarations");
            try {
                references.putIfAbsent(ModelScope.key(reference.name()),
                    declarationColumns(List.of(), reference.dimensions(), reference.measures()));
            } finally {
                popDeclaredColumns();
            }
        }
        return new ModelScope(clause, columns, references);
    }

    /**
     * Derives the closed post-MODEL row schema in partition/dimension/measure order.
     *
     * @param main main model
     * @return declared output columns and their input-derived types
     */
    public Map<String, CatalogColumn> modelOutputColumns(MainModel main) {
        return Collections.unmodifiableMap(declarationColumns(main.partitions(), main.dimensions(), main.measures()));
    }

    private Map<String, CatalogColumn> declarationColumns(List<ModelColumn> partitions,
        List<ModelColumn> dimensions, List<ModelColumn> measures) {
        var result = new LinkedHashMap<String, CatalogColumn>();
        for (var list : List.of(partitions, dimensions, measures)) {
            for (var column : list) {
                result.putIfAbsent(ModelScope.key(column.name()), CatalogColumn.of(column.name().value(),
                    inferType(column.expression()).orElse(CatalogType.UNKNOWN)));
            }
        }
        return result;
    }

    /**
     * Derives nameable query output columns, including MODEL's closed output shape.
     *
     * @param query source query
     * @return columns and known types under quote-aware SQL keys
     */
    public Map<String, CatalogColumn> queryOutputColumns(Query query) {
        return queryOutput(query).columns();
    }

    /**
     * Opens a reference-query output namespace. Unknown star expansions remain
     * permissive; explicit projections and MODEL output are closed namespaces.
     *
     * @param query reference source
     * @param path  diagnostic scope path
     */
    public void pushQueryOutput(Query query, String path) {
        var output = queryOutput(query);
        pushDeclaredColumns(output.columns(), path, output.known());
    }

    private QueryOutput queryOutput(Query query) {
        var problemCount = problems.size();
        try {
            return inferQueryOutput(query);
        } finally {
            // Metadata inference must not duplicate traversal's table/policy diagnostics.
            problems.subList(problemCount, problems.size()).clear();
        }
    }

    private QueryOutput inferQueryOutput(Query query) {
        if (query instanceof WithQuery with) {
            pushWithScope();
            try {
                for (var cte : with.ctes()) {
                    registerCte(cte);
                }
                return queryOutput(with.body());
            } finally {
                popWithScope();
            }
        }
        if (query instanceof CompositeQuery composite) return queryOutput(composite.terms().getFirst());
        if (!(query instanceof SelectQuery select)) return new QueryOutput(Map.of(), false, List.of());
        pushScope();
        try {
            registerTableRef(select.from());
            select.joins().forEach(join -> registerTableRef(join.right()));
            var output = new LinkedHashMap<String, CatalogColumn>();
            var types = new ArrayList<CatalogType>();
            Map<String, CatalogColumn> model = select.model() == null ? null
                : declarationColumns(select.model().main().partitions(), select.model().main().dimensions(), select.model().main().measures());
            if (model != null) pushDeclaredColumns(model, "model.output");
            try {
                boolean known = true;
                for (var item : select.items()) {
                    if (item instanceof ExprSelectItem expression) {
                        var type = inferType(expression.expr()).orElse(CatalogType.UNKNOWN);
                        types.add(type);
                        var name = expression.alias() != null ? expression.alias()
                            : expression.expr() instanceof ColumnExpr column ? column.name() : null;
                        if (name != null) output.put(ModelScope.key(name), CatalogColumn.of(name.value(), type));
                    }
                    else
                        if (model != null && item instanceof StarSelectItem) {
                            output.putAll(model);
                            model.values().forEach(column -> types.add(column.type()));
                        }
                        else
                            if (item instanceof StarSelectItem) {
                                for (var source : scopes.peek().sources) {
                                    known &= source.strictColumns();
                                    copyOutputColumns(output, source);
                                    source.columns().values().forEach(column -> types.add(column.type()));
                                }
                            }
                            else
                                if (item instanceof QualifiedStarSelectItem star) {
                                    var source = findSource(star.qualifier(), ScopeResolutionMode.CURRENT_SCOPE);
                                    known &= source.isPresent() && source.get().strictColumns();
                                    source.ifPresent(binding -> copyOutputColumns(output, binding));
                                    source.ifPresent(binding -> binding.columns().values().forEach(column -> types.add(column.type())));
                                }
                }
                return new QueryOutput(Collections.unmodifiableMap(output), known, known ? List.copyOf(types) : List.of());
            } finally {
                if (model != null) popDeclaredColumns();
            }
        } finally {
            popScope();
        }
    }

    private Map<String, CatalogColumn> renamedProjection(Query query, List<Identifier> aliases) {
        return renamedProjection(queryOutput(query), aliases);
    }

    private boolean reportIsolatedReference(ColumnExpr column, boolean reportErrors) {
        var boundary = queryIsolations.peek();
        if (boundary == null || !boundary.uncorrelated()) return false;
        var blocked = scopes.stream().skip(Math.max(0, scopes.size() - boundary.depth())).toList();
        boolean found = blocked.stream().anyMatch(scope -> column.tableAlias() == null
            ? scope.sources().stream().anyMatch(source -> source.lookup(column.name()) != null)
            : scope.byAlias().containsKey(normalize(column.tableAlias())));
        if (found && reportErrors) addProblem(ValidationProblem.Code.DIALECT_CLAUSE_INVALID,
            "MODEL reference/FOR query cannot contain correlated outer references", column, boundary.path());
        return found;
    }

    /**
     * Opens a new table-alias scope for a SELECT block.
     */
    public void pushScope() {
        scopes.push(new Scope());
    }

    /**
     * Closes current table-alias scope.
     */
    public void popScope() {
        scopes.pop();
    }

    /**
     * Opens a row-pattern expression scope.
     *
     * @param kind       expression location kind.
     * @param clausePath stable clause path for diagnostics.
     */
    public void pushPatternExpressionScope(PatternExpressionKind kind, String clausePath) {
        patternExpressionScopes.push(new PatternExpressionFrame(
            Objects.requireNonNull(kind, "kind"),
            Objects.requireNonNull(clausePath, "clausePath")
        ));
    }

    /**
     * Closes the current row-pattern expression scope.
     */
    public void popPatternExpressionScope() {
        patternExpressionScopes.pop();
    }

    /**
     * Returns whether validation is currently inside a row-pattern expression.
     *
     * @return {@code true} inside a measure or definition expression.
     */
    public boolean inPatternExpressionScope() {
        return !patternExpressionScopes.isEmpty();
    }

    /**
     * Returns the current row-pattern expression kind.
     *
     * @return current kind, or {@code null} outside row-pattern expressions.
     */
    public PatternExpressionKind patternExpressionKind() {
        var frame = patternExpressionScopes.peek();
        return frame == null ? null : frame.kind();
    }

    /**
     * Returns the current stable row-pattern clause path.
     *
     * @return clause path, or a generic expression path outside row-pattern scope.
     */
    public String patternExpressionPath() {
        var frame = patternExpressionScopes.peek();
        return frame == null ? "expression.matchRecognize" : frame.clausePath();
    }

    /**
     * Opens a new CTE scope inheriting previously visible CTEs.
     */
    public void pushWithScope() {
        cteScopes.push(new LinkedHashMap<>(currentCtes()));
    }

    /**
     * Registers a CTE name and optional column aliases in current WITH scope.
     *
     * @param cte CTE definition.
     */
    public void registerCte(CteDef cte) {
        var ctes = cteScopes.peek();
        if (ctes != null) {
            ctes.put(normalize(cte.name()), CteSource.from(cte));
            if (cte.body() instanceof Query query) {
                var output = queryOutput(query);
                var fields = renamedProjection(output, cte.columnAliases());
                var explicitAliases = cte.columnAliases() != null && !cte.columnAliases().isEmpty();
                ctes.put(normalize(cte.name()), new CteSource(fields, explicitAliases || output.known(), true));
            }
        }
    }

    /**
     * Closes current CTE scope.
     */
    public void popWithScope() {
        cteScopes.pop();
    }

    /**
     * Indicates whether an unqualified table name resolves to a visible CTE.
     *
     * @param table table reference
     * @return whether the reference names a CTE rather than a physical table
     */
    public boolean isCteReference(Table table) {
        return table.schema() == null && currentCtes().containsKey(normalize(table.name()));
    }

    /**
     * Registers a table-like source as visible in current SELECT scope.
     *
     * @param ref table reference from FROM or JOIN clause.
     */
    public void registerTableRef(TableRef ref) {
        if (ref == null || scopes.isEmpty()) {
            return;
        }
        switch (ref) {
            case Lateral lateral -> registerTableRef(lateral.inner());
            case PatternRecognitionTable patternRecognition -> registerPatternRecognition(patternRecognition);
            case PivotTable pivot -> {
                if (pivot.alias() == null) {
                    registerTableRef(pivot.source());
                }
                else {
                    registerDerivedSource(pivot.alias(), List.of());
                }
            }
            case UnpivotTable unpivot -> {
                if (unpivot.alias() == null) {
                    registerTableRef(unpivot.source());
                }
                else {
                    registerDerivedSource(unpivot.alias(), List.of());
                }
            }
            case Table table -> registerPhysicalTable(table);
            case QueryTable queryTable -> {
                if (queryTable.alias() != null && hasModelOutput(queryTable.query())) {
                    registerSource(queryTable.alias(), new ResolvedSource(queryTable.alias(),
                        renamedProjection(queryTable.query(), queryTable.columnAliases()), true, true));
                }
                else {
                    registerDerivedSource(queryTable.alias(), queryTable.columnAliases());
                }
            }
            case ValuesTable valuesTable -> registerDerivedSource(
                valuesTable.alias(),
                valuesTable.columnAliases()
            );
            case FunctionTable functionTable -> registerDerivedSource(
                functionTable.alias(),
                functionTable.columnAliases()
            );
            case JsonTable jsonTable -> registerDerivedSource(
                jsonTable.alias(),
                jsonTableColumnNames(jsonTable.columns())
            );
            default -> {
            }
        }
    }

    /**
     * Resolves normalized source key for a table reference in current scope.
     *
     * @param ref table reference.
     * @return normalized source key if available.
     */
    public Optional<String> sourceKey(TableRef ref) {
        if (ref == null) {
            return Optional.empty();
        }
        return switch (ref) {
            case Lateral lateral -> sourceKey(lateral.inner());
            case PatternRecognitionTable patternRecognition -> patternRecognition.alias() == null
                ? sourceKey(patternRecognition.source())
                : Optional.of(normalize(patternRecognition.alias()));
            case PivotTable pivot -> pivot.alias() == null
                ? sourceKey(pivot.source())
                : Optional.of(normalize(pivot.alias()));
            case UnpivotTable unpivot -> unpivot.alias() == null
                ? sourceKey(unpivot.source())
                : Optional.of(normalize(unpivot.alias()));
            case Table table -> Optional.of(normalize(table.alias() == null ? table.name() : table.alias()));
            case QueryTable queryTable -> queryTable.alias() == null
                ? Optional.empty()
                : Optional.of(normalize(queryTable.alias()));
            case ValuesTable valuesTable -> valuesTable.alias() == null
                ? Optional.empty()
                : Optional.of(normalize(valuesTable.alias()));
            case FunctionTable functionTable -> functionTable.alias() == null
                ? Optional.empty()
                : Optional.of(normalize(functionTable.alias()));
            case JsonTable jsonTable -> jsonTable.alias() == null
                ? Optional.empty()
                : Optional.of(normalize(jsonTable.alias()));
            default -> Optional.empty();
        };
    }

    /**
     * Counts strict current-scope sources that contain the given column.
     *
     * @param columnName        column name to lookup.
     * @param excludedSourceKey optional normalized source key to exclude.
     * @return number of matching strict sources.
     */
    public int countStrictSourcesWithColumn(String columnName, String excludedSourceKey) {
        return countStrictSourcesWithColumn(columnName, excludedSourceKey, ScopeResolutionMode.CURRENT_SCOPE);
    }

    /**
     * Counts strict current-scope sources that contain the given column.
     *
     * @param columnName        column identifier to lookup.
     * @param excludedSourceKey optional normalized source key to exclude.
     * @return number of matching strict sources.
     */
    public int countStrictSourcesWithColumn(Identifier columnName, String excludedSourceKey) {
        if (columnName == null) {
            return 0;
        }
        return countStrictSourcesWithColumn(columnName.value(), excludedSourceKey, ScopeResolutionMode.CURRENT_SCOPE);
    }

    /**
     * Returns type of a column in a strict current-scope source.
     *
     * @param sourceKey  normalized or raw source key.
     * @param columnName column name.
     * @return column type when source and column are known.
     */
    public Optional<CatalogType> sourceColumnType(String sourceKey, String columnName) {
        return sourceColumnType(sourceKey, columnName, ScopeResolutionMode.CURRENT_SCOPE);
    }

    /**
     * Returns type of a column in a strict current-scope source.
     *
     * @param sourceKey  normalized or raw source key.
     * @param columnName column identifier.
     * @return column type when source and column are known.
     */
    public Optional<CatalogType> sourceColumnType(String sourceKey, Identifier columnName) {
        if (columnName == null) {
            return Optional.empty();
        }
        return sourceColumnType(sourceKey, columnName.value(), ScopeResolutionMode.CURRENT_SCOPE);
    }

    /**
     * Returns normalized source keys visible in current scope.
     *
     * @return source keys in declaration order.
     */
    public List<String> currentScopeSourceKeys() {
        return sourceKeys(ScopeResolutionMode.CURRENT_SCOPE);
    }

    /**
     * Registers aliases visible to a specific ON join predicate.
     *
     * @param join    ON join node.
     * @param aliases aliases visible in this join position.
     */
    public void registerOnJoinVisibleAliases(OnJoin join, Set<String> aliases) {
        var scope = currentScope().orElse(null);
        if (scope == null || join == null) {
            return;
        }
        var normalizedAliases = new LinkedHashSet<String>(aliases.size());
        for (var alias : aliases) {
            normalizedAliases.add(normalize(alias));
        }
        scope.onJoinVisibleAliases.put(join, Set.copyOf(normalizedAliases));
    }

    /**
     * Returns aliases visible to a specific ON join predicate.
     *
     * @param join ON join node.
     * @return normalized alias set visible to the join ON predicate.
     */
    public Set<String> onJoinVisibleAliases(OnJoin join) {
        var scope = currentScope().orElse(null);
        if (scope == null || join == null) {
            return Set.of();
        }
        return scope.onJoinVisibleAliases.getOrDefault(join, Set.of());
    }

    /**
     * Resolves a column against visible sources.
     *
     * @param column       column reference expression.
     * @param reportErrors whether lookup failures should be recorded as problems.
     * @return resolved column metadata when available.
     */
    public Optional<CatalogColumn> resolveColumn(ColumnExpr column, boolean reportErrors) {
        return resolveColumn(column, ScopeResolutionMode.ALL_SCOPES, reportErrors);
    }

    /**
     * Resolves a row-pattern column against the input relation in the current scope.
     *
     * @param column     column identifier.
     * @param node       source expression used in diagnostics.
     * @param clausePath stable clause path.
     * @return resolved input column when known.
     */
    public Optional<CatalogColumn> resolvePatternColumn(Identifier column, Node node, String clausePath) {
        return resolveCurrentScopeColumn(null, column, true, node, clausePath);
    }

    /**
     * Resolves a qualified assignment target against the current DML scope.
     *
     * @param column       assignment target column name.
     * @param reportErrors whether lookup failures should be recorded as problems.
     * @param node         source node for structured problem reporting.
     * @param clausePath   clause/path hint to attach to reported problems.
     * @return resolved column metadata when available.
     */
    public Optional<CatalogColumn> resolveCurrentScopeColumn(
        QualifiedName column,
        boolean reportErrors,
        Node node,
        String clausePath
    ) {
        Objects.requireNonNull(column, "column");
        var parts = column.parts();
        if (parts.size() == 1) {
            return resolveCurrentScopeColumn(null, parts.getFirst(), reportErrors, node, clausePath);
        }
        if (parts.size() == 2) {
            return resolveCurrentScopeColumn(parts.getFirst(), parts.get(1), reportErrors, node, clausePath);
        }
        if (reportErrors) {
            addProblem(
                ValidationProblem.Code.COLUMN_NOT_FOUND,
                "Invalid qualified column reference: " + String.join(".", column.values()),
                node,
                clausePath
            );
        }
        return Optional.empty();
    }

    /**
     * Resolves a physical target-table column directly from the catalog.
     *
     * @param table  target table.
     * @param column column identifier.
     * @return resolved catalog column when the physical table is known.
     */
    public Optional<CatalogColumn> resolvePhysicalTableColumn(Table table, Identifier column) {
        if (table == null || column == null) {
            return Optional.empty();
        }
        var lookup = schema.resolve(table.schema() == null ? null : table.schema().value(), table.name().value());
        if (!(lookup instanceof CatalogSchema.TableLookupResult.Found(CatalogTable foundTable))) {
            return Optional.empty();
        }
        if (!foundTable.strictColumns()) {
            return Optional.of(CatalogColumn.of(column.value(), CatalogType.UNKNOWN));
        }
        var normalizedColumn = normalize(column);
        return foundTable.columns().stream()
            .filter(candidate -> normalize(candidate.name()).equals(normalizedColumn))
            .findFirst();
    }

    /**
     * Infers expression type when it can be derived from schema or literal value.
     *
     * @param expression expression to analyze.
     * @return inferred type if known.
     */
    public Optional<CatalogType> inferType(Expression expression) {
        return inferType(expression, ScopeResolutionMode.ALL_SCOPES);
    }

    /**
     * Infers cast target type when it maps to a known validation type.
     *
     * @param typeName cast target type.
     * @return inferred cast result type.
     */
    private Optional<CatalogType> inferCastType(TypeName typeName) {
        if (typeName == null) {
            return Optional.empty();
        }
        if (typeName.keyword().isPresent()) {
            return switch (typeName.keyword().get()) {
                case DOUBLE_PRECISION -> Optional.of(CatalogType.DECIMAL);
                case CHARACTER_VARYING, NATIONAL_CHARACTER, NATIONAL_CHARACTER_VARYING -> Optional.of(CatalogType.STRING);
            };
        }
        if (typeName.qualifiedName() == null) {
            return Optional.empty();
        }
        return Optional.of(CatalogTypeSemantics.fromSqlType(typeName.qualifiedName().parts().getLast().value()));
    }

    /**
     * Infers function return type via configured function catalog metadata.
     *
     * @param functionExpr function expression.
     * @return inferred return type.
     */
    private Optional<CatalogType> inferFunctionType(FunctionExpr functionExpr) {
        var functionName = functionName(functionExpr);
        if (functionName == null) {
            return Optional.empty();
        }
        return functionCatalog.resolve(functionName).flatMap(io.sqm.validate.schema.function.FunctionSignature::returnType);
    }

    /**
     * Infers the single-column type produced by a query when statically possible.
     *
     * @param query query to analyze.
     * @return inferred type for the first projected expression.
     */
    public Optional<CatalogType> inferSingleColumnType(Query query) {
        return switch (query) {
            case SelectQuery select -> inferSingleColumnType(select);
            case CompositeQuery composite -> composite.terms().isEmpty()
                ? Optional.empty()
                : inferSingleColumnType(composite.terms().getFirst());
            case WithQuery with -> inferSingleColumnType(with.body());
            default -> Optional.empty();
        };
    }

    /**
     * Infers result type for a single-expression SELECT projection.
     *
     * @param select select query.
     * @return inferred projection type.
     */
    private Optional<CatalogType> inferSingleColumnType(SelectQuery select) {
        var projectionTypes = inferProjectionTypes(select);
        if (projectionTypes.isEmpty() || projectionTypes.get().size() != 1) {
            return Optional.empty();
        }
        return projectionTypes.get().getFirst();
    }

    /**
     * Infers projection types for expression-only select items in local SELECT scope.
     *
     * @param select select query.
     * @return inferred projection types or empty when projection is not expression-only.
     */
    public Optional<List<Optional<CatalogType>>> inferProjectionTypes(SelectQuery select) {
        if (select == null) {
            return Optional.empty();
        }
        pushScope();
        try {
            registerTableRef(select.from());
            for (var join : select.joins()) {
                registerTableRef(join.right());
            }
            if (select.model() != null) pushDeclaredColumns(modelOutputColumns(select.model().main()), "model.output");
            try {
                var types = new ArrayList<Optional<CatalogType>>(select.items().size());
                for (var item : select.items()) {
                    if (item instanceof StarSelectItem && select.model() != null) {
                        scopes.peek().sources.getFirst().columns().values().forEach(column -> types.add(Optional.of(column.type())));
                        continue;
                    }
                    if (!(item instanceof ExprSelectItem exprItem)) {
                        return Optional.empty();
                    }
                    types.add(inferTypeInCurrentScope(exprItem.expr()));
                }
                return Optional.of(List.copyOf(types));
            } finally {
                if (select.model() != null) popDeclaredColumns();
            }
        } finally {
            popScope();
        }
    }

    /**
     * Resolves physical table or CTE and registers it in current scope.
     *
     * @param table table reference.
     */
    private void registerPhysicalTable(Table table) {
        if (isTableDenied(table.schema() == null ? null : table.schema().value(), table.name().value())) {
            var tableName = table.schema() == null ? table.name().value() : table.schema().value() + "." + table.name().value();
            addProblem(
                ValidationProblem.Code.POLICY_TABLE_DENIED,
                "Table is denied by policy: " + tableName,
                table,
                "from.table"
            );
        }

        var aliasOrName = table.alias() == null ? table.name() : table.alias();
        var cte = currentCtes().get(normalize(table.name()));
        if (table.schema() == null && cte != null) {
            registerSource(aliasOrName, new ResolvedSource(aliasOrName, cte.columns(), cte.strictColumns(), cte.quoteAware()));
            return;
        }

        var lookup = schema.resolve(table.schema() == null ? null : table.schema().value(), table.name().value());
        if (lookup instanceof CatalogSchema.TableLookupResult.NotFound(String schema1, String name)) {
            var tableName = schema1 == null ? name : schema1 + "." + name;
            addProblem(ValidationProblem.Code.TABLE_NOT_FOUND, "Table not found: " + tableName, table, "from.table");
            registerSource(aliasOrName, ResolvedSource.of(aliasOrName, Map.of(), false));
            return;
        }
        if (lookup instanceof CatalogSchema.TableLookupResult.Ambiguous ambiguous) {
            addProblem(ValidationProblem.Code.TABLE_AMBIGUOUS, "Ambiguous unqualified table: " + ambiguous.name(), table, "from.table");
            registerSource(aliasOrName, ResolvedSource.of(aliasOrName, Map.of(), false));
            return;
        }

        var found = (CatalogSchema.TableLookupResult.Found) lookup;
        var columnsByName = new LinkedHashMap<String, CatalogColumn>();
        for (var column : found.table().columns()) {
            columnsByName.put(normalize(column.name()), column);
        }
        registerSource(aliasOrName, ResolvedSource.of(aliasOrName, columnsByName, found.table().strictColumns()));
    }

    /**
     * Registers derived source aliases with optional column aliases.
     *
     * <p>Derived source columns are treated as non-strict because their effective
     * shape can be unknown without deeper query analysis.</p>
     *
     * @param alias         source alias.
     * @param columnAliases explicit result aliases.
     */
    private void registerDerivedSource(Identifier alias, List<Identifier> columnAliases) {
        if (alias == null) {
            return;
        }
        var columns = new LinkedHashMap<String, CatalogColumn>();
        if (columnAliases != null) {
            for (var columnAlias : columnAliases) {
                columns.put(normalize(columnAlias), CatalogColumn.of(columnAlias.value(), CatalogType.STRING));
            }
        }
        registerSource(alias, ResolvedSource.of(alias, columns, false));
    }

    /**
     * Registers the visible output shape of a pattern-recognition relation.
     *
     * <p>One-row output has a closed set of nameable partition columns and
     * measure aliases. All-rows output remains non-strict because Oracle also
     * exposes input-derived columns whose exact shape is intentionally not
     * inferred here.</p>
     */
    private void registerPatternRecognition(PatternRecognitionTable table) {
        var sourceName = table.alias() == null
            ? sourceKey(table.source())
            : Optional.of(normalize(table.alias()));
        if (sourceName.isEmpty()) {
            return;
        }
        var alias = Identifier.of(sourceName.orElseThrow());
        var columns = new LinkedHashMap<String, CatalogColumn>();
        if (table.partitionBy() != null) {
            for (var expression : table.partitionBy().items()) {
                if (expression instanceof ColumnExpr column) {
                    columns.put(normalize(column.name()), CatalogColumn.of(column.name().value(), CatalogType.UNKNOWN));
                }
            }
        }
        for (var measure : table.measures()) {
            columns.put(normalize(measure.alias()), CatalogColumn.of(measure.alias().value(), CatalogType.UNKNOWN));
        }
        registerSource(alias, ResolvedSource.of(
            alias,
            columns,
            table.rowsPerMatch().mode() == RowsPerMatch.Mode.ONE
        ));
    }

    /**
     * Registers a source under the provided alias and checks duplicate aliases.
     *
     * @param aliasOrName effective source alias key.
     * @param source      source metadata.
     */
    private void registerSource(Identifier aliasOrName, ResolvedSource source) {
        var scope = currentScope().orElse(null);
        if (scope == null) {
            return;
        }
        var key = normalize(aliasOrName);
        var previous = scope.byAlias.putIfAbsent(key, source);
        if (previous != null) {
            addProblem(
                ValidationProblem.Code.DUPLICATE_TABLE_ALIAS,
                "Duplicate table alias in scope: " + aliasOrName.value(),
                "TableRef",
                "from"
            );
        }
        scope.sources.add(source);
    }

    /**
     * Infers expression type using only current SELECT scope sources.
     *
     * <p>This method is used for subquery projection type inference to avoid
     * accidental capture from outer scopes.</p>
     *
     * @param expression expression to infer.
     * @return inferred type.
     */
    private Optional<CatalogType> inferTypeInCurrentScope(Expression expression) {
        return inferType(expression, ScopeResolutionMode.CURRENT_SCOPE);
    }

    /**
     * Resolves a column against selected scope visibility.
     *
     * @param column       column reference expression.
     * @param mode         scope resolution mode.
     * @param reportErrors whether lookup failures should be recorded as problems.
     * @return resolved column metadata when available.
     */
    private Optional<CatalogColumn> resolveColumn(
        ColumnExpr column,
        ScopeResolutionMode mode,
        boolean reportErrors
    ) {
        if (column.tableAlias() != null) {
            var source = findSource(column.tableAlias(), mode);
            if (source.isEmpty()) {
                if (reportIsolatedReference(column, reportErrors)) return Optional.empty();
                if (reportErrors) {
                    addProblem(
                        ValidationProblem.Code.UNKNOWN_TABLE_ALIAS,
                        "Unknown table alias: " + column.tableAlias().value(),
                        column,
                        "column.reference"
                    );
                }
                return Optional.empty();
            }
            var resolvedSource = source.get();
            if (!resolvedSource.strictColumns()) {
                return Optional.empty();
            }
            var dbColumn = resolvedSource.lookup(column.name());
            if (dbColumn == null && reportErrors) {
                addProblem(
                    ValidationProblem.Code.COLUMN_NOT_FOUND,
                    "Column not found: " + column.tableAlias().value() + "." + column.name().value(),
                    column,
                    "column.reference"
                );
            }
            return Optional.ofNullable(dbColumn);
        }

        var matches = new ArrayList<CatalogColumn>();
        var unknownSourceVisible = false;
        for (var scope : iterScopes(mode)) {
            for (var source : scope.sources) {
                if (!source.strictColumns()) {
                    unknownSourceVisible = true;
                    continue;
                }
                var dbColumn = source.lookup(column.name());
                if (dbColumn != null) {
                    matches.add(dbColumn);
                }
            }
            // A local binding shadows outer scopes. An unknown local row shape
            // also prevents proving that an unqualified name belongs to an outer row.
            if (!matches.isEmpty() || unknownSourceVisible) break;
        }
        if (matches.isEmpty()) {
            if (!unknownSourceVisible && reportIsolatedReference(column, reportErrors)) return Optional.empty();
            if (reportErrors && (!unknownSourceVisible || mode == ScopeResolutionMode.CURRENT_SCOPE)) {
                addProblem(
                    ValidationProblem.Code.COLUMN_NOT_FOUND,
                    "Column not found: " + column.name().value(),
                    column,
                    "column.reference"
                );
            }
            return Optional.empty();
        }
        if (matches.size() > 1) {
            if (reportErrors) {
                addProblem(
                    ValidationProblem.Code.COLUMN_AMBIGUOUS,
                    "Ambiguous column reference: " + column.name().value(),
                    column,
                    "column.reference"
                );
            }
            return Optional.empty();
        }
        return Optional.of(matches.getFirst());
    }

    /**
     * Resolves a column against current-scope sources only and reports DML-specific clause paths.
     *
     * @param sourceAlias  optional source alias qualifier.
     * @param columnName   column identifier.
     * @param reportErrors whether lookup failures should be recorded as problems.
     * @param node         source node for structured problem reporting.
     * @param clausePath   clause/path hint.
     * @return resolved column metadata when available.
     */
    private Optional<CatalogColumn> resolveCurrentScopeColumn(
        Identifier sourceAlias,
        Identifier columnName,
        boolean reportErrors,
        Node node,
        String clausePath
    ) {
        if (columnName == null) {
            return Optional.empty();
        }
        if (sourceAlias != null) {
            var source = findSource(sourceAlias, ScopeResolutionMode.CURRENT_SCOPE);
            if (source.isEmpty()) {
                if (reportErrors) {
                    addProblem(
                        ValidationProblem.Code.UNKNOWN_TABLE_ALIAS,
                        "Unknown table alias: " + sourceAlias.value(),
                        node,
                        clausePath
                    );
                }
                return Optional.empty();
            }
            var resolvedSource = source.get();
            if (!resolvedSource.strictColumns()) {
                return Optional.empty();
            }
            var dbColumn = resolvedSource.lookup(columnName);
            if (dbColumn == null && reportErrors) {
                addProblem(
                    ValidationProblem.Code.COLUMN_NOT_FOUND,
                    "Column not found: " + sourceAlias.value() + "." + columnName.value(),
                    node,
                    clausePath
                );
            }
            return Optional.ofNullable(dbColumn);
        }

        var matches = new ArrayList<CatalogColumn>();
        var unknownSourceVisible = false;
        for (var scope : iterScopes(ScopeResolutionMode.CURRENT_SCOPE)) {
            for (var source : scope.sources) {
                if (!source.strictColumns()) {
                    unknownSourceVisible = true;
                    continue;
                }
                var dbColumn = source.lookup(columnName);
                if (dbColumn != null) {
                    matches.add(dbColumn);
                }
            }
        }
        if (matches.isEmpty()) {
            if (reportErrors && !unknownSourceVisible) {
                addProblem(
                    ValidationProblem.Code.COLUMN_NOT_FOUND,
                    "Column not found: " + columnName.value(),
                    node,
                    clausePath
                );
            }
            return Optional.empty();
        }
        if (matches.size() > 1) {
            if (reportErrors) {
                addProblem(
                    ValidationProblem.Code.COLUMN_AMBIGUOUS,
                    "Ambiguous column reference: " + columnName.value(),
                    node,
                    clausePath
                );
            }
            return Optional.empty();
        }
        return Optional.of(matches.getFirst());
    }

    /**
     * Infers expression type under selected scope visibility.
     *
     * @param expression expression to analyze.
     * @param mode       scope resolution mode.
     * @return inferred type if known.
     */
    private Optional<CatalogType> inferType(Expression expression, ScopeResolutionMode mode) {
        return switch (expression) {
            case CellRefExpr cell -> Optional.ofNullable(modelScope()).map(scope -> scope.resolve(cell.model(), cell.measure()))
                .map(CatalogColumn::type);
            case ModelAggregateExpr aggregate -> inferType(aggregate.aggregate(), mode);
            case CurrentDimensionExpr current -> current.dimension() == null ? Optional.empty()
                : Optional.ofNullable(modelScope()).map(scope -> scope.resolve(null, current.dimension())).map(CatalogColumn::type);
            case IterationNumberExpr ignored -> Optional.of(CatalogType.INTEGER);
            case PreviousModelValueExpr previous -> inferType(previous.cell(), mode);
            case PresenceValueExpr presence -> inferType(presence.whenPresent(), mode);
            case LiteralExpr literalExpr -> CatalogTypeSemantics.fromLiteral(literalExpr.value());
            case ColumnExpr columnExpr -> resolveColumn(columnExpr, mode, false).map(column -> column.type());
            case QueryExpr queryExpr -> inferSingleColumnType(queryExpr.subquery());
            case CastExpr castExpr -> inferCastType(castExpr.type());
            case FunctionExpr functionExpr -> inferFunctionType(functionExpr);
            case NegativeArithmeticExpr negative -> inferNumericType(negative.expr(), mode);
            case BinaryArithmeticExpr binary -> inferBinaryNumericType(binary.lhs(), binary.rhs(), mode);
            case PowerArithmeticExpr power -> inferBinaryNumericType(power.lhs(), power.rhs(), mode);
            default -> Optional.empty();
        };
    }

    /**
     * Infers numeric type for unary arithmetic expression under selected scope visibility.
     *
     * @param expression operand expression.
     * @param mode       scope resolution mode.
     * @return numeric type when operand is numeric.
     */
    private Optional<CatalogType> inferNumericType(Expression expression, ScopeResolutionMode mode) {
        var operandType = inferType(expression, mode);
        return operandType.filter(CatalogTypeSemantics::isNumeric);
    }

    /**
     * Infers numeric type for binary arithmetic expression under selected scope visibility.
     *
     * @param lhs  left operand.
     * @param rhs  right operand.
     * @param mode scope resolution mode.
     * @return promoted numeric type when both operands are numeric.
     */
    private Optional<CatalogType> inferBinaryNumericType(Expression lhs, Expression rhs, ScopeResolutionMode mode) {
        var left = inferType(lhs, mode);
        var right = inferType(rhs, mode);
        if (left.isEmpty() || right.isEmpty()) {
            return Optional.empty();
        }
        if (!CatalogTypeSemantics.isNumeric(left.get()) || !CatalogTypeSemantics.isNumeric(right.get())) {
            return Optional.empty();
        }
        return Optional.of(promoteNumeric(left.get(), right.get()));
    }

    /**
     * Finds source metadata by alias for selected scope visibility.
     *
     * @param alias source alias.
     * @param mode  scope resolution mode.
     * @return resolved source metadata.
     */
    private Optional<ResolvedSource> findSource(Identifier alias, ScopeResolutionMode mode) {
        var key = normalize(alias);
        for (var scope : iterScopes(mode)) {
            var source = scope.byAlias.get(key);
            if (source != null) {
                return Optional.of(source);
            }
        }
        return Optional.empty();
    }

    /**
     * Finds source metadata by raw or normalized alias for selected scope visibility.
     *
     * @param alias source alias text.
     * @param mode  scope resolution mode.
     * @return resolved source metadata.
     */
    private Optional<ResolvedSource> findSource(String alias, ScopeResolutionMode mode) {
        if (alias == null) {
            return Optional.empty();
        }
        var key = normalize(alias);
        for (var scope : iterScopes(mode)) {
            var source = scope.byAlias.get(key);
            if (source != null) {
                return Optional.of(source);
            }
        }
        return Optional.empty();
    }

    /**
     * Counts strict sources with a matching column under selected scope visibility.
     *
     * @param columnName        column name to lookup.
     * @param excludedSourceKey optional normalized source key to exclude.
     * @param mode              scope resolution mode.
     * @return number of matching strict sources.
     */
    private int countStrictSourcesWithColumn(
        String columnName,
        String excludedSourceKey,
        ScopeResolutionMode mode
    ) {
        var normalizedColumn = normalize(columnName);
        var normalizedExclude = excludedSourceKey == null ? null : normalize(excludedSourceKey);
        var count = 0;
        for (var scope : iterScopes(mode)) {
            for (var entry : scope.byAlias.entrySet()) {
                if (normalizedExclude != null && normalizedExclude.equals(entry.getKey())) {
                    continue;
                }
                var source = entry.getValue();
                if (!source.strictColumns()) {
                    continue;
                }
                if (source.columns().containsKey(normalizedColumn)) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Returns type of a column in a strict source under selected scope visibility.
     *
     * @param sourceKey  normalized or raw source key.
     * @param columnName column name.
     * @param mode       scope resolution mode.
     * @return column type when source and column are known.
     */
    private Optional<CatalogType> sourceColumnType(
        String sourceKey,
        String columnName,
        ScopeResolutionMode mode
    ) {
        if (sourceKey == null) {
            return Optional.empty();
        }
        var source = findSource(sourceKey, mode);
        if (source.isEmpty() || !source.get().strictColumns()) {
            return Optional.empty();
        }
        var column = source.get().columns().get(normalize(columnName));
        return column == null ? Optional.empty() : Optional.of(column.type());
    }

    /**
     * Returns normalized source keys under selected scope visibility.
     *
     * @param mode scope resolution mode.
     * @return source keys in visibility order.
     */
    private List<String> sourceKeys(ScopeResolutionMode mode) {
        if (mode == ScopeResolutionMode.CURRENT_SCOPE) {
            var scope = currentScope().orElse(null);
            return scope == null ? List.of() : List.copyOf(scope.byAlias.keySet());
        }
        var keys = new LinkedHashSet<String>();
        for (var scope : iterScopes(mode)) {
            keys.addAll(scope.byAlias.keySet());
        }
        return List.copyOf(keys);
    }

    /**
     * Returns scopes according to selected visibility mode.
     *
     * @param mode scope resolution mode.
     * @return iterable scopes.
     */
    private Iterable<Scope> iterScopes(ScopeResolutionMode mode) {
        if (mode == ScopeResolutionMode.CURRENT_SCOPE) {
            var current = currentScope().orElse(null);
            return current == null ? List.of() : List.of(current);
        }
        var boundary = queryIsolations.peek();
        return boundary == null ? scopes : scopes.stream()
            .limit(Math.max(0, scopes.size() - boundary.depth())).toList();
    }

    /**
     * Returns current SELECT scope.
     *
     * @return current scope when available.
     */
    private Optional<Scope> currentScope() {
        return Optional.ofNullable(scopes.peek());
    }

    /**
     * Returns CTE map visible in current WITH scope.
     *
     * @return CTE map keyed by normalized name.
     */
    private Map<String, CteSource> currentCtes() {
        var ctes = cteScopes.peek();
        return ctes == null ? Map.of() : ctes;
    }

    /**
     * Controls whether lookup/inference uses current scope only or all visible scopes.
     */
    private enum ScopeResolutionMode {
        CURRENT_SCOPE,
        ALL_SCOPES
    }

    /**
     * Location of a row-pattern-only expression.
     */
    public enum PatternExpressionKind {
        /**
         * Expression declared by a {@link PatternMeasure}.
         */
        MEASURE,
        /**
         * Predicate declared by a {@link PatternDefinition}.
         */
        DEFINITION
    }

    private record QueryOutput(Map<String, CatalogColumn> columns, boolean known, List<CatalogType> types) {
    }

    private record QueryIsolation(int depth, String path, boolean uncorrelated) {
    }

    private record ModelFrame(ModelScope model, int depth) {
    }

    private record PatternExpressionFrame(PatternExpressionKind kind, String clausePath) {
    }

    /**
     * Table-source scope used by a single SELECT block.
     *
     * @param byAlias              sources indexed by normalized alias.
     * @param sources              sources in declaration order.
     * @param onJoinVisibleAliases per-join ON predicate visible alias sets.
     */
    private record Scope(
        Map<String, ResolvedSource> byAlias,
        List<ResolvedSource> sources,
        Map<OnJoin, Set<String>> onJoinVisibleAliases
    ) {
        /**
         * Creates an empty SELECT scope.
         */
        private Scope() {
            this(new LinkedHashMap<>(), new ArrayList<>(), new IdentityHashMap<>());
        }
    }

    /**
     * Source metadata used for column lookup.
     *
     * @param aliasOrName   source alias/name.
     * @param columns       visible columns by normalized name.
     * @param strictColumns whether unresolved columns should be reported.
     */
    private record ResolvedSource(Identifier aliasOrName, Map<String, CatalogColumn> columns,
                                  boolean strictColumns, boolean quoteAware) {
        /**
         * Creates immutable source metadata.
         *
         * @param aliasOrName   source alias/name.
         * @param columns       source columns.
         * @param strictColumns strict lookup mode.
         * @return source metadata.
         */
        private static ResolvedSource of(Identifier aliasOrName, Map<String, CatalogColumn> columns, boolean strictColumns) {
            return new ResolvedSource(aliasOrName, Collections.unmodifiableMap(new LinkedHashMap<>(columns)), strictColumns, false);
        }

        private CatalogColumn lookup(Identifier name) {
            return columns.get(quoteAware ? ModelScope.key(name) : normalize(name));
        }

        private CatalogColumn lookup(String name) {
            return lookup(Identifier.of(name));
        }
    }

    /**
     * CTE metadata resolved from a WITH definition.
     *
     * @param columns       CTE result columns.
     * @param strictColumns whether column list is explicit and strict.
     * @param quoteAware    whether columns use SQL identifier equality
     */
    private record CteSource(Map<String, CatalogColumn> columns, boolean strictColumns, boolean quoteAware) {
        /**
         * Creates CTE metadata from definition.
         *
         * @param cte CTE definition.
         * @return CTE metadata.
         */
        private static CteSource from(CteDef cte) {
            if (cte.columnAliases() == null || cte.columnAliases().isEmpty()) {
                return new CteSource(Map.of(), false, false);
            }
            var columns = new LinkedHashMap<String, CatalogColumn>(cte.columnAliases().size());
            for (var alias : cte.columnAliases()) {
                columns.put(normalize(alias), CatalogColumn.of(alias.value(), CatalogType.UNKNOWN));
            }
            return new CteSource(Collections.unmodifiableMap(new LinkedHashMap<>(columns)), true, false);
        }
    }
}



