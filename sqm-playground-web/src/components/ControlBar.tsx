import type { ExampleDto, SqlDialect } from "../types/api";

interface ControlBarProps {
  examples: ExampleDto[];
  selectedExampleId: string;
  examplesLoading: boolean;
  examplesError: string | null;
  sourceDialect: SqlDialect;
  targetDialect: SqlDialect;
  parseLoading: boolean;
  canParse: boolean;
  onExampleChange: (nextExampleId: string) => void;
  onSourceDialectChange: (nextDialect: SqlDialect) => void;
  onTargetDialectChange: (nextDialect: SqlDialect) => void;
  onParse: () => void;
}

/**
 * Renders the top control area for examples, dialects, and action buttons.
 */
export function ControlBar(props: ControlBarProps) {
  return (
    <article className="card control-card">
      <h2>Controls</h2>
      <p>Choose the source and target dialects, then run one of the playground actions.</p>

      <div className="control-stack">
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

        <div className="action-group">
          <p className="action-group-title">Actions</p>
          <div className="button-row">
            <button
              type="button"
              className="button-primary"
              onClick={props.onParse}
              disabled={!props.canParse || props.parseLoading}
            >
            {props.parseLoading ? "Parsing..." : "Parse"}
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
      </div>
    </article>
  );
}
