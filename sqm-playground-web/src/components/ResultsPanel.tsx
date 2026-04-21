import { useRef, useState } from "react";
import type {
  ParseResponseDto,
  PlaygroundDiagnosticDto,
  RenderResponseDto,
  SqlDialect,
  TranspileResponseDto,
  ValidateResponseDto
} from "../types/api";
import { AstNodeTree, type AstTreeCommand } from "./AstNodeTree";
import { CodeBlock } from "./CodeBlock";
import { JsonTreeViewer, type JsonTreeCommand } from "./JsonTreeViewer";

export type ResultTab = "ast" | "dsl" | "json" | "renderedSql" | "diagnostics" | "about";

interface ResultsPanelProps {
  activeResultTab: ResultTab;
  onResultTabChange: (nextTab: ResultTab) => void;
  parseResponse: ParseResponseDto | null;
  parseLoading: boolean;
  parseError: string | null;
  renderResponse: RenderResponseDto | null;
  renderedSqlDialect: SqlDialect | null;
  renderedSqlTimestamp: number | null;
  renderLoading: boolean;
  renderError: string | null;
  transpileResponse: TranspileResponseDto | null;
  transpileLoading: boolean;
  transpileError: string | null;
  validateResponse: ValidateResponseDto | null;
  validateLoading: boolean;
  validateError: string | null;
  onDiagnosticSelect: (diagnostic: PlaygroundDiagnosticDto) => void;
}

/**
 * Renders the tabbed results shell.
 */
