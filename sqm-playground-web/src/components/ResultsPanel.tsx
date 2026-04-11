import type { ParseResponseDto, RenderResponseDto, TranspileResponseDto, ValidateResponseDto } from "../types/api";
import { AstNodeTree } from "./AstNodeTree";

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
  return (
    <article className="card">
      <h2>Results</h2>
      <p>The results area now uses tabs so each future response type has room to breathe.</p>

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

      {props.activeResultTab === "ast" ? (
        <section className="result-panel result-panel-scroll" role="tabpanel" aria-label="AST">
          <h3>AST</h3>
          {props.parseLoading ? (
            <p className="result-placeholder">Parsing SQL and building the AST...</p>
          ) : props.parseResponse?.ast ? (
            <AstNodeTree node={props.parseResponse.ast} />
          ) : (
            <p className="result-placeholder">Parse a query to inspect the SQM tree.</p>
          )}
        </section>
      ) : null}

      {props.activeResultTab === "dsl" ? (
          <section className="result-panel result-panel-scroll" role="tabpanel" aria-label="DSL">
            <h3>DSL</h3>
            {props.parseLoading ? (
                <p className="result-placeholder">Parsing SQL and building the DSL...</p>
            ) : props.parseResponse?.sqmDsl ? (
                <pre className="result-code-block">{props.parseResponse.sqmDsl}</pre>
            ) : (
                <p className="result-placeholder">Parse a query to generate the SQM DSL.</p>
            )}
          </section>
      ) : null}

      {props.activeResultTab === "json" ? (
        <section className="result-panel" role="tabpanel" aria-label="JSON">
          <h3>JSON</h3>
          {props.parseResponse?.sqmJson ? (
            <pre className="result-code-block">{props.parseResponse.sqmJson}</pre>
          ) : (
            <p className="result-placeholder">Parse a query to inspect the SQM JSON.</p>
          )}
        </section>
      ) : null}

      {props.activeResultTab === "renderedSql" ? (
        <section className="result-panel" role="tabpanel" aria-label="Rendered SQL">
          <h3>Rendered SQL</h3>
          {props.renderLoading ? (
            <p className="result-placeholder">Rendering SQL for the selected target dialect...</p>
          ) : props.transpileLoading ? (
            <p className="result-placeholder">Transpiling SQL for the selected target dialect...</p>
          ) : props.renderResponse?.renderedSql ? (
            <pre className="result-code-block">{props.renderResponse.renderedSql}</pre>
          ) : props.transpileResponse?.renderedSql ? (
            <pre className="result-code-block">{props.transpileResponse.renderedSql}</pre>
          ) : (
            <p className="result-placeholder">Rendered or transpiled SQL will appear here after an output request.</p>
          )}
        </section>
      ) : null}

      {props.activeResultTab === "diagnostics" ? (
        <section className="result-panel" role="tabpanel" aria-label="Diagnostics">
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
      ) : null}

      {props.activeResultTab === "about" ? (
        <section className="result-panel" role="tabpanel" aria-label="About Result">
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
      ) : null}
    </article>
  );
}
