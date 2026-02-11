# SQM Development Rules

**Document Purpose**: Comprehensive guidelines for developing new features in the SQM (Structured Query Model) project.  
**Created**: 2026-02-02  
**Based On**: Implementation of PostgreSQL AT TIME ZONE feature (28 tests, 8 new files, 10 modified files)

---

## Core Architecture Principles

### Repository-Wide Baseline Rules (Applies to Any Feature)

1. Model-first design: introduce/update core model nodes before parser/renderer/DSL wiring.
2. One node, full lifecycle: every node must have parser, renderer, visitor/transformer, match API, JSON mixins, and docs coverage.
3. Visitor-first traversal: rely on recursive base visitors/transformers and override only node-specific behavior.
4. Split by concern: prefer focused classes (rules/handlers/components) over monoliths.
5. Registry-based composition: compose behavior via registries/factories, not hardcoded branching.
6. Extensibility by contracts: use interfaces/settings for future dialect/features instead of tight coupling.
7. Stable defaults: new extension points must preserve existing behavior unless explicitly enabled.
8. DSL readability first: prefer user-friendly DSL APIs; avoid direct `Impl` usage in user-facing code.
9. Deterministic outputs: keep generation/rendering stable (ordering, naming, formatting).
10. Diagnostic quality: emit actionable errors with code, message, node kind, and clause/path context.
11. Test every behavior: add happy-path, error-path, and boundary/edge tests for each new behavior.
12. Documentation is part of done: update README and feature docs whenever behavior/API changes.
13. Repository hygiene: avoid unrelated changes (including IDE files) and keep commits scoped.
14. Compatibility by intent: keep compatibility for established features; for new features, prefer simple clean design unless compatibility is explicitly required.

### Principle 1: Sealed Interfaces + Immutable Records
Every AST node is a sealed interface with a record implementation:
- Compile-time exhaustiveness checking
- Immutability guaranteed
- Type safety across codebase

```java
public sealed interface AtTimeZoneExpr extends Expression { ... }
public record AtTimeZoneExprImpl(Expression timestamp, Expression timezone) implements AtTimeZoneExpr { ... }
```

### Principle 2: Factory Methods Over Constructors
All creation goes through static factory methods, never direct instantiation:
```
✅ AtTimeZoneExpr.of(timestamp, timezone)
❌ new AtTimeZoneExprImpl(timestamp, timezone)
```

### Principle 3: Feature-Aware Design
SQL features are controlled by feature flags, validated at **both** parse AND render time:
- Feature enum: `SqlFeature`
- Parser validation: `ctx.capabilities().supports(SqlFeature.XXX)`
- Renderer validation: `ctx.dialect().capabilities().supports(SqlFeature.XXX)`

### Principle 4: Module Isolation
Strict module boundaries:
```
sqm-core (models only, no parser/renderer imports)
  ├─ sqm-parser-ansi (dialect-specific parsing)
  ├─ sqm-parser-postgresql (dialect-specific parsing)
  ├─ sqm-render-ansi (dialect-specific rendering)
  ├─ sqm-render-postgresql (dialect-specific rendering)
  ├─ sqm-json (serialization)
  └─ sqm-it (integration tests)
```

### Principle 5: Three Complementary Patterns
Every new node type must implement:
1. **Visitor Pattern** (RecursiveNodeVisitor) - for analysis/traversal
2. **Transformer Pattern** (RecursiveNodeTransformer) - for query rewriting
3. **Match API** (ExpressionMatch, etc.) - for type-safe dispatching

Missing any one is incomplete.

---

## The 12 Development Rules

### Rule 1: The "Complete Pattern" Rule ⭐ CRITICAL

**When adding a new expression/predicate/join type, implement ALL of:**

- [ ] Sealed interface + Impl record in sqm-core
- [ ] Add `visit<Type>()` to NodeVisitor interface
- [ ] Implement `visit<Type>()` in RecursiveNodeVisitor
- [ ] Add `visit<Type>()` to RecursiveNodeTransformer with proper logic
- [ ] Add match method to corresponding Match interface (e.g., ExpressionMatch)
- [ ] Implement match method in Impl class (e.g., ExpressionMatchImpl)
- [ ] Create parser(s) for each supported dialect
- [ ] Create renderer(s) for each supported dialect with feature validation
- [ ] Add node to parent sealed interface `permits` clause
- [ ] Add DSL helper in Dsl.java if appropriate
- [ ] Create comprehensive tests (minimum 13 tests - see Rule 5)
- [ ] Update MODEL.md if new top-level node type

