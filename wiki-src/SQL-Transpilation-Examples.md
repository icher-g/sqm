# SQL Transpilation Examples

This page collects practical transpilation examples that match the currently implemented feature set.

## Exact PostgreSQL -> MySQL Conversion

Source SQL:

```sql
SELECT first_name || ' ' || last_name AS full_name
FROM users
```

Target SQL:

```sql
SELECT CONCAT(first_name, ' ', last_name) AS full_name
FROM users
```

## Approximate PostgreSQL -> MySQL Conversion

Source SQL:

```sql
SELECT *
FROM users
WHERE name ILIKE 'al%'
```

Target SQL:

```sql
SELECT *
FROM users
WHERE LOWER(name) LIKE LOWER('al%')
```

Expected warning:

- `APPROXIMATE_ILIKE_LOWERING`

## Exact MySQL -> PostgreSQL Conversion

Source SQL:

```sql
SELECT *
FROM users
WHERE first_name <=> last_name
```

Target SQL:

```sql
SELECT *
FROM users
WHERE first_name IS NOT DISTINCT FROM last_name
```

## Warning-Based MySQL -> non-MySQL Conversion

Source SQL:

```sql
SELECT /*+ MAX_EXECUTION_TIME(1000) */ *
FROM users USE INDEX (idx_users_name)
```

Target SQL:

```sql
SELECT *
FROM users
```

Expected warning:

- `MYSQL_HINTS_DROPPED`

The same warning-based drop policy applies for any non-MySQL target dialect.

## Warning-Based SQL Server -> non-SQL Server Conversion

Source SQL:

```sql
SELECT [u].[id]
FROM [users] AS [u] WITH (NOLOCK)
```

Target SQL:

```sql
SELECT u.id
FROM users AS u
```

Expected warning:

- `SQLSERVER_HINTS_DROPPED`

The same warning-based drop policy applies for any non-SQL Server target dialect.

## Unsupported Example

Source SQL:

```sql
INSERT INTO users (name) VALUES ('alice') RETURNING id
```

Current PostgreSQL -> MySQL result:

- status: `UNSUPPORTED`
- problem code: `UNSUPPORTED_RETURNING`

## Related Example Classes

- `examples/src/main/java/io/sqm/examples/Transpile_PostgresToMySql.java`
- `examples/src/main/java/io/sqm/examples/Transpile_PostgresToMySqlApproximate.java`
