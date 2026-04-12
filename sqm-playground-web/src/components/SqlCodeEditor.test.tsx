import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import { describe, expect, it, vi } from "vitest";
import { SqlCodeEditor } from "./SqlCodeEditor";

vi.mock("@monaco-editor/react", () => ({
  default: function MonacoEditorMock(props: {
    value?: string;
    onChange?: (value: string) => void;
    onMount?: (editor: { getModel: () => { uri: { toString: () => string } } }, monaco: unknown) => void;
    beforeMount?: (monaco: unknown) => void;
    options?: {
      ariaLabel?: string;
    };
  }) {
    const monacoMock = {
      languages: {
        registerCompletionItemProvider: vi.fn(),
        CompletionItemKind: {
          Keyword: 1,
          Snippet: 2
        },
        CompletionItemInsertTextRule: {
          InsertAsSnippet: 4
        }
      }
    };

    props.beforeMount?.(monacoMock);
    props.onMount?.(
      {
        getModel: () => ({
          uri: {
            toString: () => "inmemory://model.sql"
          }
        })
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
  it("renders the SQL value and forwards edits", async () => {
    const handleChange = vi.fn();

    function Harness() {
      const [value, setValue] = useState("select 1");

      return (
        <SqlCodeEditor
          ariaLabel="SQL text"
          dialect="ansi"
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
});