**Violation Impact**: Feature is incomplete; users encounter friction and missing functionality.

---

### Rule 2: The "Feature Flag" Rule ⭐ CRITICAL

**Every dialect-specific SQL construct MUST have a feature flag:**

1. Add to SqlFeature enum:
```java
public enum SqlFeature {
    NEW_FEATURE("Feature Name"),
    ...
}
```

2. Implement in Specs class:
```java
// AnsiSpecs.java
public boolean supports(SqlFeature feature) {
    return switch(feature) {
        case NEW_FEATURE -> false; // ANSI doesn't support
        ...
    };
}

// PostgresSpecs.java
public boolean supports(SqlFeature feature) {
    return switch(feature) {
        case NEW_FEATURE -> true; // PostgreSQL 9.0+ supports
        ...
    };
}
```

3. Validate in parser:
```java
if (!ctx.capabilities().supports(SqlFeature.NEW_FEATURE)) {
    throw new FeatureNotSupportedException(
        "Feature not supported in dialect: " + ctx.capabilities().dialect());
}
```

4. Validate in renderer:
```java
if (!ctx.dialect().capabilities().supports(SqlFeature.NEW_FEATURE)) {
    throw new UnsupportedOperationException(
        "Feature not supported in dialect: " + ctx.dialect().name());
}
```

**Violation Impact**: Silent failures, confusing error messages, dialect constraints invisible to users.

---

### Rule 3: The "Immutability Contract" Rule ⭐ CRITICAL

**Transformers MUST NOT mutate nodes; they must return new instances.**

Correct pattern:
```java
@Override
public Node visitMyExpr(MyExpr expr) {
    Node newChild1 = apply(expr.child1());
    Node newChild2 = apply(expr.child2());
    
    // If no changes, return original (idempotent)
    if (newChild1 == expr.child1() && newChild2 == expr.child2()) {
        return expr;
    }
    
    // If changes detected, return NEW instance
    return MyExpr.of(newChild1, newChild2);
}
```

**Violation Impact**: Breaks immutability guarantees, causes hard-to-debug state issues, breaks transformer composability.

---

### Rule 4: The "Factory Only" Rule

**Never instantiate `Impl` classes directly. Always use factory methods.**

```
✅ ColumnExpr.of("name")
✅ AtTimeZoneExpr.of(timestamp, timezone)
✅ Select.query(expressions)

❌ new ColumnExprImpl("name")
❌ new AtTimeZoneExprImpl(timestamp, timezone)
```

**Why**: Allows future optimizations (object pooling, caching, lazy initialization) without breaking public APIs.

---

### Rule 5: The "Comprehensive Test" Rule

**Minimum test requirements for any new node type:**

| Component | Min Tests | Required Coverage |
|-----------|-----------|-------------------|
| Model (core) | 1+ | Factory method, getters, equality |
| Visitor | 2+ | Method invoked, children recursively visited |
| Transformer | 2+ | No-change → same instance, with-change → new instance |
| Match API | 4+ | Match, non-match, all orElse variants, field extraction |
| Parser | 2+ | Valid parse, invalid parse (error path), feature validation |
| Renderer | 2+ | Valid render, feature validation (unsupported), formatting |
| **Total Minimum** | **13+** | **All paths covered** |

**Test File Location Convention:**
```
sqm-core/src/test/java/io/sqm/core/walk/MyExprVisitorTest.java
sqm-core/src/test/java/io/sqm/core/walk/MyExprTransformerTest.java
sqm-core/src/test/java/io/sqm/core/match/MyExprMatchTest.java
sqm-parser-ansi/src/test/java/io/sqm/parser/ansi/MyExprParserTest.java
sqm-render-ansi/src/test/java/io/sqm/render/ansi/MyExprRendererTest.java
```

---

### Rule 6: The "Feature Validation First" Rule

**When implementing parsers/renderers for unsupported features:**

1. Check feature support BEFORE attempting to parse/render
2. For parser return error(`message`, cur.fullPos())
3. For renderer throw exception: `UnsupportedOperationException`
4. Add test cases documenting what's rejected and why

