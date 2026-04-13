import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { SqlCodeEditor } from "./SqlCodeEditor";

const setModelMarkers = vi.fn();
const revealLineInCenter = vi.fn();
const setPosition = vi.fn();
const setSelection = vi.fn();
const focus = vi.fn();

vi.mock("@monaco-editor/react", () => ({
  default: function MonacoEditorMock(props: {
    value?: string;
    onChange?: (value: string) => void;
    onMount?: (
      editor: {
        focus: () => void;
        getModel: () => {
          uri: { toString: () => string };
          getFullModelRange: () => {
            startLineNumber: number;
            startColumn: number;
            endLineNumber: number;
            endColumn: number;
          };
        };
        revealLineInCenter: (lineNumber: number) => void;
        setPosition: (position: { lineNumber: number; column: number }) => void;
        setSelection: (selection: {
          startLineNumber: number;
          startColumn: number;
          endLineNumber: number;
          endColumn: number;
        }) => void;
      },
      monaco: unknown
    ) => void;
    beforeMount?: (monaco: unknown) => void;
    options?: {
      ariaLabel?: string;
    };
  }) {
    const monacoMock = {
      editor: {
        setModelMarkers
      },
      languages: {
        registerCompletionItemProvider: vi.fn(),
        CompletionItemKind: {
          Field: 3,
          Function: 4,
          Keyword: 1,
          Snippet: 2,
          Struct: 5,
          Variable: 6
        },
        CompletionItemInsertTextRule: {
          InsertAsSnippet: 4
        }
      },
      MarkerSeverity: {
        Error: 8,
        Info: 2,
        Warning: 4
      }
    };

    props.beforeMount?.(monacoMock);
    props.onMount?.(
      {
        focus,
        getModel: () => ({
          uri: {
            toString: () => "inmemory://model.sql"
          },
          getFullModelRange: () => ({
            startLineNumber: 1,
            startColumn: 1,
            endLineNumber: 3,
            endColumn: 10
          })
        }),
        revealLineInCenter,
        setPosition,
        setSelection
      },
      monacoMock
    );

    return (
      <textarea
        aria-label={props.options?.ariaLabel ?? "Editor"}
        value={props.value ?? ""}
        onChange={(event) => props.onChange?.(event.target.value)}
      />
    );
  }
}));

describe("SqlCodeEditor", () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it("renders the SQL value and forwards edits", async () => {
    const handleChange = vi.fn();

    function Harness() {
      const [value, setValue] = useState("select 1");

      return (
        <SqlCodeEditor
          ariaLabel="SQL text"
          dialect="ansi"
          diagnostics={[]}
          focusedDiagnostic={null}
          value={value}
          onChange={(nextValue) => {
            handleChange(nextValue);
            setValue(nextValue);
          }}
        />
      );
    }

    render(<Harness />);

    const editor = screen.getByLabelText("SQL text");
    expect(editor).toHaveValue("select 1");

    await userEvent.clear(editor);
    await userEvent.type(editor, "select 2");

    expect(handleChange).toHaveBeenLastCalledWith("select 2");
  });

  it("maps diagnostics into Monaco markers and navigates to selected diagnostics", () => {
    render(
      <SqlCodeEditor
        ariaLabel="SQL text"
        dialect="ansi"
        diagnostics={[
          {
            severity: "error",
            phase: "parse",
            code: "PARSE_ERROR",
            message: "Unexpected token",
            line: 2,
            column: 7
          }
        ]}
        focusedDiagnostic={{
          diagnostic: {
            severity: "error",
            phase: "parse",
            code: "PARSE_ERROR",
            message: "Unexpected token",
            line: 2,
            column: 7
          },
          version: 1
        }}
        value="select\nfrom"
        onChange={() => {}}
      />
    );

    expect(setModelMarkers).toHaveBeenCalled();
    expect(setPosition).toHaveBeenCalledWith({
      lineNumber: 2,
      column: 7
    });
    expect(revealLineInCenter).toHaveBeenCalledWith(2);
    expect(focus).toHaveBeenCalled();
  });

  it("falls back to the start of the statement when diagnostics have no source position", () => {
    render(
      <SqlCodeEditor
        ariaLabel="SQL text"
        dialect="ansi"
        diagnostics={[
          {
            severity: "error",
            phase: "render",
            code: "RENDER_ERROR",
            message: "Dialect does not support this statement",
            line: null,
            column: null
          }
        ]}
        focusedDiagnostic={{
          diagnostic: {
            severity: "error",
            phase: "render",
            code: "RENDER_ERROR",
            message: "Dialect does not support this statement",
            line: null,
            column: null
          },
          version: 1
        }}
        value="update orders set status = 'priority'"
        onChange={() => {}}
      />
    );

    expect(setSelection).toHaveBeenCalledWith({
      startLineNumber: 1,
      startColumn: 1,
      endLineNumber: 3,
      endColumn: 10
    });
    expect(setPosition).toHaveBeenCalledWith({
      lineNumber: 1,
      column: 1
    });
    expect(revealLineInCenter).toHaveBeenCalledWith(1);
    expect(focus).toHaveBeenCalled();
  });
});
