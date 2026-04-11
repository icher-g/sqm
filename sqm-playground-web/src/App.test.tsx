import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import App from "./App";

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
  diagnostics: []
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
      );

    render(<App />);

    expect(screen.getByText("Frontend shell")).toBeInTheDocument();
    expect(screen.getByText("Editor")).toBeInTheDocument();
    expect(screen.getByText("Results")).toBeInTheDocument();
    expect(screen.getByText("Choose an example, set the relevant dialects, and run actions directly above the SQL text.")).toBeInTheDocument();
    expect(screen.getByLabelText("Source dialect")).toHaveValue("ansi");
    expect(screen.getByLabelText("Target dialect")).toHaveValue("postgresql");
    expect(screen.getByRole("button", { name: "Validate" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Transpile" })).toBeDisabled();

    await waitFor(() => {
      expect(screen.getByLabelText("Example")).toHaveValue("basic-select");
    });

    expect(screen.getByLabelText("SQL text")).toHaveValue("select id, name\nfrom customer\nwhere id = 1\norder by name");
    expect(screen.getByRole("button", { name: "Parse" })).toBeEnabled();
    expect(screen.getByRole("button", { name: "Render" })).toBeEnabled();
    expect(screen.getByRole("tab", { name: "AST" })).toHaveAttribute("aria-selected", "true");
    expect(screen.getByRole("tabpanel", { name: "AST" })).toBeInTheDocument();

    await userEvent.selectOptions(screen.getByLabelText("Example"), "pg-distinct");
    await userEvent.click(screen.getByRole("tab", { name: "Diagnostics" }));

    expect(screen.getByLabelText("SQL text")).toHaveValue("select distinct on (id) id\nfrom customer\norder by id");
    expect(screen.getByLabelText("Source dialect")).toHaveValue("postgresql");
    expect(screen.getByRole("tab", { name: "Diagnostics" })).toHaveAttribute("aria-selected", "true");
    expect(screen.getByRole("tabpanel", { name: "Diagnostics" })).toBeInTheDocument();
    expect(screen.queryByRole("tabpanel", { name: "AST" })).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Parse" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenNthCalledWith(
        2,
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
    expect(screen.getByRole("tabpanel", { name: "JSON" })).toHaveTextContent('"kind": "select"');

    await userEvent.click(screen.getByRole("tab", { name: "About Result" }));
    expect(screen.getByText("req-parse")).toBeInTheDocument();
    expect(screen.getByText("query")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Render" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenNthCalledWith(
        3,
        "http://localhost:8080/sqm/playground/api/v1/render",
        expect.objectContaining({
          method: "POST",
          body: JSON.stringify({
            sql: "select distinct on (id) id\nfrom customer\norder by id",
            sourceDialect: "postgresql",
            targetDialect: "postgresql"
          })
        })
      );
    });

    expect(screen.getByRole("button", { name: "Render" })).toHaveClass("button-primary");
    expect(screen.getByRole("button", { name: "Parse" })).not.toHaveClass("button-primary");
    expect(screen.getByRole("tab", { name: "Rendered SQL" })).toHaveAttribute("aria-selected", "true");
    expect(screen.getByRole("tabpanel", { name: "Rendered SQL" })).toHaveTextContent("select distinct on (id) id");
    expect(screen.queryByText("SelectQuery")).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole("tab", { name: "About Result" }));
    expect(screen.getByText("req-render")).toBeInTheDocument();
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

    await userEvent.click(screen.getByRole("button", { name: "Render" }));

    await waitFor(() => {
      expect(screen.getByRole("tab", { name: "Diagnostics" })).toHaveAttribute("aria-selected", "true");
    });

    expect(screen.getByRole("tabpanel", { name: "Diagnostics" })).toHaveTextContent("RENDER_ERROR");
    expect(screen.getByRole("tabpanel", { name: "Diagnostics" })).toHaveTextContent("Dialect does not support this statement");
  });
});
