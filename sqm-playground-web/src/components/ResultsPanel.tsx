import { useRef, useState } from "react";
import type { ParseResponseDto, RenderResponseDto, TranspileResponseDto, ValidateResponseDto } from "../types/api";
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
  renderLoading: boolean;
  renderError: string | null;
  transpileResponse: TranspileResponseDto | null;
  transpileLoading: boolean;
  transpileError: string | null;
  validateResponse: ValidateResponseDto | null;
  validateLoading: boolean;
  validateError: string | null;
}

/**
 * Renders the tabbed results shell.
 */
export function ResultsPanel(props: ResultsPanelProps) {
  const [astCommand, setAstCommand] = useState<AstTreeCommand | null>(null);
  const [jsonCommand, setJsonCommand] = useState<JsonTreeCommand | null>(null);
  const [copiedTarget, setCopiedTarget] = useState<"ast" | "json" | null>(null);
  const copyResetTimeoutRef = useRef<number | null>(null);

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
    <article className="card">
      <h2>Results</h2>
      <p>Inspect SQM as a tree, review the generated JSON and DSL, compare rendered SQL, and capture diagnostics.</p>

      <div className="tab-row" role="tablist" aria-label="Result tabs">
        <button
          type="button"
          role="tab"
          aria-selected={props.activeResultTab === "ast"}
          className={props.activeResultTab === "ast" ? "tab-button tab-button-active" : "tab-button"}
          onClick={() => props.onResultTabChange("ast")}
        >
          AST
        </button>
        <button
            type="button"
            role="tab"
            aria-selected={props.activeResultTab === "dsl"}
            className={props.activeResultTab === "dsl" ? "tab-button tab-button-active" : "tab-button"}
            onClick={() => props.onResultTabChange("dsl")}
        >
          DSL
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={props.activeResultTab === "json"}
          className={props.activeResultTab === "json" ? "tab-button tab-button-active" : "tab-button"}
          onClick={() => props.onResultTabChange("json")}
        >
          JSON
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={props.activeResultTab === "renderedSql"}
          className={props.activeResultTab === "renderedSql" ? "tab-button tab-button-active" : "tab-button"}
          onClick={() => props.onResultTabChange("renderedSql")}
        >
          Rendered SQL
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={props.activeResultTab === "diagnostics"}
          className={props.activeResultTab === "diagnostics" ? "tab-button tab-button-active" : "tab-button"}
          onClick={() => props.onResultTabChange("diagnostics")}
        >
          Diagnostics
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={props.activeResultTab === "about"}
          className={props.activeResultTab === "about" ? "tab-button tab-button-active" : "tab-button"}
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
            {props.parseResponse?.ast ? (
              <div className="ast-toolbar">
                <button
                  type="button"
                  className="ast-toolbar-button"
                  onClick={() => copyText(formatAstTree(props.parseResponse!.ast!), "ast")}
                >
                  {copiedTarget === "ast" ? "Copied AST" : "Copy AST"}
                </button>
                <button type="button" className="ast-toolbar-button" onClick={() => runAstCommand("collapse")}>
                  Collapse all
                </button>
                <button type="button" className="ast-toolbar-button" onClick={() => runAstCommand("expand")}>
                  Expand all
                </button>
              </div>
            ) : null}
          </div>
          {props.parseLoading ? (
            <p className="result-placeholder">Parsing SQL and building the AST...</p>
          ) : props.parseResponse?.ast ? (
            <AstNodeTree node={props.parseResponse.ast} command={astCommand} />
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
            <h3>DSL</h3>
            {props.parseLoading ? (
                <p className="result-placeholder">Parsing SQL and building the DSL...</p>
            ) : props.parseResponse?.sqmDsl ? (
                <CodeBlock code={props.parseResponse.sqmDsl} language="java" />
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
            {props.parseResponse?.sqmJson ? (
              <div className="ast-toolbar">
                <button
                  type="button"
                  className="ast-toolbar-button"
                  onClick={() => copyText(props.parseResponse!.sqmJson!, "json")}
                >
                  {copiedTarget === "json" ? "Copied JSON" : "Copy JSON"}
                </button>
                <button type="button" className="ast-toolbar-button" onClick={() => runJsonCommand("collapse")}>
                  Collapse all
                </button>
                <button type="button" className="ast-toolbar-button" onClick={() => runJsonCommand("expand")}>
                  Expand all
                </button>
              </div>
            ) : null}
          </div>
          {props.parseResponse?.sqmJson ? (
            <JsonTreeViewer json={props.parseResponse.sqmJson} command={jsonCommand} />
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
          <h3>Rendered SQL</h3>
          {props.renderLoading ? (
            <p className="result-placeholder">Rendering SQL for the selected target dialect...</p>
          ) : props.transpileLoading ? (
            <p className="result-placeholder">Transpiling SQL for the selected target dialect...</p>
          ) : props.renderResponse?.renderedSql ? (
            <CodeBlock code={props.renderResponse.renderedSql} language="sql" />
          ) : props.transpileResponse?.renderedSql ? (
            <CodeBlock code={props.transpileResponse.renderedSql} language="sql" />
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
            <ul className="diagnostic-list">
              {props.transpileResponse.diagnostics.map((diagnostic) => (
                <li key={`${diagnostic.code}-${diagnostic.message}`} className="diagnostic-item">
                  <strong>{diagnostic.severity}</strong> {diagnostic.code}: {diagnostic.message}
                </li>
              ))}
            </ul>
          ) : props.validateError ? (
            <p className="result-error">{props.validateError}</p>
          ) : props.validateLoading ? (
            <p className="result-placeholder">Validating SQL for the selected source dialect...</p>
          ) : props.validateResponse?.diagnostics.length ? (
            <ul className="diagnostic-list">
              {props.validateResponse.diagnostics.map((diagnostic) => (
                <li key={`${diagnostic.code}-${diagnostic.message}`} className="diagnostic-item">
                  <strong>{diagnostic.severity}</strong> {diagnostic.code}: {diagnostic.message}
                </li>
              ))}
            </ul>
          ) : props.renderResponse?.diagnostics.length ? (
            <ul className="diagnostic-list">
              {props.renderResponse.diagnostics.map((diagnostic) => (
                <li key={`${diagnostic.code}-${diagnostic.message}`} className="diagnostic-item">
                  <strong>{diagnostic.severity}</strong> {diagnostic.code}: {diagnostic.message}
                </li>
              ))}
            </ul>
          ) : props.parseError ? (
            <p className="result-error">{props.parseError}</p>
          ) : props.parseResponse?.diagnostics.length ? (
            <ul className="diagnostic-list">
              {props.parseResponse.diagnostics.map((diagnostic) => (
                <li key={`${diagnostic.code}-${diagnostic.message}`} className="diagnostic-item">
                  <strong>{diagnostic.severity}</strong> {diagnostic.code}: {diagnostic.message}
                </li>
              ))}
            </ul>
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