export function ResultsPanel(props: ResultsPanelProps) {
  const [astCommand, setAstCommand] = useState<AstTreeCommand | null>(null);
  const [jsonCommand, setJsonCommand] = useState<JsonTreeCommand | null>(null);
  const [parseView, setParseView] = useState<"sequence" | number>("sequence");
  const [copiedTarget, setCopiedTarget] = useState<"ast" | "json" | null>(null);
  const copyResetTimeoutRef = useRef<number | null>(null);
  const statementViews = props.parseResponse ? deriveStatementViews(props.parseResponse) : [];
  const selectedStatement = typeof parseView === "number"
    ? statementViews.find((statement) => statement.index === parseView) ?? null
    : null;
  const selectedAst = selectedStatement?.ast ?? props.parseResponse?.ast ?? null;
  const selectedDsl = selectedStatement?.sqmDsl ?? props.parseResponse?.sqmDsl ?? null;
  const selectedJson = selectedStatement?.sqmJson ?? props.parseResponse?.sqmJson ?? null;
  const hasParseViewChoices = Boolean(props.parseResponse?.multiStatement && statementViews.length > 1);
  const renderParams = props.renderResponse?.params ?? [];
  const transpileParams = props.transpileResponse?.params ?? [];

  function runAstCommand(type: AstTreeCommand["type"]) {
    setAstCommand((current) => ({
      type,
      version: (current?.version ?? 0) + 1
    }));
  }

  function runJsonCommand(type: JsonTreeCommand["type"]) {
    setJsonCommand((current) => ({
      type,
      version: (current?.version ?? 0) + 1
    }));
  }

  async function copyText(text: string, target: "ast" | "json") {
    if (!navigator.clipboard) {
      return;
    }

    await navigator.clipboard.writeText(text);
    setCopiedTarget(target);

    if (copyResetTimeoutRef.current !== null) {
      window.clearTimeout(copyResetTimeoutRef.current);
    }

    copyResetTimeoutRef.current = window.setTimeout(() => {
      setCopiedTarget(null);
      copyResetTimeoutRef.current = null;
    }, 1500);
  }

  return (
    <article className="card results-card">
      <div className="panel-heading">
        <h2>Results</h2>
        <p>AST, DSL, JSON, SQL, diagnostics</p>
      </div>

      <div className="tab-row" role="tablist" aria-label="Result tabs">
        <button
          type="button"
          role="tab"
          aria-selected={props.activeResultTab === "ast"}
          className={props.activeResultTab === "ast" ? "tab-button tab-button-active" : "tab-button"}
          title="Show the parsed SQM abstract syntax tree."
          onClick={() => props.onResultTabChange("ast")}
        >
          AST
        </button>
        <button
            type="button"
            role="tab"
            aria-selected={props.activeResultTab === "dsl"}
            className={props.activeResultTab === "dsl" ? "tab-button tab-button-active" : "tab-button"}
            title="Show generated Java DSL for the parsed SQM model."
            onClick={() => props.onResultTabChange("dsl")}
        >
          DSL
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={props.activeResultTab === "json"}
          className={props.activeResultTab === "json" ? "tab-button tab-button-active" : "tab-button"}
          title="Show the parsed SQM model as JSON."
          onClick={() => props.onResultTabChange("json")}
        >
          JSON
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={props.activeResultTab === "renderedSql"}
          className={props.activeResultTab === "renderedSql" ? "tab-button tab-button-active" : "tab-button"}
          title="Show SQL produced by render or transpile actions."
          onClick={() => props.onResultTabChange("renderedSql")}
        >
          Rendered SQL
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={props.activeResultTab === "diagnostics"}
          className={props.activeResultTab === "diagnostics" ? "tab-button tab-button-active" : "tab-button"}
          title="Show warnings and errors returned by the last operation."
          onClick={() => props.onResultTabChange("diagnostics")}
        >
          Diagnostics
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={props.activeResultTab === "about"}
          className={props.activeResultTab === "about" ? "tab-button tab-button-active" : "tab-button"}
          title="Show metadata for the latest playground operations."
          onClick={() => props.onResultTabChange("about")}
        >
          About Result
        </button>
      </div>

      <section
        className="result-panel result-panel-scroll"
        role="tabpanel"
        aria-label="AST"
        hidden={props.activeResultTab !== "ast"}
      >
          <div className="result-panel-header">
            <h3>AST</h3>
            <div className="result-panel-actions">
              {hasParseViewChoices ? (
                <select
                  className="statement-view-select"
                  value={parseView}
                  onChange={(event) => setParseView(event.target.value === "sequence" ? "sequence" : Number(event.target.value))}
                  aria-label="AST statement view"
                >
                  <option value="sequence">All statements</option>
                  {statementViews.map((statement) => (
                    <option key={statement.index} value={statement.index}>
                      Statement {statement.index}: {statement.label}
                    </option>
                  ))}
                </select>
              ) : null}
              {selectedAst ? (
              <div className="ast-toolbar">
                <button
                  type="button"
                  className="ast-toolbar-button"
                  title="Copy the current AST view as plain text."
                  onClick={() => copyText(formatAstTree(selectedAst), "ast")}
                >
                  {copiedTarget === "ast" ? "Copied AST" : "Copy AST"}
                </button>
                <button
                  type="button"
                  className="ast-toolbar-button"
                  title="Collapse every expandable AST node."
                  onClick={() => runAstCommand("collapse")}
                >
                  Collapse all
                </button>
                <button
                  type="button"
                  className="ast-toolbar-button"
                  title="Expand every AST node."
                  onClick={() => runAstCommand("expand")}
                >
                  Expand all
                </button>
              </div>
              ) : null}
            </div>
          </div>
          {props.parseLoading ? (
            <p className="result-placeholder">Parsing SQL and building the AST...</p>
          ) : selectedAst ? (
            <AstNodeTree node={selectedAst} command={astCommand} />
          ) : (
            <p className="result-placeholder">Parse a query to inspect the SQM tree.</p>
          )}
        </section>

      <section
        className="result-panel result-panel-scroll"
        role="tabpanel"
        aria-label="DSL"
        hidden={props.activeResultTab !== "dsl"}
      >
            <div className="result-panel-header">
              <h3>DSL</h3>
            </div>
            {props.parseLoading ? (
                <p className="result-placeholder">Parsing SQL and building the DSL...</p>
            ) : selectedDsl ? (
                <CodeBlock code={selectedDsl} language="java" />
            ) : (
                <p className="result-placeholder">Parse a query to generate the SQM DSL.</p>
            )}
          </section>

      <section
        className="result-panel result-panel-scroll"
        role="tabpanel"
        aria-label="JSON"
        hidden={props.activeResultTab !== "json"}
      >
          <div className="result-panel-header">
            <h3>JSON</h3>
            <div className="result-panel-actions">
              {hasParseViewChoices ? (
                <select
                  className="statement-view-select"
                  value={parseView}
                  onChange={(event) => setParseView(event.target.value === "sequence" ? "sequence" : Number(event.target.value))}
                  aria-label="JSON statement view"
                >
                  <option value="sequence">All statements</option>
                  {statementViews.map((statement) => (
                    <option key={statement.index} value={statement.index}>
                      Statement {statement.index}: {statement.label}
                    </option>
                  ))}
                </select>
              ) : null}
              {selectedJson ? (
              <div className="ast-toolbar">
                <button
                  type="button"
                  className="ast-toolbar-button"
                  title="Copy the current JSON view."
                  onClick={() => copyText(selectedJson, "json")}
                >
                  {copiedTarget === "json" ? "Copied JSON" : "Copy JSON"}
                </button>
                <button
                  type="button"
                  className="ast-toolbar-button"
                  title="Collapse every expandable JSON node."
                  onClick={() => runJsonCommand("collapse")}
                >
                  Collapse all
                </button>
                <button
                  type="button"
                  className="ast-toolbar-button"
                  title="Expand every JSON node."
                  onClick={() => runJsonCommand("expand")}
                >
                  Expand all
                </button>
              </div>
              ) : null}
            </div>
          </div>
          {selectedJson ? (
            <JsonTreeViewer json={selectedJson} command={jsonCommand} />
          ) : (
            <p className="result-placeholder">Parse a query to inspect the SQM JSON.</p>
          )}
        </section>

      <section
        className="result-panel"
        role="tabpanel"
        aria-label="Rendered SQL"
        hidden={props.activeResultTab !== "renderedSql"}
      >
          <div className="result-panel-header">
            <h3>Rendered SQL</h3>
            {(props.renderResponse?.renderedSql || props.transpileResponse?.renderedSql) && (
              <span className="result-meta">
                Dialect: {formatDialectLabel(props.renderedSqlDialect)}
                {props.renderedSqlTimestamp ? ` · ${formatTimestamp(props.renderedSqlTimestamp)}` : ""}
              </span>
            )}
          </div>
          {props.renderLoading ? (
            <p className="result-placeholder">Rendering SQL for the selected target dialect...</p>
          ) : props.transpileLoading ? (
            <p className="result-placeholder">Transpiling SQL for the selected target dialect...</p>
          ) : props.renderResponse?.renderedSql ? (
            <div className="render-output-stack">
              <CodeBlock code={props.renderResponse.renderedSql} language="sql" />
              {renderParams.length > 0 ? (
                <div className="render-params">
                  <h4>Parameters</h4>
                  <CodeBlock code={JSON.stringify(renderParams, null, 2)} language="json" />
                </div>
              ) : null}
            </div>
          ) : props.transpileResponse?.renderedSql ? (
            <div className="render-output-stack">
              <CodeBlock code={props.transpileResponse.renderedSql} language="sql" />
              {transpileParams.length > 0 ? (
                <div className="render-params">
                  <h4>Parameters</h4>
                  <CodeBlock code={JSON.stringify(transpileParams, null, 2)} language="json" />
                </div>
              ) : null}
            </div>
          ) : (
            <p className="result-placeholder">Rendered or transpiled SQL will appear here after an output request.</p>
          )}
        </section>

      <section
        className="result-panel"
        role="tabpanel"
        aria-label="Diagnostics"
        hidden={props.activeResultTab !== "diagnostics"}
      >
          <h3>Diagnostics</h3>
          {props.renderError ? (
            <p className="result-error">{props.renderError}</p>
          ) : props.transpileError ? (
            <p className="result-error">{props.transpileError}</p>
          ) : props.transpileLoading ? (
            <p className="result-placeholder">Transpiling SQL for the selected source and target dialects...</p>
          ) : props.transpileResponse?.diagnostics.length ? (
            renderDiagnosticsList(props.transpileResponse.diagnostics, props.onDiagnosticSelect)
          ) : props.validateError ? (
            <p className="result-error">{props.validateError}</p>
          ) : props.validateLoading ? (
            <p className="result-placeholder">Validating SQL for the selected source dialect...</p>
          ) : props.validateResponse?.diagnostics.length ? (
            renderDiagnosticsList(props.validateResponse.diagnostics, props.onDiagnosticSelect)
          ) : props.renderResponse?.diagnostics.length ? (
            renderDiagnosticsList(props.renderResponse.diagnostics, props.onDiagnosticSelect)
          ) : props.parseError ? (
            <p className="result-error">{props.parseError}</p>
          ) : props.parseResponse?.diagnostics.length ? (
            renderDiagnosticsList(props.parseResponse.diagnostics, props.onDiagnosticSelect)
          ) : props.validateResponse ? (
            <p className="result-placeholder">
              {props.validateResponse.valid
                ? "Validation succeeded with no diagnostics."
                : "Validation completed with no structured diagnostics."}
            </p>
          ) : props.transpileResponse ? (
            <p className="result-placeholder">
              {props.transpileResponse.outcome === "approximate"
                ? "Transpilation completed approximately with no diagnostics."
                : props.transpileResponse.outcome === "exact"
                  ? "Transpilation completed exactly with no diagnostics."
                  : "Transpilation did not produce diagnostics."}
            </p>
          ) : props.renderResponse ? (
            <p className="result-placeholder">No diagnostics were returned for the last render.</p>
          ) : props.parseResponse ? (
            <p className="result-placeholder">No diagnostics were returned for the last parse.</p>
          ) : (
            <p className="result-placeholder">Warnings and errors will appear here.</p>
          )}
        </section>

      <section
        className="result-panel"
        role="tabpanel"
        aria-label="About Result"
        hidden={props.activeResultTab !== "about"}
      >
          <h3>About Result</h3>
          {props.parseResponse || props.renderResponse || props.transpileResponse || props.validateResponse ? (
            <dl className="about-list">
              <div className="about-row">
                <dt>Request ID</dt>
                <dd>{props.parseResponse?.requestId ?? "n/a"}</dd>
              </div>
              <div className="about-row">
                <dt>Duration</dt>
                <dd>{props.parseResponse ? `${props.parseResponse.durationMs} ms` : "n/a"}</dd>
              </div>
              <div className="about-row">
                <dt>Statement kind</dt>
                <dd>{props.parseResponse?.statementKind ?? "n/a"}</dd>
              </div>
              <div className="about-row">
                <dt>Statement count</dt>
                <dd>{props.parseResponse ? statementCount(props.parseResponse) : "n/a"}</dd>
              </div>
              <div className="about-row">
                <dt>Root node type</dt>
                <dd>{props.parseResponse?.summary?.rootNodeType ?? "n/a"}</dd>
              </div>
              <div className="about-row">
                <dt>Last render request</dt>
                <dd>{props.renderResponse?.requestId ?? "n/a"}</dd>
              </div>
              <div className="about-row">
                <dt>Last render duration</dt>
                <dd>{props.renderResponse ? `${props.renderResponse.durationMs} ms` : "n/a"}</dd>
              </div>
              <div className="about-row">
                <dt>Last transpile request</dt>
                <dd>{props.transpileResponse?.requestId ?? "n/a"}</dd>
              </div>
              <div className="about-row">
                <dt>Last transpile duration</dt>
                <dd>{props.transpileResponse ? `${props.transpileResponse.durationMs} ms` : "n/a"}</dd>
              </div>
              <div className="about-row">
                <dt>Transpile outcome</dt>
                <dd>{props.transpileResponse?.outcome ?? "n/a"}</dd>
              </div>
              <div className="about-row">
                <dt>Last validate request</dt>
                <dd>{props.validateResponse?.requestId ?? "n/a"}</dd>
              </div>
              <div className="about-row">
                <dt>Last validate duration</dt>
                <dd>{props.validateResponse ? `${props.validateResponse.durationMs} ms` : "n/a"}</dd>
              </div>
              <div className="about-row">
                <dt>Validation result</dt>
                <dd>{props.validateResponse ? (props.validateResponse.valid ? "valid" : "invalid") : "n/a"}</dd>
              </div>
            </dl>
          ) : (
            <p className="result-placeholder">Operation summary details will appear here.</p>
          )}
        </section>
    </article>
  );
}

