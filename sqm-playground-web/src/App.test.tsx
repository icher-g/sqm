import { render, screen, waitFor } from "@testing-library/react";
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

describe("App", () => {
  afterEach(() => {
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
      );

    render(<App />);

    expect(screen.getByText("Frontend shell")).toBeInTheDocument();
    expect(screen.getByText("Controls")).toBeInTheDocument();
    expect(screen.getByText("Editor")).toBeInTheDocument();
    expect(screen.getByText("Results")).toBeInTheDocument();
    expect(screen.getByLabelText("Source dialect")).toHaveValue("ansi");
    expect(screen.getByLabelText("Target dialect")).toHaveValue("postgresql");
    expect(screen.getByRole("button", { name: "Render" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Validate" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Transpile" })).toBeDisabled();

    await waitFor(() => {
      expect(screen.getByLabelText("Example")).toHaveValue("basic-select");
    });

    expect(screen.getByLabelText("SQL text")).toHaveValue("select id, name\nfrom customer\nwhere id = 1\norder by name");
    expect(screen.getByRole("button", { name: "Parse" })).toBeEnabled();
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

    await userEvent.click(screen.getByRole("tab", { name: "AST" }));
    expect(screen.getAllByText("SelectQuery").length).toBeGreaterThan(0);
    expect(screen.getAllByText("SelectItem").length).toBeGreaterThan(0);

    await userEvent.click(screen.getByRole("tab", { name: "JSON" }));
    expect(screen.getByRole("tabpanel", { name: "JSON" })).toHaveTextContent('"kind": "select"');

    await userEvent.click(screen.getByRole("tab", { name: "About Result" }));
    expect(screen.getByText("req-parse")).toBeInTheDocument();
    expect(screen.getByText("query")).toBeInTheDocument();
  });
});
