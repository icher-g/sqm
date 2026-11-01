# Parameters and Binding

SQM provides a unified model for representing parameters inside SQL queries and a flexible rendering pipeline that determines how these parameters appear in the final SQL string.
The core idea is that parameterization is **dialect-independent** at the model level and **dialect-dependent** at render time.

The parameterization workflow consists of two parts:

1. **Parameter Expressions in the AST** (`OrdinalParamExpr`, `NamedParamExpr`, `AnonymousParamExpr`)
2. **Parameterization Mode** (`ParameterizationMode`)

---

## Parameter Types in the Model

### **1. Literals**
Any value embedded directly in the AST:

```java
col("age").gt(lit(21))
```

Depending on the parameterization mode, literals may be:
- kept inline (default rendering), or
- converted into bind parameters (Bind mode)

---

### **2. Named Parameters**
Explicit parameters such as:

```sql
WHERE a = :name
```

Represented by:

```java
param("name")
```

---

### **3. Anonymous Parameters**
Parameters without names - the typical `?` placeholder:

```sql
WHERE a = ?
```

Represented internally by:

```java
param()
```

These are typically introduced automatically by the renderer or during parameterization.

---

### Ordinal parameters

`param(1)` constructs an explicit indexed `OrdinalParamExpr`. The dialect owns
its SQL spelling. Named, anonymous, and ordinal helpers all belong to the public
DSL.

## Rendering modes

Controlled by:

```java
RenderOptions.of(parameterizationMode)
```

SQM supports two main strategies:

### **1. `Inline`**
(Default)
Literals stay inline. Explicit parameters use the dialect's placeholder policy;
for example, ANSI rendering can normalize a named model parameter to `?`.

```sql
WHERE age > 21
WHERE a = ? AND b = ?
```

### **2. `Bind`**
Eligible literals are converted to dialect-specific placeholders, and a `SqlText`
is produced with an ordered parameter list. ANSI uses `?`; other dialects can use
different placeholder styles.

This mode is used for JDBC-style parameter binding and safety.

---

## Rendering Example

### Code

```java
var q = select(col("a"), col("b"))
    .from(tbl("t"))
    .where(
        col("a").eq(10).and(col("b").eq("x"))
    )
    .build();

var opts = RenderOptions.of(ParameterizationMode.Bind);

SqlText t = ctx.render(q, opts);
```

### Output

**SQL**

```sql
SELECT a, b
FROM t
WHERE a = ? AND b = ?
```

**Parameters:**

```
[10, "x"]
```

## Model values versus execution values

Render output supplies SQL and ordered literal values. Explicit named/ordinal
parameters are model placeholders; supply their values through your execution
layer. Binding syntax and eligible parameter positions are dialect-specific.
Retain rendering metadata when placeholder normalization changes the SQL spelling;
a text-only round trip does not necessarily preserve the original parameter names.

See [Rendering SQL](Rendering-SQL), [Literal Transforms](Literal-Transforms), and
[SQL Middleware Framework](SQL-Middleware-Framework) for runtime parameterization.
