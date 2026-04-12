import MonacoEditor, { type OnMount } from "@monaco-editor/react";
import { useEffect, useRef } from "react";
import type * as Monaco from "monaco-editor";
import type { SqlDialect } from "../types/api";
import { bindSqlModelDialect, registerSqlLanguageSupport } from "./SqlEditorLanguageSupport";

interface SqlCodeEditorProps {
  value: string;
  onChange: (nextValue: string) => void;
  ariaLabel: string;
  dialect: SqlDialect;
}

/**
 * Wraps Monaco so the rest of the editor panel stays focused on playground concerns.
 */
export function SqlCodeEditor(props: SqlCodeEditorProps) {
  const editorRef = useRef<Monaco.editor.IStandaloneCodeEditor | null>(null);

  useEffect(() => {
    const model = editorRef.current?.getModel();
    if (!model) {
      return;
    }

    bindSqlModelDialect(model.uri.toString(), props.dialect);
  }, [props.dialect]);

  const handleEditorMount: OnMount = (editor) => {
    editorRef.current = editor;
    const model = editor.getModel();

    if (model) {
      bindSqlModelDialect(model.uri.toString(), props.dialect);
    }
  };

  return (
    <div className="sql-editor-shell">
      <MonacoEditor
        beforeMount={registerSqlLanguageSupport}
        className="sql-editor-monaco"
        defaultLanguage="sql"
        height="24rem"
        options={{
          ariaLabel: props.ariaLabel,
          automaticLayout: true,
          fontFamily: "Cascadia Code, Consolas, monospace",
          fontLigatures: false,
          fontSize: 15,
          lineNumbers: "on",
          minimap: {
            enabled: false
          },
          padding: {
            top: 16,
            bottom: 16
          },
          scrollBeyondLastLine: false,
          tabSize: 2,
          wordWrap: "on"
        }}
        theme="vs-dark"
        value={props.value}
        onChange={(value) => props.onChange(value ?? "")}
        onMount={handleEditorMount}
        path="sqm-playground.sql"
      />
    </div>
  );
}
