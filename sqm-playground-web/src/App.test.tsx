import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import App from "./App";

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

const EXAMPLES_RESPONSE = {
  requestId: "req-1",
  success: true,
  durationMs: 1,
  diagnostics: [],
  examples: [
    {
      id: "basic-select",
      title: "Basic SELECT",
      dialect: "ansi",
      sql: "select id, name\nfrom customer\nwhere id = 1\norder by name"
    },
    {
      id: "pg-distinct",
      title: "Distinct ON",
      dialect: "postgresql",
      sql: "select distinct on (id) id\nfrom customer\norder by id"
    }
  ]
};

const PARSE_RESPONSE = {
  requestId: "req-parse",
  success: true,
  durationMs: 12,
  statementKind: "query",
  sqmJson: "{\n  \"kind\": \"select\"\n}",
  sqmDsl: "public final class MyQuery {\n    public static SelectQuery getStatement() {\n        return builder.select(\n            star()\n        );\n    }\n}",
  ast: {
    nodeType: "SelectQuery",
    nodeInterface: "io.sqm.core.api.Query",
    kind: "select",
    category: "statement",
    label: "SelectQuery",
    details: [
      {
        name: "quantifier",
        value: "all"
      }
    ],
    children: [
      {
        slot: "items",
        multiple: true,
        nodes: [
          {
            nodeType: "SelectItem",
            nodeInterface: "io.sqm.core.api.SelectItem",
            kind: null,
            category: "expression",
            label: "SelectItem",
            details: [],
            children: []
          }
        ]
      }
    ]
  },
  summary: {
    rootNodeType: "SelectQuery",
    rootInterface: "io.sqm.core.api.Query"
  },
  diagnostics: []
};

const RENDER_RESPONSE = {
  requestId: "req-render",
  success: true,
  durationMs: 9,
  renderedSql: "select distinct on (id) id\nfrom customer\norder by id",
  params: [],
  diagnostics: []
};

const FORMAT_RESPONSE = {
  requestId: "req-format",
  success: true,
  durationMs: 8,
  renderedSql: "select distinct on (id) id\nfrom customer\norder by id",
  params: [],
  diagnostics: []
};

const VALIDATE_RESPONSE = {
  requestId: "req-validate",
  success: true,
  durationMs: 5,
  valid: true,
  diagnostics: []
};

const TRANSPILE_RESPONSE = {
  requestId: "req-transpile",
  success: true,
  durationMs: 11,
  outcome: "approximate",
  renderedSql: "select id\nfrom customer",
  params: [1],
  diagnostics: [
    {
      severity: "warning",
      phase: "transpile",
      code: "APPROXIMATE_TRANSPILE",
      message: "Output was approximated for the target dialect",
      line: null,
      column: null
    }
  ]
};

const TRANSPILE_FAILURE_RESPONSE = {
  requestId: "req-transpile-fail",
  success: false,
  durationMs: 6,
  outcome: "unsupported",
  renderedSql: null,
  params: [],
  diagnostics: [
    {
      severity: "error",
      phase: "transpile",
      code: "TRANSPILE_ERROR",
      message: "Cannot rewrite statement for target dialect",
      line: null,
      column: null
    }
  ]
};

const PARSE_FAILURE_RESPONSE = {
  requestId: "req-parse-fail",
  success: false,
  durationMs: 4,
  statementKind: null,
  sqmJson: null,
  sqmDsl: null,
  ast: null,
  summary: null,
  diagnostics: [
    {
      severity: "error",
      phase: "parse",
      code: "PARSE_ERROR",
      message: "Unexpected token",
      line: 1,
      column: 8
    }
  ]
};

const RENDER_FAILURE_RESPONSE = {
  requestId: "req-render-fail",
  success: false,
  durationMs: 3,
  renderedSql: null,
  params: [],
  diagnostics: [
    {
      severity: "error",
      phase: "render",
      code: "RENDER_ERROR",
      message: "Dialect does not support this statement",
      line: null,
      column: null
    }
  ]
};

