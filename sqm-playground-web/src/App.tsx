import { useState } from "react";

/**
 * Root application component for the frontend shell.
 */
export default function App() {
  const [sqlText, setSqlText] = useState(
    "select id, name\nfrom customer\nwhere id = 1\norder by name"
  );

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
          <p>AST, JSON, SQL output, and diagnostics will go here.</p>
        </article>
      </section>
    </main>
  );
}