Example test:
```java
@Test
void parse_ansi_featureNotSupported() {
    var ctx = ParseContext.of(new AnsiSpecs());
    var result = ctx.parse(Query.class, "SELECT ts AT TIME ZONE 'UTC'");
    
    assertFalse(result.ok());
    assertTrue(result.errorMessage().contains("AT_TIME_ZONE"));
}
```

**Why**: Clear error messages help users debug and understand dialect constraints.

---

### Rule 7: The "No Cross-Module Model Pollution" Rule

**sqm-core models must not import from parser or renderer modules.**

```
✅ sqm-core → sqm-core (allowed)
✅ sqm-parser-* → sqm-core (allowed)
✅ sqm-render-* → sqm-core (allowed)

❌ sqm-core → sqm-parser-* (forbidden)
❌ sqm-core → sqm-render-* (forbidden)
```

**Why**: Maintains clean dependency graph and allows parser/renderer implementations to be swapped without rebuilding core.

---

### Rule 8: The "JavaDoc Everywhere" Rule

**Every public class, interface, method, and parameter must have JavaDoc.**

Minimum template:
```java
/**
 * Represents a {@code AT TIME ZONE} expression that converts a timestamp
 * to a different time zone.
 *
 * <p>Example SQL:</p>
 * <pre>
 * SELECT ts AT TIME ZONE 'UTC' FROM events
 * </pre>
 *
 * @param timestamp the timestamp expression to convert (must not be null)
 * @param timezone the target timezone expression (must not be null)
 * @return a new {@link AtTimeZoneExpr} instance
 * @throws NullPointerException if timestamp or timezone is null
 */
public static AtTimeZoneExpr of(Expression timestamp, Expression timezone) {
    Objects.requireNonNull(timestamp);
    Objects.requireNonNull(timezone);
    return new AtTimeZoneExprImpl(timestamp, timezone);
}
```

**Why**: Project is a library; users need to understand the API without reading implementation.

---

### Rule 9: The "Error Path Testing" Rule

**Test both happy paths AND error scenarios:**

- ✅ What happens when feature is NOT supported in dialect
- ✅ What happens with null/invalid inputs
- ✅ What happens with nested expressions
- ✅ What happens with complex expressions (functions, arithmetic, etc.)
- ✅ What happens with edge cases (empty, single element, etc.)

Checklist for new parser:
```
- [ ] Valid simple case
- [ ] Valid complex case (nested expressions)
- [ ] Feature not supported → FeatureNotSupportedException
- [ ] Invalid syntax → ParseError
- [ ] Edge cases (empty, single, boundary values)
```

---

### Rule 10: The "Integration Test Anchor" Rule

**For any new major feature, add at least one round-trip integration test in sqm-it:**

```java
@Test
void roundTripSelectAtTimeZone() {
    String originalSql = "SELECT ts AT TIME ZONE 'UTC' AS utc_time FROM events";
    
    var parseResult = parseContext.parse(Query.class, originalSql);
    assertTrue(parseResult.ok(), parseResult.errorMessage());
    
    var query = parseResult.value();
    var renderedSql = renderContext.render(query).sql();
    
    // Normalize whitespace (renderer produces formatted SQL)
    String normalized1 = originalSql.replaceAll("\\s+", " ").trim();
    String normalized2 = renderedSql.replaceAll("\\s+", " ").trim();
    
    assertEquals(normalized1, normalized2);
}
```

**Why**: Catches bugs that unit tests miss (e.g., parser missing a field, renderer missing a clause).

---

### Rule 11: The "Registration Verification" Rule

**After creating a new parser/renderer, verify it's registered:**

Checklist:
- [ ] Check `Parsers.java` for `.register(new MyExprParser())`
- [ ] Check `Renderers.java` for `.register(new MyExprRenderer())`
- [ ] Check parent parser has integration point (e.g., PostfixExprParser adds to loop)
- [ ] Verify registration matches expected node type
- [ ] Build and verify no "unregistered parser" errors

Common locations:
```
sqm-parser-ansi/src/main/java/io/sqm/parser/ansi/Parsers.java
sqm-render-ansi/src/main/java/io/sqm/render/ansi/Renderers.java
sqm-parser-postgresql/src/main/java/io/sqm/parser/postgresql/Parsers.java
sqm-render-postgresql/src/main/java/io/sqm/render/postgresql/Renderers.java
```

