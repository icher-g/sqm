/**
 * Supported SQL dialect identifiers.
 */
export type SqlDialect = "ansi" | "postgresql" | "mysql" | "sqlserver";

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
 * Examples response payload returned by the playground backend.
 */
export interface ExamplesResponseDto {
  requestId: string;
  success: boolean;
  durationMs: number;
  diagnostics: Array<{
    severity: string;
    code: string;
    message: string;
    phase: string;
    line: number | null;
    column: number | null;
  }>;
  examples: ExampleDto[];
}
