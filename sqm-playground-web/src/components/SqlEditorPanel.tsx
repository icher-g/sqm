import type { ExampleDto, SqlDialect } from "../types/api";

interface SqlEditorPanelProps {
  sqlText: string;
  examples: ExampleDto[];
  selectedExampleId: string;
  examplesLoading: boolean;
  examplesError: string | null;
  sourceDialect: SqlDialect;
  targetDialect: SqlDialect;
  activeAction: "parse" | "render" | "validate" | null;
  parseLoading: boolean;
  renderLoading: boolean;
  validateLoading: boolean;
  canParse: boolean;
  canRender: boolean;
  canValidate: boolean;
  onSqlTextChange: (nextSqlText: string) => void;
  onExampleChange: (nextExampleId: string) => void;
  onSourceDialectChange: (nextDialect: SqlDialect) => void;
  onTargetDialectChange: (nextDialect: SqlDialect) => void;
  onParse: () => void;
  onRender: () => void;
  onValidate: () => void;
}

/**
 * Renders the editable SQL input area.
 */
export function SqlEditorPanel(props: SqlEditorPanelProps) {
  return (
    <article className="card editor-card">
      <h2>Editor</h2>
      <p>Choose an example, set the relevant dialects, and run actions directly above the SQL text.</p>

      <div className="editor-controls">
        <div className="control-field control-field-example">
          <label htmlFor="example-picker">Example</label>
          <select
            id="example-picker"
            value={props.selectedExampleId}
            onChange={(event) => props.onExampleChange(event.target.value)}
            disabled={props.examplesLoading || props.examples.length === 0}
          >
            {props.examples.length === 0 ? (
              <option value="">{props.examplesLoading ? "Loading..." : "No examples"}</option>
            ) : (
              props.examples.map((example) => (
                <option key={example.id} value={example.id}>
                  {example.title} ({example.dialect})
                </option>
              ))
            )}
          </select>
          <p className="control-help">
            {props.examplesLoading ? "Loading examples from the backend..." : props.examplesError ?? "Choose a starter query."}
          </p>
        </div>

        <div className="editor-action-grid">
          <div className="editor-action-card">
            <div className="control-field">
              <label htmlFor="source-dialect">Source dialect</label>
              <select
                id="source-dialect"
                value={props.sourceDialect}
                onChange={(event) => props.onSourceDialectChange(event.target.value as SqlDialect)}
              >
                <option value="ansi">ansi</option>
                <option value="postgresql">postgresql</option>
                <option value="mysql">mysql</option>
                <option value="sqlserver">sqlserver</option>
              </select>
            </div>
            <div className="button-column">
              <button
                type="button"
                className={props.activeAction === "parse" ? "button-primary" : undefined}
                onClick={props.onParse}
                disabled={!props.canParse || props.parseLoading || props.renderLoading || props.validateLoading}
              >
                {props.parseLoading ? "Parsing..." : "Parse"}
              </button>
              <button
                type="button"
                className={props.activeAction === "validate" ? "button-primary" : undefined}
                onClick={props.onValidate}
                disabled={!props.canValidate || props.validateLoading || props.parseLoading || props.renderLoading}
              >
                {props.validateLoading ? "Validating..." : "Validate"}
              </button>
            </div>
          </div>

          <div className="editor-action-card">
            <div className="control-field">
              <label htmlFor="target-dialect">Target dialect</label>
              <select
                id="target-dialect"
                value={props.targetDialect}
                onChange={(event) => props.onTargetDialectChange(event.target.value as SqlDialect)}
              >
                <option value="ansi">ansi</option>
                <option value="postgresql">postgresql</option>
                <option value="mysql">mysql</option>
                <option value="sqlserver">sqlserver</option>
              </select>
            </div>
            <div className="button-column">
              <button
                type="button"
                className={props.activeAction === "render" ? "button-primary" : undefined}
                onClick={props.onRender}
                disabled={!props.canRender || props.renderLoading || props.parseLoading || props.validateLoading}
              >
                {props.renderLoading ? "Rendering..." : "Render"}
              </button>
              <button type="button" disabled>
                Transpile
              </button>
            </div>
          </div>
        </div>
      </div>

      <label className="editor-label" htmlFor="sql-editor">
        SQL text
      </label>
      <textarea
        id="sql-editor"
        className="sql-editor"
        value={props.sqlText}
        onChange={(event) => props.onSqlTextChange(event.target.value)}
        spellCheck={false}
      />
    </article>
  );
}