interface DerivedStatementView {
  index: number;
  label: string;
  sqmDsl: string | null;
  sqmJson: string | null;
  ast: NonNullable<ParseResponseDto["ast"]> | null;
}

function deriveStatementViews(response: ParseResponseDto): DerivedStatementView[] {
  const astStatements = response.ast?.children.find((child) => child.slot === "statements")?.nodes ?? [];
  const dslStatements = extractStatementDsl(response.sqmDsl);
  const jsonStatements = extractStatementJson(response.sqmJson);
  const count = Math.max(astStatements.length, dslStatements.length, jsonStatements.length);

  return Array.from({ length: count }, (_, offset) => {
    const ast = astStatements[offset] ?? null;
    return {
      index: offset + 1,
      label: ast?.nodeType ?? "statement",
      sqmDsl: dslStatements[offset] ?? null,
      sqmJson: jsonStatements[offset] ?? null,
      ast
    };
  });
}

function statementCount(response: ParseResponseDto) {
  if (!response.multiStatement) {
    return response.success ? 1 : 0;
  }
  return deriveStatementViews(response).length;
}

function extractStatementJson(sqmJson: string | null): string[] {
  if (!sqmJson) {
    return [];
  }

  try {
    const parsed = JSON.parse(sqmJson) as { statements?: unknown };
    if (!Array.isArray(parsed.statements)) {
      return [];
    }
    return parsed.statements.map((statement) => JSON.stringify(statement, null, 2));
  } catch {
    return [];
  }
}

