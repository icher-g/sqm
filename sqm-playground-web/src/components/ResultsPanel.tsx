import { useState } from "react";

type ResultTab = "ast" | "json" | "renderedSql" | "diagnostics" | "about";

/**
 * Renders the tabbed results shell.
 */
export function ResultsPanel() {
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
        <section className="result-panel" role="tabpanel" aria-label="AST">
          <h3>AST</h3>
          <p className="result-placeholder">Parse a query to inspect the SQM tree.</p>
        </section>
      ) : null}

      {activeResultTab === "json" ? (
        <section className="result-panel" role="tabpanel" aria-label="JSON">
          <h3>JSON</h3>
          <p className="result-placeholder">Serialized SQM JSON will appear here.</p>
        </section>
      ) : null}

      {activeResultTab === "renderedSql" ? (
        <section className="result-panel" role="tabpanel" aria-label="Rendered SQL">
          <h3>Rendered SQL</h3>
          <p className="result-placeholder">Rendered or transpiled SQL will appear here.</p>
        </section>
      ) : null}

      {activeResultTab === "diagnostics" ? (
        <section className="result-panel" role="tabpanel" aria-label="Diagnostics">
          <h3>Diagnostics</h3>
          <p className="result-placeholder">Warnings and errors will appear here.</p>
        </section>
      ) : null}

      {activeResultTab === "about" ? (
        <section className="result-panel" role="tabpanel" aria-label="About Result">
          <h3>About Result</h3>
          <p className="result-placeholder">Operation summary details will appear here.</p>
        </section>
      ) : null}
    </article>
  );
}
