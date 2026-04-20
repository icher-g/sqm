import type { ExampleDto, PlaygroundDiagnosticDto, RenderParameterizationMode, SqlDialect } from "../types/api";
import { SqlCodeEditor } from "./SqlCodeEditor";

interface SqlEditorPanelProps {
  sqlText: string;
  examples: ExampleDto[];
  selectedExampleId: string;
  examplesLoading: boolean;
  examplesError: string | null;
  sourceDialect: SqlDialect;
  targetDialect: SqlDialect;
  renderParameterizationMode: RenderParameterizationMode;
  editorDiagnostics: PlaygroundDiagnosticDto[];
  focusedDiagnostic: { diagnostic: PlaygroundDiagnosticDto; version: number } | null;
  activeAction: "parse" | "format" | "render" | "validate" | "transpile" | null;
  parseLoading: boolean;
  formatLoading: boolean;
  renderLoading: boolean;
  transpileLoading: boolean;
  validateLoading: boolean;
  canParse: boolean;
  canFormat: boolean;
  canRender: boolean;
  canTranspile: boolean;
  canValidate: boolean;
  onSqlTextChange: (nextSqlText: string) => void;
  onExampleChange: (nextExampleId: string) => void;
  onSourceDialectChange: (nextDialect: SqlDialect) => void;
  onTargetDialectChange: (nextDialect: SqlDialect) => void;
  onRenderParameterizationModeChange: (nextMode: RenderParameterizationMode) => void;
  onParse: () => void;
  onFormat: () => void;
  onRender: () => void;
  onTranspile: () => void;
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
                title="Parse SQL using the selected source dialect and show its SQM AST, JSON, and DSL."
                onClick={props.onParse}
                disabled={
                  !props.canParse
                  || props.parseLoading
                  || props.formatLoading
                  || props.renderLoading
                  || props.transpileLoading
                  || props.validateLoading
                }
              >
                {props.parseLoading ? "Parsing..." : "Parse"}
              </button>
              <button
                type="button"
                className={props.activeAction === "validate" ? "button-primary" : undefined}
                title="Validate SQL using the selected source dialect and show diagnostics."
                onClick={props.onValidate}
                disabled={
                  !props.canValidate
                  || props.validateLoading
                  || props.parseLoading
                  || props.formatLoading
                  || props.renderLoading
                  || props.transpileLoading
                }
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
            <div className="control-field parameterization-field">
              <span className="control-label" id="render-parameterization-label">Render parameters</span>
              <div className="segmented-control" role="group" aria-labelledby="render-parameterization-label">
                <button
                  type="button"
                  className={props.renderParameterizationMode === "inline" ? "segmented-button segmented-button-active" : "segmented-button"}
                  aria-pressed={props.renderParameterizationMode === "inline"}
                  title="Render literal values directly in the SQL output."
                  onClick={() => props.onRenderParameterizationModeChange("inline")}
                >
                  Inline
                </button>
                <button
                  type="button"
                  className={props.renderParameterizationMode === "bind" ? "segmented-button segmented-button-active" : "segmented-button"}
                  aria-pressed={props.renderParameterizationMode === "bind"}
                  title="Render literals as bind placeholders and return parameter values separately."
                  onClick={() => props.onRenderParameterizationModeChange("bind")}
                >
                  Bind
                </button>
              </div>
            </div>
            <div className="button-column">
              <button
                type="button"
                className={props.activeAction === "render" ? "button-primary" : undefined}
                title="Render SQL using the selected target dialect."
                onClick={props.onRender}
                disabled={
                  !props.canRender
                  || props.renderLoading
                  || props.parseLoading
                  || props.formatLoading
                  || props.transpileLoading
                  || props.validateLoading
                }
              >
                {props.renderLoading ? "Rendering..." : "Render"}
              </button>
              <button
                type="button"
                className={props.activeAction === "transpile" ? "button-primary" : undefined}
                title="Transpile SQL from the source dialect to the target dialect."
                onClick={props.onTranspile}
                disabled={
                  !props.canTranspile
                  || props.transpileLoading
                  || props.parseLoading
                  || props.formatLoading
                  || props.renderLoading
                  || props.validateLoading
                }
              >
                {props.transpileLoading ? "Transpiling..." : "Transpile"}
              </button>
            </div>
          </div>
        </div>
      </div>

      <div className="editor-field">
        <div className="editor-field-header">
          <span className="editor-label">SQL text</span>
          <button
            type="button"
            className={props.activeAction === "format" ? "editor-secondary-button button-primary" : "editor-secondary-button"}
            title="Format the SQL text using the selected source dialect."
            onClick={props.onFormat}
            disabled={
              !props.canFormat
              || props.formatLoading
              || props.parseLoading
              || props.renderLoading
              || props.transpileLoading
              || props.validateLoading
            }
          >
            {props.formatLoading ? "Formatting..." : "Format SQL"}
          </button>
        </div>
        <SqlCodeEditor
          ariaLabel="SQL text"
          dialect={props.sourceDialect}
          diagnostics={props.editorDiagnostics}
          focusedDiagnostic={props.focusedDiagnostic}
          value={props.sqlText}
          onChange={props.onSqlTextChange}
        />
      </div>
    </article>
  );
}
