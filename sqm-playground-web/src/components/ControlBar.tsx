import type { ExampleDto } from "../types/api";

interface ControlBarProps {
  examples: ExampleDto[];
  selectedExampleId: string;
  examplesLoading: boolean;
  examplesError: string | null;
  onExampleChange: (nextExampleId: string) => void;
}

/**
 * Renders the top control area for examples, dialects, and action buttons.
 */
export function ControlBar(props: ControlBarProps) {
  return (
    <article className="card">
      <h2>Controls</h2>
      <p>Choose the source and target dialects, then run one of the playground actions.</p>

      <div className="control-stack">
        <div className="control-field">
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
  );
}
