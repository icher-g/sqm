import type * as Monaco from "monaco-editor";
import type { SqlDialect } from "../types/api";

interface SqlCompletionSpec {
  label: string;
  kind: "snippet" | "keyword" | "table" | "column" | "alias" | "function";
  detail: string;
  documentation: string;
  insertText: string;
  isSnippet: boolean;
  sortText: string;
}

const modelDialects = new Map<string, SqlDialect>();
let sqlSupportRegistered = false;

/**
 * Registers SQL-specific Monaco helpers used by the playground editor.
 */
export function registerSqlLanguageSupport(monaco: typeof Monaco) {
  if (sqlSupportRegistered) {
    return;
  }

  monaco.languages.registerCompletionItemProvider("sql", {
    triggerCharacters: [" ", ".", "("],
    provideCompletionItems(model, position) {
      const dialect = modelDialects.get(model.uri.toString()) ?? "ansi";
      const sqlBeforeCursor = model.getValueInRange({
        startLineNumber: 1,
        startColumn: 1,
        endLineNumber: position.lineNumber,
        endColumn: position.column
      });
      const word = model.getWordUntilPosition(position);
      const range = {
        startLineNumber: position.lineNumber,
        endLineNumber: position.lineNumber,
        startColumn: word.startColumn,
        endColumn: word.endColumn
      };

      return {
        suggestions: getSqlCompletionSpecs(dialect, model.getValue(), sqlBeforeCursor).map((spec) => ({
          label: spec.label,
          kind: toMonacoCompletionKind(monaco, spec.kind),
          detail: spec.detail,
          documentation: spec.documentation,
          insertText: spec.insertText,
          insertTextRules: spec.isSnippet
            ? monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet
            : undefined,
          range,
          sortText: spec.sortText
        }))
      };
    }
  });

  sqlSupportRegistered = true;
}

/**
 * Associates an editor model with the currently selected source dialect.
 */
export function bindSqlModelDialect(modelUri: string, dialect: SqlDialect) {
  modelDialects.set(modelUri, dialect);
}

/**
 * Builds the SQL completion catalog for the requested dialect.
 */
export function getSqlCompletionSpecs(
  dialect: SqlDialect,
  sqlText = "",
  sqlBeforeCursor = sqlText
): SqlCompletionSpec[] {
  const aliasBindings = extractAliasBindings(sqlText);
  const dotAccessMatch = /([A-Za-z_][\w]*)\.\w*$/i.exec(sqlBeforeCursor);
  if (dotAccessMatch) {
    return getColumnSpecsForReference(dotAccessMatch[1], aliasBindings);
  }

  const wantsTableNames = /\b(from|join|update|into|delete\s+from)\s+[A-Za-z_]*$/i.test(normalizeWhitespace(sqlBeforeCursor));
  const wantsExpressionSuggestions = /\b(select|where|on|having|set|order\s+by|group\s+by|values|returning)\s+.*$/i
    .test(normalizeWhitespace(sqlBeforeCursor));

  const sharedSnippets: SqlCompletionSpec[] = [
    {
      label: "SELECT",
      kind: "snippet",
      detail: "Query snippet",
      documentation: "Insert a SELECT query template.",
      insertText: "select ${1:*}\nfrom ${2:table}\nwhere ${3:condition}",
      isSnippet: true,
      sortText: "01-select"
    },
    {
      label: "INSERT",
      kind: "snippet",
      detail: "DML snippet",
      documentation: "Insert an INSERT statement template.",
      insertText: "insert into ${1:table} (${2:column})\nvalues (${3:value})",
      isSnippet: true,
      sortText: "02-insert"
    },
    {
      label: "UPDATE",
      kind: "snippet",
      detail: "DML snippet",
      documentation: "Insert an UPDATE statement template.",
      insertText: "update ${1:table}\nset ${2:column} = ${3:value}\nwhere ${4:condition}",
      isSnippet: true,
      sortText: "03-update"
    },
    {
      label: "DELETE",
      kind: "snippet",
      detail: "DML snippet",
      documentation: "Insert a DELETE statement template.",
      insertText: "delete from ${1:table}\nwhere ${2:condition}",
      isSnippet: true,
      sortText: "04-delete"
    }
  ];

  const sharedKeywords = [
    "select",
    "from",
    "where",
    "join",
    "left join",
    "inner join",
    "group by",
    "order by",
    "having",
    "insert into",
    "update",
    "delete from",
    "values",
    "set",
    "union",
    "exists"
  ];

  const dialectKeywords: Record<SqlDialect, string[]> = {
    ansi: ["fetch first", "offset", "current date"],
    postgresql: ["distinct on", "ilike", "returning", "nulls first", "nulls last"],
    mysql: ["straight_join", "limit", "replace into", "on duplicate key update"],
    sqlserver: ["top", "output", "with", "offset fetch"]
  };

  const tableSpecs = getTableSpecs();
  const aliasSpecs = getAliasSpecs(aliasBindings);
  const columnSpecs = getColumnSpecs(aliasBindings);
  const functionSpecs = getFunctionSpecs();
  const keywordSpecs = [
    ...sharedKeywords.map((keyword, index) => toKeywordSpec(keyword, `10-shared-${index}`)),
    ...dialectKeywords[dialect].map((keyword, index) => toKeywordSpec(keyword, `20-dialect-${index}`))
  ];

  if (wantsTableNames) {
    return tableSpecs;
  }

  if (wantsExpressionSuggestions) {
    return [...columnSpecs, ...aliasSpecs, ...functionSpecs, ...keywordSpecs];
  }

  return [
    ...sharedSnippets,
    ...tableSpecs,
    ...functionSpecs,
    ...aliasSpecs,
    ...columnSpecs,
    ...keywordSpecs
  ];
}

