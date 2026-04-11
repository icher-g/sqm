import { useState } from "react";
import type { ParseResponseDto, RenderResponseDto } from "../types/api";
import { AstNodeTree } from "./AstNodeTree";

type ResultTab = "ast" | "dsl" | "json" | "renderedSql" | "diagnostics" | "about";

interface ResultsPanelProps {
  parseResponse: ParseResponseDto | null;
  parseLoading: boolean;
  parseError: string | null;
  renderResponse: RenderResponseDto | null;
  renderLoading: boolean;
  renderError: string | null;
}

/**
 * Renders the tabbed results shell.
 */
export function ResultsPanel(props: ResultsPanelProps) {
  const [activeResultTab, setActiveResultTab] = useState<ResultTab>("ast");

  return (
    <article className="card">
      <h2>Results</h2>
      <p>The results area now uses tabs so each future response type has room to breathe.</p>

      <div className="tab-row" role="tablist" aria-label="Result tabs">
        <button
          type="button"
          role="tab"
          aria-selected={activeResultTab === "ast"}
          className={activeResultTab === "ast" ? "tab-button tab-button-active" : "tab-button"}
          onClick={() => setActiveResultTab("ast")}
        >
          AST
        </button>
        <button
            type="button"
            role="tab"
            aria-selected={activeResultTab === "dsl"}
            className={activeResultTab === "dsl" ? "tab-button tab-button-active" : "tab-button"}
            onClick={() => setActiveResultTab("dsl")}
        >
          DSL
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={activeResultTab === "json"}
          className={activeResultTab === "json" ? "tab-button tab-button-active" : "tab-button"}
          onClick={() => setActiveResultTab("json")}
        >
          JSON
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={activeResultTab === "renderedSql"}
          className={activeResultTab === "renderedSql" ? "tab-button tab-button-active" : "tab-button"}
          onClick={() => setActiveResultTab("renderedSql")}
        >
          Rendered SQL
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={activeResultTab === "diagnostics"}
          className={activeResultTab === "diagnostics" ? "tab-button tab-button-active" : "tab-button"}
          onClick={() => setActiveResultTab("diagnostics")}
        >
          Diagnostics
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={activeResultTab === "about"}
          className={activeResultTab === "about" ? "tab-button tab-button-active" : "tab-button"}
          onClick={() => setActiveResultTab("about")}
        >
          About Result
        </button>
      </div>

      {activeResultTab === "ast" ? (
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

      {activeResultTab === "dsl" ? (
          <section className="result-panel result-panel-scroll" role="tabpanel" aria-label="DSL">
            <h3>DSL</h3>
            {props.parseLoading ? (
                <p className="result-placeholder">Parsing SQL and building the DSL...</p>
            ) : props.parseResponse?.sqmDsl ? (
                <pre className="result-code-block">{props.parseResponse.sqmDsl}</pre>
            ) : (
                <p className="result-placeholder">Parse a query to inspect the SQM tree.</p>
            )}
          </section>
      ) : null}

      {activeResultTab === "json" ? (
        <section className="result-panel" role="tabpanel" aria-label="JSON">
          <h3>JSON</h3>
          {props.parseResponse?.sqmJson ? (
            <pre className="result-code-block">{props.parseResponse.sqmJson}</pre>
          ) : (
            <p className="result-placeholder">Serialized SQM JSON will appear here.</p>
          )}
        </section>
      ) : null}

      {activeResultTab === "renderedSql" ? (
        <section className="result-panel" role="tabpanel" aria-label="Rendered SQL">
          <h3>Rendered SQL</h3>
          {props.renderLoading ? (
            <p className="result-placeholder">Rendering SQL for the selected target dialect...</p>
          ) : props.renderResponse?.renderedSql ? (
            <pre className="result-code-block">{props.renderResponse.renderedSql}</pre>
          ) : (
            <p className="result-placeholder">Rendered SQL will appear here after a render request.</p>
          )}
        </section>
      ) : null}

      {activeResultTab === "diagnostics" ? (
        <section className="result-panel" role="tabpanel" aria-label="Diagnostics">
          <h3>Diagnostics</h3>
          {props.renderError ? (
            <p className="result-error">{props.renderError}</p>
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
          ) : props.renderResponse ? (
            <p className="result-placeholder">No diagnostics were returned for the last render.</p>
          ) : props.parseResponse ? (
            <p className="result-placeholder">No diagnostics were returned for the last parse.</p>
          ) : (
            <p className="result-placeholder">Warnings and errors will appear here.</p>
          )}
        </section>
      ) : null}

      {activeResultTab === "about" ? (
        <section className="result-panel" role="tabpanel" aria-label="About Result">
          <h3>About Result</h3>
          {props.parseResponse ? (
            <dl className="about-list">
              <div className="about-row">
                <dt>Request ID</dt>
                <dd>{props.parseResponse.requestId}</dd>
              </div>
              <div className="about-row">
                <dt>Duration</dt>
                <dd>{props.parseResponse.durationMs} ms</dd>
              </div>
              <div className="about-row">
                <dt>Statement kind</dt>
                <dd>{props.parseResponse.statementKind ?? "n/a"}</dd>
              </div>
              <div className="about-row">
                <dt>Root node type</dt>
                <dd>{props.parseResponse.summary?.rootNodeType ?? "n/a"}</dd>
              </div>
              <div className="about-row">
                <dt>Last render request</dt>
                <dd>{props.renderResponse?.requestId ?? "n/a"}</dd>
              </div>
              <div className="about-row">
                <dt>Last render duration</dt>
                <dd>{props.renderResponse ? `${props.renderResponse.durationMs} ms` : "n/a"}</dd>
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
