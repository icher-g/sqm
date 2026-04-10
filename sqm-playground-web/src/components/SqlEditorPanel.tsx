interface SqlEditorPanelProps {
  sqlText: string;
  onSqlTextChange: (nextSqlText: string) => void;
}

/**
 * Renders the editable SQL input area.
 */
export function SqlEditorPanel(props: SqlEditorPanelProps) {
  return (
    <article className="card">
      <h2>Editor</h2>
      <p>Edit the SQL text directly. Later stories will connect this input to backend operations.</p>

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