function toKeywordSpec(keyword: string, sortText: string): SqlCompletionSpec {
  return {
    label: keyword.toUpperCase(),
    kind: "keyword",
    detail: "SQL keyword",
    documentation: `Insert ${keyword.toUpperCase()} for the active dialect.`,
    insertText: keyword,
    isSnippet: false,
    sortText
  };
}

function toMonacoCompletionKind(monaco: typeof Monaco, kind: SqlCompletionSpec["kind"]) {
  switch (kind) {
    case "snippet":
      return monaco.languages.CompletionItemKind.Snippet;
    case "function":
      return monaco.languages.CompletionItemKind.Function;
    case "table":
      return monaco.languages.CompletionItemKind.Struct;
    case "column":
      return monaco.languages.CompletionItemKind.Field;
    case "alias":
      return monaco.languages.CompletionItemKind.Variable;
    case "keyword":
    default:
      return monaco.languages.CompletionItemKind.Keyword;
  }
}

function getTableSpecs(): SqlCompletionSpec[] {
  return Object.keys(BUILT_IN_SCHEMA).map((tableName, index) => ({
    label: tableName,
    kind: "table",
    detail: "Known playground table",
    documentation: `Insert the ${tableName} table name.`,
    insertText: tableName,
    isSnippet: false,
    sortText: `05-table-${index}`
  }));
}

function getAliasSpecs(aliasBindings: Map<string, string>): SqlCompletionSpec[] {
  return Array.from(aliasBindings.entries()).map(([alias, tableName], index) => ({
    label: alias,
    kind: "alias",
    detail: `Alias for ${tableName}`,
    documentation: `Insert the ${alias} alias bound to ${tableName}.`,
    insertText: alias,
    isSnippet: false,
    sortText: `06-alias-${index}`
  }));
}

function getColumnSpecs(aliasBindings: Map<string, string>): SqlCompletionSpec[] {
  const columns = new Map<string, SqlCompletionSpec>();

  for (const [tableName, tableColumns] of Object.entries(BUILT_IN_SCHEMA)) {
    for (const column of tableColumns) {
      columns.set(`${tableName}.${column}`, {
        label: column,
        kind: "column",
        detail: `Column from ${tableName}`,
        documentation: `Insert the ${column} column from ${tableName}.`,
        insertText: column,
        isSnippet: false,
        sortText: `07-column-${tableName}-${column}`
      });
    }
  }

  for (const [alias, tableName] of aliasBindings.entries()) {
    for (const column of BUILT_IN_SCHEMA[tableName] ?? []) {
      columns.set(`${alias}.${column}`, {
        label: `${alias}.${column}`,
        kind: "column",
        detail: `Column via ${alias}`,
        documentation: `Insert the ${column} column through alias ${alias}.`,
        insertText: `${alias}.${column}`,
        isSnippet: false,
        sortText: `04-alias-column-${alias}-${column}`
      });
    }
  }

  return Array.from(columns.values());
}

function getColumnSpecsForReference(reference: string, aliasBindings: Map<string, string>): SqlCompletionSpec[] {
  const tableName = aliasBindings.get(reference) ?? reference;
  return (BUILT_IN_SCHEMA[tableName] ?? []).map((column, index) => ({
    label: column,
    kind: "column",
    detail: `Column on ${reference}`,
    documentation: `Insert the ${column} column for ${reference}.`,
    insertText: column,
    isSnippet: false,
    sortText: `01-qualified-${index}`
  }));
}

function getFunctionSpecs(): SqlCompletionSpec[] {
  return [
    {
      label: "count(...)",
      kind: "function",
      detail: "Aggregate function",
      documentation: "Insert a COUNT aggregate call.",
      insertText: "count(${1:*})",
      isSnippet: true,
      sortText: "03-function-count"
    },
    {
      label: "sum(...)",
      kind: "function",
      detail: "Aggregate function",
      documentation: "Insert a SUM aggregate call.",
      insertText: "sum(${1:expression})",
      isSnippet: true,
      sortText: "03-function-sum"
    },
    {
      label: "coalesce(...)",
      kind: "function",
      detail: "Scalar function",
      documentation: "Insert a COALESCE scalar call.",
      insertText: "coalesce(${1:value}, ${2:fallback})",
      isSnippet: true,
      sortText: "03-function-coalesce"
    },
    {
      label: "lower(...)",
      kind: "function",
      detail: "Scalar function",
      documentation: "Insert a LOWER scalar call.",
      insertText: "lower(${1:value})",
      isSnippet: true,
      sortText: "03-function-lower"
    }
  ];
}

function extractAliasBindings(sqlText: string): Map<string, string> {
  const bindings = new Map<string, string>();
  const normalizedSql = normalizeWhitespace(sqlText);
  const pattern = /\b(?:from|join|update|into)\s+([A-Za-z_][\w]*)\s+(?:as\s+)?([A-Za-z_][\w]*)/gi;

  for (const match of normalizedSql.matchAll(pattern)) {
    const tableName = match[1]?.toLowerCase();
    const alias = match[2]?.toLowerCase();
    if (tableName && alias) {
      bindings.set(alias, tableName);
    }
  }

  return bindings;
}

function normalizeWhitespace(value: string): string {
  return value.replace(/\s+/g, " ").trim();
}

const BUILT_IN_SCHEMA: Record<string, string[]> = {
  customer: ["id", "name", "vip"],
  orders: ["id", "customer_id", "status"],
  users: ["id", "first_name", "last_name"]
};