function extractStatementDsl(sqmDsl: string | null): string[] {
  if (!sqmDsl) {
    return [];
  }

  const markers = Array.from(sqmDsl.matchAll(/^\/\/ Statement \d+\s*\r?\n/gm));
  if (markers.length === 0) {
    return [];
  }

  return markers.map((marker, index) => {
    const start = (marker.index ?? 0) + marker[0].length;
    const end = markers[index + 1]?.index ?? sqmDsl.length;
    return sqmDsl.slice(start, end).trim();
  });
}

function formatAstTree(node: NonNullable<ParseResponseDto["ast"]>) {
  return formatAstNode(node, 0).join("\n");
}

function formatAstNode(node: NonNullable<ParseResponseDto["ast"]>, depth: number): string[] {
  const indent = "  ".repeat(depth);
  const lines = [`${indent}${node.label}`];

  for (const detail of node.details) {
    lines.push(`${indent}  ${detail.name}: ${detail.value}`);
  }

  for (const child of node.children) {
    const slotLabel = `${child.slot}${child.multiple ? "[]" : ""}`;
    lines.push(`${indent}  ${slotLabel}`);

    if (child.nodes.length === 0) {
      lines.push(`${indent}    <empty>`);
      continue;
    }

    for (const childNode of child.nodes) {
      lines.push(...formatAstNode(childNode, depth + 2));
    }
  }

  return lines;
}

