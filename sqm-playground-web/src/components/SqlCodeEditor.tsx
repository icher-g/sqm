import MonacoEditor, { type OnMount } from "@monaco-editor/react";
import { useEffect, useRef } from "react";
import type * as Monaco from "monaco-editor";
import type { PlaygroundDiagnosticDto, SqlDialect } from "../types/api";
import { bindSqlModelDialect, registerSqlLanguageSupport } from "./SqlEditorLanguageSupport";

interface SqlCodeEditorProps {
  value: string;
  onChange: (nextValue: string) => void;
  ariaLabel: string;
  dialect: SqlDialect;
  diagnostics: PlaygroundDiagnosticDto[];
  focusedDiagnostic: { diagnostic: PlaygroundDiagnosticDto; version: number } | null;
}

/**
 * Wraps Monaco so the rest of the editor panel stays focused on playground concerns.
 */
export function SqlCodeEditor(props: SqlCodeEditorProps) {
  const editorRef = useRef<Monaco.editor.IStandaloneCodeEditor | null>(null);
  const monacoRef = useRef<typeof Monaco | null>(null);

  useEffect(() => {
    const model = editorRef.current?.getModel();
    if (!model) {
      return;
    }

    bindSqlModelDialect(model.uri.toString(), props.dialect);
  }, [props.dialect]);

  useEffect(() => {
    const monaco = monacoRef.current;
    const model = editorRef.current?.getModel();
    if (!monaco || !model) {
      return;
    }

    monaco.editor.setModelMarkers(
      model,
      "sqm-playground",
      props.diagnostics
        .filter((diagnostic) => diagnostic.line !== null && diagnostic.column !== null)
        .map((diagnostic) => ({
          message: `${diagnostic.code}: ${diagnostic.message}`,
          severity: toMarkerSeverity(monaco, diagnostic.severity),
          startLineNumber: diagnostic.line!,
          startColumn: diagnostic.column!,
          endLineNumber: diagnostic.line!,
          endColumn: diagnostic.column! + 1
        }))
    );
  }, [props.diagnostics]);

  useEffect(() => {
    const selection = props.focusedDiagnostic;
    if (!selection) {
      return;
    }

    if (selection.diagnostic.line === null || selection.diagnostic.column === null) {
      focusEditorWithFallbackSelection();
      return;
    }

    const position = toDiagnosticPosition(selection.diagnostic);

    editorRef.current?.focus();
    editorRef.current?.setPosition({
      lineNumber: position.lineNumber,
      column: position.column
    });
    editorRef.current?.revealLineInCenter(position.lineNumber);
  }, [props.focusedDiagnostic]);

  const handleEditorMount: OnMount = (editor, monaco) => {
    editorRef.current = editor;
    monacoRef.current = monaco;
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

  function focusEditorWithFallbackSelection() {
    const editor = editorRef.current;
    const model = editor?.getModel();
    if (!editor || !model) {
      return;
    }

    editor.focus();
    editor.setSelection(model.getFullModelRange());
    editor.setPosition({
      lineNumber: 1,
      column: 1
    });
    editor.revealLineInCenter(1);
  }
}

function toMarkerSeverity(monaco: typeof Monaco, severity: PlaygroundDiagnosticDto["severity"]) {
  switch (severity) {
    case "info":
      return monaco.MarkerSeverity.Info;
    case "warning":
      return monaco.MarkerSeverity.Warning;
    case "error":
    default:
      return monaco.MarkerSeverity.Error;
  }
}

function toDiagnosticPosition(diagnostic: PlaygroundDiagnosticDto) {
  return {
    lineNumber: diagnostic.line ?? 1,
    column: diagnostic.column ?? 1
  };
}