describe("App", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
    vi.clearAllMocks();
    window.history.replaceState({}, "", "/");
  });

  it("loads examples and parses SQL through the backend", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(
        new Response(JSON.stringify(EXAMPLES_RESPONSE), {
          status: 200,
          headers: {
            "Content-Type": "application/json"
          }
        })
      )
      .mockResolvedValueOnce(
        new Response(JSON.stringify(FORMAT_RESPONSE), {
          status: 200,
          headers: {
            "Content-Type": "application/json"
          }
        })
      )
      .mockResolvedValueOnce(
        new Response(JSON.stringify(PARSE_RESPONSE), {
          status: 200,
          headers: {
            "Content-Type": "application/json"
          }
        })
      )
      .mockResolvedValueOnce(
        new Response(JSON.stringify(RENDER_RESPONSE), {
          status: 200,
          headers: {
            "Content-Type": "application/json"
          }
        })
      )
      .mockResolvedValueOnce(
        new Response(JSON.stringify(VALIDATE_RESPONSE), {
          status: 200,
          headers: {
            "Content-Type": "application/json"
          }
        })
      )
      .mockResolvedValueOnce(
        new Response(JSON.stringify(TRANSPILE_RESPONSE), {
          status: 200,
          headers: {
            "Content-Type": "application/json"
          }
        })
      );

    render(<App />);

    expect(screen.getByText("Explore SQL Through SQM")).toBeInTheDocument();
    expect(screen.getByText("Editor")).toBeInTheDocument();
    expect(screen.getByText("Results")).toBeInTheDocument();
    expect(screen.getByText("AST, DSL, JSON, SQL, diagnostics")).toBeInTheDocument();
    expect(screen.getByText("Examples, dialects, actions")).toBeInTheDocument();
    expect(screen.getByLabelText("Source dialect")).toHaveValue("ansi");
    expect(screen.getByLabelText("Target dialect")).toHaveValue("postgresql");
    expect(screen.getByRole("button", { name: "Inline" })).toHaveAttribute("aria-pressed", "true");
    expect(screen.getByRole("button", { name: "Transpile" })).toBeEnabled();

    await waitFor(() => {
      expect(screen.getByLabelText("Example")).toHaveValue("basic-select");
    });

    expect(screen.getByLabelText("SQL text")).toHaveValue("select id, name\nfrom customer\nwhere id = 1\norder by name");
    expect(screen.getByRole("button", { name: "Parse" })).toBeEnabled();
    expect(screen.getByRole("button", { name: "Render" })).toBeEnabled();
    expect(screen.getByRole("button", { name: "Validate" })).toBeEnabled();
    expect(screen.getByRole("button", { name: "Format SQL" })).toBeEnabled();
    expect(screen.getByRole("tab", { name: "AST" })).toHaveAttribute("aria-selected", "true");
    expect(screen.getByRole("tabpanel", { name: "AST" })).toBeInTheDocument();

    await userEvent.selectOptions(screen.getByLabelText("Example"), "pg-distinct");
    await userEvent.click(screen.getByRole("tab", { name: "Diagnostics" }));

    expect(screen.getByLabelText("SQL text")).toHaveValue("select distinct on (id) id\nfrom customer\norder by id");
    expect(screen.getByLabelText("Source dialect")).toHaveValue("postgresql");
    expect(screen.getByRole("tab", { name: "Diagnostics" })).toHaveAttribute("aria-selected", "true");
    expect(screen.getByRole("tabpanel", { name: "Diagnostics" })).toBeInTheDocument();
    expect(screen.queryByRole("tabpanel", { name: "AST" })).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Format SQL" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenNthCalledWith(
        2,
        "http://localhost:8080/sqm/playground/api/v1/render",
        expect.objectContaining({
          method: "POST",
          body: JSON.stringify({
            sql: "select distinct on (id) id\nfrom customer\norder by id",
            sourceDialect: "postgresql",
            targetDialect: "postgresql",
            parameterizationMode: "inline"
          })
        })
      );
    });

    expect(screen.getByLabelText("SQL text")).toHaveValue("select distinct on (id) id\nfrom customer\norder by id");
    expect(screen.getByRole("button", { name: "Format SQL" })).toHaveClass("button-primary");

    await userEvent.click(screen.getByRole("button", { name: "Parse" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenNthCalledWith(
        3,
        "http://localhost:8080/sqm/playground/api/v1/parse",
        expect.objectContaining({
          method: "POST",
          body: JSON.stringify({
            sql: "select distinct on (id) id\nfrom customer\norder by id",
            dialect: "postgresql"
          })
        })
      );
    });

    expect(screen.getByRole("button", { name: "Parse" })).toHaveClass("button-primary");
    expect(screen.getByRole("button", { name: "Render" })).not.toHaveClass("button-primary");
    expect(screen.getByRole("tab", { name: "AST" })).toHaveAttribute("aria-selected", "true");
    expect(screen.getByRole("tabpanel", { name: "AST" })).toBeInTheDocument();

    expect(screen.getAllByText("SelectQuery").length).toBeGreaterThan(0);
    expect(screen.getAllByText("SelectItem").length).toBeGreaterThan(0);

    await userEvent.click(screen.getByRole("tab", { name: "JSON" }));
    expect(screen.getByRole("tabpanel", { name: "JSON" })).toHaveTextContent("kind");
    expect(screen.getByRole("tabpanel", { name: "JSON" })).toHaveTextContent('"select"');

    await userEvent.click(screen.getByRole("tab", { name: "About Result" }));
    expect(screen.getByText("req-parse")).toBeInTheDocument();
    expect(screen.getByText("query")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Bind" }));
    expect(screen.getByRole("button", { name: "Bind" })).toHaveAttribute("aria-pressed", "true");

    await userEvent.click(screen.getByRole("button", { name: "Render" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenNthCalledWith(
        4,
        "http://localhost:8080/sqm/playground/api/v1/render",
        expect.objectContaining({
          method: "POST",
          body: JSON.stringify({
            sql: "select distinct on (id) id\nfrom customer\norder by id",
            sourceDialect: "postgresql",
            targetDialect: "postgresql",
            parameterizationMode: "bind"
          })
        })
      );
    });

    expect(screen.getByRole("button", { name: "Render" })).toHaveClass("button-primary");
    expect(screen.getByRole("button", { name: "Parse" })).not.toHaveClass("button-primary");
    expect(screen.getByRole("tab", { name: "Rendered SQL" })).toHaveAttribute("aria-selected", "true");
    expect(screen.getByRole("tabpanel", { name: "Rendered SQL" })).toHaveTextContent("Dialect: PostgreSQL");
    expect(screen.getByRole("tabpanel", { name: "Rendered SQL" })).toHaveTextContent("select distinct on (id) id");

    await userEvent.click(screen.getByRole("tab", { name: "About Result" }));
    expect(screen.getByText("req-render")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Validate" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenNthCalledWith(
        5,
        "http://localhost:8080/sqm/playground/api/v1/validate",
        expect.objectContaining({
          method: "POST",
          body: JSON.stringify({
            sql: "select distinct on (id) id\nfrom customer\norder by id",
            dialect: "postgresql"
          })
        })
      );
    });

    expect(screen.getByRole("button", { name: "Validate" })).toHaveClass("button-primary");
    expect(screen.getByRole("tab", { name: "Diagnostics" })).toHaveAttribute("aria-selected", "true");
    expect(screen.getByRole("tabpanel", { name: "Diagnostics" })).toHaveTextContent("Validation succeeded with no diagnostics.");

    await userEvent.click(screen.getByRole("tab", { name: "About Result" }));
    expect(screen.getByText("req-validate")).toBeInTheDocument();
    expect(screen.getByText("valid")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Transpile" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenNthCalledWith(
        6,
        "http://localhost:8080/sqm/playground/api/v1/transpile",
        expect.objectContaining({
          method: "POST",
          body: JSON.stringify({
            sql: "select distinct on (id) id\nfrom customer\norder by id",
            sourceDialect: "postgresql",
            targetDialect: "postgresql",
            parameterizationMode: "bind"
          })
        })
      );
    });

    expect(screen.getByRole("button", { name: "Transpile" })).toHaveClass("button-primary");
    expect(screen.getByRole("tab", { name: "Rendered SQL" })).toHaveAttribute("aria-selected", "true");
    expect(screen.getByRole("tabpanel", { name: "Rendered SQL" })).toHaveTextContent("Dialect: PostgreSQL");
    expect(screen.getByRole("tabpanel", { name: "Rendered SQL" })).toHaveTextContent("select id");
    expect(screen.getByRole("tabpanel", { name: "Rendered SQL" })).toHaveTextContent("from customer");
    expect(screen.getByRole("tabpanel", { name: "Rendered SQL" })).toHaveTextContent("Parameters");

    await userEvent.click(screen.getByRole("tab", { name: "Diagnostics" }));
    expect(screen.getByRole("tabpanel", { name: "Diagnostics" })).toHaveTextContent("APPROXIMATE_TRANSPILE");
    expect(screen.getByRole("tabpanel", { name: "Diagnostics" })).toHaveTextContent("Output was approximated for the target dialect");

    await userEvent.click(screen.getByRole("tab", { name: "About Result" }));
    expect(screen.getByText("req-transpile")).toBeInTheDocument();
    expect(screen.getByText("approximate")).toBeInTheDocument();
  });

  it("switches to diagnostics when parse or render returns a failure response", async () => {
    vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(
        new Response(JSON.stringify(EXAMPLES_RESPONSE), {
          status: 200,
          headers: {
            "Content-Type": "application/json"
          }
        })
      )
      .mockResolvedValueOnce(
        new Response(JSON.stringify(PARSE_FAILURE_RESPONSE), {
          status: 200,
          headers: {
            "Content-Type": "application/json"
          }
        })
      )
      .mockResolvedValueOnce(
        new Response(JSON.stringify(RENDER_FAILURE_RESPONSE), {
          status: 200,
          headers: {
            "Content-Type": "application/json"
          }
        })
      );

    render(<App />);

    await waitFor(() => {
      expect(screen.getByLabelText("Example")).toHaveValue("basic-select");
    });

    await userEvent.click(screen.getByRole("button", { name: "Parse" }));

    await waitFor(() => {
      expect(screen.getByRole("tab", { name: "Diagnostics" })).toHaveAttribute("aria-selected", "true");
    });

    expect(screen.getByRole("tabpanel", { name: "Diagnostics" })).toHaveTextContent("PARSE_ERROR");
    expect(screen.getByRole("tabpanel", { name: "Diagnostics" })).toHaveTextContent("Unexpected token");
    expect(setModelMarkers).toHaveBeenCalled();

    await userEvent.click(screen.getByRole("button", { name: /PARSE_ERROR: Unexpected token/i }));

    expect(setPosition).toHaveBeenCalledWith({
      lineNumber: 1,
      column: 8
    });
    expect(revealLineInCenter).toHaveBeenCalledWith(1);

    setPosition.mockClear();
    revealLineInCenter.mockClear();
    focus.mockClear();

    await userEvent.click(screen.getByRole("button", { name: "Render" }));

    await waitFor(() => {
      expect(screen.getByRole("tab", { name: "Diagnostics" })).toHaveAttribute("aria-selected", "true");
    });

    expect(screen.getByRole("tabpanel", { name: "Diagnostics" })).toHaveTextContent("RENDER_ERROR");
    expect(screen.getByRole("tabpanel", { name: "Diagnostics" })).toHaveTextContent("Dialect does not support this statement");

    await userEvent.click(screen.getByRole("button", { name: /RENDER_ERROR: Dialect does not support this statement/i }));

    expect(setPosition).toHaveBeenCalledWith({
      lineNumber: 1,
      column: 1
    });
    expect(revealLineInCenter).toHaveBeenCalledWith(1);
    expect(focus).toHaveBeenCalled();
  });

  it("uses the target dialect as the render input dialect", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(
        new Response(JSON.stringify(EXAMPLES_RESPONSE), {
          status: 200,
          headers: {
            "Content-Type": "application/json"
          }
        })
      )
      .mockResolvedValueOnce(
        new Response(JSON.stringify(RENDER_RESPONSE), {
          status: 200,
          headers: {
            "Content-Type": "application/json"
          }
        })
      );

    render(<App />);

    await waitFor(() => {
      expect(screen.getByLabelText("Example")).toHaveValue("basic-select");
    });

    expect(screen.getByLabelText("Source dialect")).toHaveValue("ansi");
    expect(screen.getByLabelText("Target dialect")).toHaveValue("postgresql");

    await userEvent.click(screen.getByRole("button", { name: "Render" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenNthCalledWith(
        2,
        "http://localhost:8080/sqm/playground/api/v1/render",
        expect.objectContaining({
          method: "POST",
          body: JSON.stringify({
            sql: "select id, name\nfrom customer\nwhere id = 1\norder by name",
            sourceDialect: "postgresql",
            targetDialect: "postgresql",
            parameterizationMode: "inline"
          })
        })
      );
    });
  });

  it("hydrates editor state from shareable URLs", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
      new Response(JSON.stringify(EXAMPLES_RESPONSE), {
        status: 200,
        headers: {
          "Content-Type": "application/json"
        }
      })
    );

    window.history.replaceState(
      {},
      "",
      "/?sql=select%201&source=mysql&target=sqlserver&example=pg-distinct&tab=renderedSql"
    );

    render(<App />);

    await waitFor(() => {
      expect(screen.getByLabelText("Example")).toHaveValue("pg-distinct");
    });

    expect(screen.getByLabelText("SQL text")).toHaveValue("select 1");
    expect(screen.getByLabelText("Source dialect")).toHaveValue("mysql");
    expect(screen.getByLabelText("Target dialect")).toHaveValue("sqlserver");
    expect(screen.getByRole("tab", { name: "Rendered SQL" })).toHaveAttribute("aria-selected", "true");
  });

  it("selects the SQL editor when transpile diagnostics have no source position", async () => {
    vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(
        new Response(JSON.stringify(EXAMPLES_RESPONSE), {
          status: 200,
          headers: {
            "Content-Type": "application/json"
          }
        })
      )
      .mockResolvedValueOnce(
        new Response(JSON.stringify(TRANSPILE_FAILURE_RESPONSE), {
          status: 200,
          headers: {
            "Content-Type": "application/json"
          }
        })
      );

    render(<App />);

    await waitFor(() => {
      expect(screen.getByLabelText("Example")).toHaveValue("basic-select");
    });

    await userEvent.click(screen.getByRole("button", { name: "Transpile" }));

    await waitFor(() => {
      expect(screen.getByRole("tab", { name: "Diagnostics" })).toHaveAttribute("aria-selected", "true");
    });

    await userEvent.click(screen.getByRole("button", { name: /TRANSPILE_ERROR: Cannot rewrite statement for target dialect/i }));

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
