import { useState } from "react";

type ResultTab = "ast" | "json" | "renderedSql" | "diagnostics" | "about";

/**
 * Root application component for the frontend shell.
 */
export default function App() {
  const [sqlText, setSqlText] = useState(
    "select id, name\nfrom customer\nwhere id = 1\norder by name"
  );
  const [activeResultTab, setActiveResultTab] = useState<ResultTab>("ast");

  return (
    <main className="app-shell">
      <header className="hero">
        <p className="eyebrow">SQM Playground</p>
        <h1>Frontend shell</h1>
        <p className="hero-copy">
          This is the first learning slice. It only proves that the React app starts and renders a placeholder
          workstation.
        </p>
      </header>

      <section className="shell-grid">
        <article className="card">
          <h2>Controls</h2>
          <p>Choose the source and target dialects, then run one of the playground actions.</p>

          <div className="control-stack">
            <div className="control-field">
              <label htmlFor="source-dialect">Source dialect</label>
              <select id="source-dialect" defaultValue="ansi">
                <option value="ansi">ansi</option>
                <option value="postgresql">postgresql</option>
                <option value="mysql">mysql</option>
                <option value="sqlserver">sqlserver</option>
              </select>
            </div>

            <div className="control-field">
              <label htmlFor="target-dialect">Target dialect</label>
              <select id="target-dialect" defaultValue="postgresql">
                <option value="ansi">ansi</option>
                <option value="postgresql">postgresql</option>
                <option value="mysql">mysql</option>
                <option value="sqlserver">sqlserver</option>
              </select>
            </div>

            <div className="button-row">
              <button type="button" disabled>
                Parse
              </button>
              <button type="button" disabled>
                Render
              </button>
              <button type="button" disabled>
                Validate
              </button>
              <button type="button" disabled>
                Transpile
              </button>
            </div>
          </div>
        </article>

        <article className="card">
          <h2>Editor</h2>
          <p>Edit the SQL text directly. Later stories will connect this input to backend operations.</p>

          <label className="editor-label" htmlFor="sql-editor">
            SQL text
          </label>
          <textarea
            id="sql-editor"
            className="sql-editor"
            value={sqlText}
            onChange={(event) => setSqlText(event.target.value)}
            spellCheck={false}
          />
        </article>

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
      </section>
    </main>
  );
}
