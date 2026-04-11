/**
 * Supported SQL dialect identifiers.
 */
export type SqlDialect = "ansi" | "postgresql" | "mysql" | "sqlserver";

/**
 * Severity level returned in playground diagnostics.
 */
export type DiagnosticSeverity = "info" | "warning" | "error";

/**
 * Processing phase that produced a playground diagnostic.
 */
export type DiagnosticPhase = "parse" | "render" | "validate" | "transpile";

/**
 * Built-in example payload returned by the playground backend.
 */
export interface ExampleDto {
  id: string;
  title: string;
  dialect: SqlDialect;
  sql: string;
}

/**
 * Structured diagnostic returned by the playground backend.
 */
export interface PlaygroundDiagnosticDto {
  severity: DiagnosticSeverity;
  code: string;
  message: string;
  phase: DiagnosticPhase;
  line: number | null;
  column: number | null;
}

/**
 * Scalar metadata entry attached to an AST node.
 */
export interface AstDetailDto {
  name: string;
  value: string;
}

/**
 * Named child slot in the browser-friendly AST representation.
 */
export interface AstChildSlotDto {
  slot: string;
  multiple: boolean;
  nodes: AstNodeDto[];
}

/**
 * Browser-friendly AST node returned by parse operations.
 */
export interface AstNodeDto {
  nodeType: string;
  nodeInterface: string;
  kind: string | null;
  category: string | null;
  label: string;
  details: AstDetailDto[];
  children: AstChildSlotDto[];
}

/**
 * Summary metadata for a successful parse response.
 */
export interface ParseResponseSummaryDto {
  rootNodeType: string;
  rootInterface: string;
}

/**
 * Parse request payload sent to the playground backend.
 */
export interface ParseRequestDto {
  sql: string;
  dialect: SqlDialect;
}

/**
 * Parse response payload returned by the playground backend.
 */
export interface ParseResponseDto {
  requestId: string;
  success: boolean;
  durationMs: number;
  statementKind: string | null;
  sqmJson: string | null;
  ast: AstNodeDto | null;
  summary: ParseResponseSummaryDto | null;
  diagnostics: PlaygroundDiagnosticDto[];
}

/**
 * Examples response payload returned by the playground backend.
 */
export interface ExamplesResponseDto {
  requestId: string;
  success: boolean;
  durationMs: number;
  diagnostics: PlaygroundDiagnosticDto[];
  examples: ExampleDto[];
}