function formatDialectLabel(dialect: SqlDialect | null) {
  if (!dialect) {
    return "n/a";
  }

  switch (dialect) {
    case "ansi":
      return "ANSI";
    case "postgresql":
      return "PostgreSQL";
    case "mysql":
      return "MySQL";
    case "sqlserver":
      return "SQL Server";
  }
}

function formatTimestamp(timestamp: number) {
  return new Intl.DateTimeFormat(undefined, {
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit"
  }).format(timestamp);
}

function renderDiagnosticsList(
  diagnostics: PlaygroundDiagnosticDto[],
  onDiagnosticSelect: (diagnostic: PlaygroundDiagnosticDto) => void
) {
  return (
    <ul className="diagnostic-list">
      {diagnostics.map((diagnostic) => (
        <li key={`${diagnostic.code}-${diagnostic.message}-${diagnostic.line ?? "na"}-${diagnostic.column ?? "na"}`}>
          <button
            type="button"
            className="diagnostic-button"
            title="Focus the SQL editor at this diagnostic location."
            onClick={() => onDiagnosticSelect(diagnostic)}
          >
            <strong>{diagnostic.severity}</strong> {diagnostic.code}: {diagnostic.message}
            {diagnostic.statementIndex != null ? ` (statement ${diagnostic.statementIndex})` : ""}
            {diagnostic.line !== null && diagnostic.column !== null
              ? ` (${diagnostic.line}:${diagnostic.column})`
              : ""}
          </button>
        </li>
      ))}
    </ul>
  );
}