**Why**: Missing registration = silent failure (feature exists but can't be used).

---

### Rule 12: The "Match API Consistency" Rule

**Every new expression/predicate type should have a corresponding Match method.**

Implementation pattern:

1. **Add to Match interface**:
```java
// In ExpressionMatch.java
<R> R atTimeZone(Function<AtTimeZoneExpr, R> handler);
```

2. **Implement in Impl class**:
```java
// In ExpressionMatchImpl.java
@Override
public <R> ExpressionMatch<R> atTimeZone(Function<AtTimeZoneExpr, R> handler) {
    if (!matched && expr instanceof AtTimeZoneExpr e) {
        matched = true;
        result = handler.apply(e);
    }
    return this;
}
```

3. **Test with pattern matching**:
```java
String tz = Match
    .<String>expression(expr)
    .atTimeZone(e -> e.timezone().toString())
    .orElse("DEFAULT");
```

**Why**: Enables fluent, type-safe pattern matching instead of instanceof chains.

---

## Project-Specific Patterns

### Pattern A: The Standard Visitor Flow
```
node.accept(visitor) 
  → visitor.visitMyExpr(node) 
  → return result
  → super.visitMyExpr(node) [calls defaultResult()]
```

Every node type needs explicit visitor methods; no generic fallback.

### Pattern B: The Sealed Hierarchy
```java
public sealed interface Expression extends Node 
    permits CaseExpr, ColumnExpr, AtTimeZoneExpr, FunctionExpr, ... { }

public sealed interface AtTimeZoneExpr extends Expression { ... }
```

Use sealing at every level to prevent unexpected subtypes.

### Pattern C: The Feature Validation Sandwich

**Parse Time:**
1. Match/detect syntax (MatchableParser.match())
2. Validate feature supported
3. Parse expression
4. Return ParseResult

**Render Time:**
1. Validate feature supported
2. Render SQL
3. Return SqlText

Both layers validate independently.

### Pattern D: The DSL Fluent Builder

```java
col("ts")
    .atTimeZone(lit("UTC"))
    .as("utc_time")
```

DSL methods should:
- Return `this` or new instances for method chaining
- Keep names short (col, tbl, func, lit, etc.)
- Provide convenience overloads for common cases

---

## Common Pitfalls to Avoid

| Pitfall | Impact | Solution |
|---------|--------|----------|
| Mutating records in transformer | Breaks immutability contract | Always return new instance |
| Missing feature validation | Silent failures | Validate in parser AND renderer |
| Parser without renderer | Half-implemented feature | Pair them always |
| Missing Match method | Users forced to use instanceof | Add to Match interface + Impl |
| Forgetting registration | Parser/renderer not called | Double-check Parsers.java, Renderers.java |
| Whitespace in test assertions | False failures in tests | Always normalize: `sql.replaceAll("\\s+", " ").trim()` |
| ParseResult API confusion | Wrong field accessed | Use `.ok()`, `.value()`, `.errorMessage()` (NOT `isOk()`) |
| Numeric literals as Integer | Type mismatch | Parser returns Long, not Integer; use `100L` in assertions |
| Direct Impl instantiation | Violates factory pattern | Use `Type.of(...)` only |
| Missing JavaDoc | Library unusable | Document all public APIs |
| Recursive visitor incomplete | Traversal stops early | Ensure all children are visited with `accept()` |
| Transformer doesn't recurse | Changes not propagated | Call `accept()` on all children |
| Registered parser never called | Parser silently ignored | Check MatchableParser.match() logic and order in loop |
| Match method in wrong interface | Method not found | Add to correct Match type (Expression, Predicate, etc.) |

---

## Feature Development Workflow

### Step-by-Step Checklist

**Phase 1: Design** (30 min)
- [ ] Identify node type (Expression? Predicate? Join? etc.)
- [ ] Map where in sealed hierarchy
- [ ] Identify which dialects support it
- [ ] Sketch parser/renderer approach
- [ ] Identify DSL helper naming

**Phase 2: Model** (30 min)
- [ ] Create interface in sqm-core
- [ ] Create Impl record in sqm-core/internal
- [ ] Implement accept() method
- [ ] Add factory method (of())
- [ ] Add permits clause to parent interface
- [ ] Add getters/accessors

**Phase 3: Visitor/Transformer/Match** (1 hour)
- [ ] Add visitXxx() to NodeVisitor
- [ ] Implement in RecursiveNodeVisitor
- [ ] Implement visitXxx() in RecursiveNodeTransformer
- [ ] Add xxxMatch() to appropriate Match interface
- [ ] Implement in corresponding MatchImpl

**Phase 4: Feature Flag** (15 min)
- [ ] Add feature to SqlFeature enum
- [ ] Update AnsiSpecs (usually false)
- [ ] Update PostgresSpecs (usually true for PG features)
- [ ] Update other dialect specs as needed

**Phase 5: Parser** (2 hours)
- [ ] Create XxxParser class implementing MatchableParser/InfixParser
- [ ] Implement match() method (token detection)
- [ ] Implement parse() method (with feature validation)
- [ ] Register in Parsers.java
- [ ] Integrate in parent parser (e.g., PostfixExprParser)
- [ ] Write 2+ parser tests

**Phase 6: Renderer** (1 hour)
- [ ] Create XxxRenderer implementing Renderer<Xxx>
- [ ] Implement render() method (with feature validation)
- [ ] Register in Renderers.java
- [ ] Write 2+ renderer tests

**Phase 7: DSL Helper** (15 min)
- [ ] Add method to Dsl.java if appropriate
- [ ] Keep name short
- [ ] Add JavaDoc

**Phase 8: Testing** (2 hours)
- [ ] Write 1+ core model tests
- [ ] Write 2+ visitor tests
- [ ] Write 2+ transformer tests
- [ ] Write 4+ match tests
- [ ] Write 2+ parser tests (happy + error path)
- [ ] Write 2+ renderer tests (happy + error path)
- [ ] Total: 13+ tests

**Phase 9: Integration** (30 min)
- [ ] Add round-trip test in sqm-it
- [ ] Verify full project test suite passes
- [ ] Clean build: `mvn clean install`

**Phase 10: Documentation** (30 min)
- [ ] JavaDoc all public classes/methods
- [ ] Update MODEL.md if top-level node type
- [ ] Add usage examples in JavaDoc

**Total Effort**: 6-8 hours for average feature (like AT TIME ZONE)

---

## Quick Reference Checklists

### Checklist: Adding a New Expression Type

**Implementation**:
- [ ] Create interface + Impl record in sqm-core
- [ ] Implement accept(NodeVisitor<R> v)
- [ ] Create factory method Type.of(...)
- [ ] Add to Expression permits clause
- [ ] Add feature flag if dialect-specific

**Patterns**:
- [ ] Add visitMyExpr() to NodeVisitor
- [ ] Implement in RecursiveNodeVisitor
- [ ] Implement in RecursiveNodeTransformer
- [ ] Add myExpr() to ExpressionMatch
- [ ] Implement in ExpressionMatchImpl

**Parser** (per dialect):
- [ ] Create MyExprParser
- [ ] Implement MatchableParser<MyExpr>
- [ ] Implement match() for syntax detection
- [ ] Implement parse() with feature validation
- [ ] Register in Parsers.java
- [ ] Integrate in parent parser
- [ ] Write tests (valid, invalid, feature validation)

**Renderer** (per dialect):
- [ ] Create MyExprRenderer
- [ ] Implement Renderer<MyExpr>
- [ ] Implement render() with feature validation
- [ ] Register in Renderers.java
- [ ] Write tests (valid, feature validation)

**DSL** (if appropriate):
- [ ] Add helper method in Dsl.java
- [ ] Keep name short
- [ ] Add JavaDoc

**Tests**: Minimum 13 tests covering all above

**Verification**:
- [ ] All unit tests pass
- [ ] Full project test suite passes
- [ ] Clean build succeeds
- [ ] Registration verified

---

### Checklist: Before Committing Code

- [ ] All tests pass locally: `mvn test`
- [ ] Clean build: `mvn clean install -DskipTests`
- [ ] No compilation warnings (except JDK deprecation)
- [ ] All public APIs have JavaDoc
- [ ] No cross-module model pollution (sqm-core clean)
- [ ] All Impl classes instantiated via factory methods only
- [ ] Feature flags validated in parser AND renderer
- [ ] Transformers return new instances, not mutations
- [ ] All sealed interfaces have permits clause updated
- [ ] Parsers registered in Parsers.java
- [ ] Renderers registered in Renderers.java
- [ ] Match methods added to appropriate Match interface
- [ ] Integration tests added to sqm-it
- [ ] MODEL.md updated if new top-level type
- [ ] Error paths tested (not just happy path)

---

## Testing Patterns

### Parser Test Template
```java
@Test
void parse_validExpression() {
    var result = parseContext.parse(Predicate.class, "a IS DISTINCT FROM b");
    assertTrue(result.ok(), result.errorMessage());
    assertInstanceOf(IsDistinctFromPredicate.class, result.value());
}

@Test
void parse_ansi_featureNotSupported() {
    var ctx = ParseContext.of(new AnsiSpecs());
    var result = ctx.parse(Query.class, "SELECT ts AT TIME ZONE 'UTC'");
    assertFalse(result.ok());
    assertTrue(result.errorMessage().contains("AT_TIME_ZONE"));
}
```

### Renderer Test Template
```java
@Test
void render_validExpression() {
    var expr = col("ts").atTimeZone(lit("UTC"));
    var ctx = RenderContext.of(new AnsiDialect());
    var sql = ctx.render(expr).sql();
    assertTrue(sql.contains("AT TIME ZONE"));
}

@Test
void render_featureNotSupported() {
    var expr = col("ts").atTimeZone(lit("UTC"));
    var ctx = RenderContext.of(new AnsiDialect());
    assertThrows(UnsupportedOperationException.class, 
        () -> ctx.render(expr).sql());
}
```

### Visitor Test Template
```java
@Test
void visitAtTimeZoneExpr_isInvoked() {
    var visitor = new RecursiveNodeVisitor<Void>() {
        private boolean visited = false;
        
        @Override
        public Void visitAtTimeZoneExpr(AtTimeZoneExpr expr) {
            visited = true;
            return super.visitAtTimeZoneExpr(expr);
        }
        
        @Override
        protected Void defaultResult() {
            return null;
        }
    };
    
    var expr = col("ts").atTimeZone(lit("UTC"));
    expr.accept(visitor);
    assertTrue(visitor.visited);
}
```

### Transformer Test Template
```java
@Test
void transform_preservesUnchangedExpr() {
    var expr = col("ts").atTimeZone(lit("UTC"));
    var transformer = new RecursiveNodeTransformer() { };
    
    Node result = transformer.transform(expr);
    
    assertSame(expr, result); // Same instance when no changes
}

@Test
void transform_createsNewInstance() {
    var expr = col("ts").atTimeZone(lit("UTC"));
    var transformer = new RecursiveNodeTransformer() {
        @Override
        public Node visitAtTimeZoneExpr(AtTimeZoneExpr e) {
            return AtTimeZoneExpr.of(
                col("new_ts"),
                e.timezone()
            );
        }
    };
    
    Node result = transformer.transform(expr);
    
    assertNotSame(expr, result); // New instance when changed
    assertInstanceOf(AtTimeZoneExpr.class, result);
}
```

### Match Test Template
```java
@Test
void atTimeZone_matches() {
    var expr = col("ts").atTimeZone(lit("UTC"));
    
    String result = Match
        .<String>expression(expr)
        .atTimeZone(e -> "matched")
        .orElse("not-matched");
    
    assertEquals("matched", result);
}

@Test
void atTimeZone_doesNotMatch() {
    var expr = col("name");
    
    String result = Match
        .<String>expression(expr)
        .atTimeZone(e -> "matched")
        .orElse("not-matched");
    
    assertEquals("not-matched", result);
}
```

---

## References

- **Model Hierarchy**: docs/MODEL.md
- **README**: README.md (contains examples and overview)
- **Project Structure**: AGENTS.md (agent development guidelines)
- **AT TIME ZONE Example**: See implementation files for comprehensive reference

---

## Document History

| Date | Event |
|------|-------|
| 2026-02-02 | Initial version created from AT TIME ZONE implementation experience |

---

**Last Updated**: 2026-02-02  
**Author**: Development AI Agent  
**Status**: Active Guidelines
