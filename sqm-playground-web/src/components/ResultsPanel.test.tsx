import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ResultsPanel } from "./ResultsPanel";
import type { ParseResponseDto, RenderResponseDto } from "../types/api";

const PARSE_RESPONSE: ParseResponseDto = {
  requestId: "req-parse",
  success: true,
  durationMs: 12,
  statementKind: "query",
  multiStatement: false,
  sqmJson: "{\n  \"kind\": \"select\"\n}",
  sqmDsl: "builder.select(star())",
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

const MULTI_PARSE_RESPONSE: ParseResponseDto = {
  ...PARSE_RESPONSE,
  statementKind: "sequence",
  multiStatement: true,
  sqmDsl: [
    "// Statement 1",
    "public static SelectQuery getStatement() {",
    "  return select(literal(1)).build();",
    "}",
    "",
    "// Statement 2",
    "public static SelectQuery getStatement() {",
    "  return select(literal(2)).build();",
    "}"
  ].join("\n"),
  sqmJson: JSON.stringify({
    kind: "statementSequence",
    statements: [
      {
        kind: "select",
        value: 1
      },
      {
        kind: "select",
        value: 2
      }
    ]
  }, null, 2),
  ast: {
    ...PARSE_RESPONSE.ast!,
    nodeType: "StatementSequence",
    label: "StatementSequence",
    category: "statementSequence",
    children: [
      {
        slot: "statements",
        multiple: true,
        nodes: [
          PARSE_RESPONSE.ast!,
          {
            ...PARSE_RESPONSE.ast!,
            label: "SecondSelect",
            nodeType: "SecondSelect"
          }
        ]
      }
    ]
  },
  summary: {
    rootNodeType: "StatementSequence",
    rootInterface: "io.sqm.core.StatementSequence"
  }
};

const PARAMETERIZED_RENDER_RESPONSE: RenderResponseDto = {
  requestId: "req-render",
  success: true,
  durationMs: 7,
  renderedSql: "SELECT ?",
  params: [7, "alice"],
  diagnostics: []
};

describe("ResultsPanel", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("copies AST and JSON using plain-text export formats", async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    Object.assign(navigator, {
      clipboard: {
        writeText
      }
    });

    const noop = () => {};

    const { rerender } = render(
      <ResultsPanel
        activeResultTab="ast"
        onResultTabChange={noop}
        parseResponse={PARSE_RESPONSE}
        parseLoading={false}
        parseError={null}
        renderResponse={null}
        renderedSqlDialect={null}
        renderedSqlTimestamp={null}
        renderLoading={false}
        renderError={null}
        transpileResponse={null}
        transpileLoading={false}
        transpileError={null}
        validateResponse={null}
        validateLoading={false}
        validateError={null}
        onDiagnosticSelect={noop}
      />
    );

    await userEvent.click(screen.getByRole("button", { name: "Copy AST" }));

    expect(writeText).toHaveBeenNthCalledWith(
      1,
      ["SelectQuery", "  quantifier: all", "  items[]", "    SelectItem"].join("\n")
    );
    expect(screen.getByRole("button", { name: "Copied AST" })).toBeInTheDocument();

    rerender(
      <ResultsPanel
        activeResultTab="json"
        onResultTabChange={noop}
        parseResponse={PARSE_RESPONSE}
        parseLoading={false}
        parseError={null}
        renderResponse={null}
        renderedSqlDialect={null}
        renderedSqlTimestamp={null}
        renderLoading={false}
        renderError={null}
        transpileResponse={null}
        transpileLoading={false}
        transpileError={null}
        validateResponse={null}
        validateLoading={false}
        validateError={null}
        onDiagnosticSelect={noop}
      />
    );

    await userEvent.click(screen.getByRole("button", { name: "Copy JSON" }));

    expect(writeText).toHaveBeenNthCalledWith(2, PARSE_RESPONSE.sqmJson);
    expect(screen.getByRole("button", { name: "Copied JSON" })).toBeInTheDocument();
  });

  it("switches AST, DSL, and JSON views between statement sequence entries", async () => {
    const noop = () => {};
    const writeText = vi.fn().mockResolvedValue(undefined);
    Object.assign(navigator, {
      clipboard: {
        writeText
      }
    });

    const { rerender } = render(
      <ResultsPanel
        activeResultTab="ast"
        onResultTabChange={noop}
        parseResponse={MULTI_PARSE_RESPONSE}
        parseLoading={false}
        parseError={null}
        renderResponse={null}
        renderedSqlDialect={null}
        renderedSqlTimestamp={null}
        renderLoading={false}
        renderError={null}
        transpileResponse={null}
        transpileLoading={false}
        transpileError={null}
        validateResponse={null}
        validateLoading={false}
        validateError={null}
        onDiagnosticSelect={noop}
      />
    );

    await userEvent.selectOptions(screen.getByLabelText("AST statement view"), "2");

    expect(screen.getByText("SecondSelect")).toBeInTheDocument();

    rerender(
      <ResultsPanel
        activeResultTab="dsl"
        onResultTabChange={noop}
        parseResponse={MULTI_PARSE_RESPONSE}
        parseLoading={false}
        parseError={null}
        renderResponse={null}
        renderedSqlDialect={null}
        renderedSqlTimestamp={null}
        renderLoading={false}
        renderError={null}
        transpileResponse={null}
        transpileLoading={false}
        transpileError={null}
        validateResponse={null}
        validateLoading={false}
        validateError={null}
        onDiagnosticSelect={noop}
      />
    );

    const dslPanel = screen.getByRole("tabpanel", { name: "DSL" });
    expect(dslPanel.textContent).toContain("literal(2)");
    expect(dslPanel.textContent).not.toContain("literal(1)");

    rerender(
      <ResultsPanel
        activeResultTab="json"
        onResultTabChange={noop}
        parseResponse={MULTI_PARSE_RESPONSE}
        parseLoading={false}
        parseError={null}
        renderResponse={null}
        renderedSqlDialect={null}
        renderedSqlTimestamp={null}
        renderLoading={false}
        renderError={null}
        transpileResponse={null}
        transpileLoading={false}
        transpileError={null}
        validateResponse={null}
        validateLoading={false}
        validateError={null}
        onDiagnosticSelect={noop}
      />
    );

    await userEvent.click(screen.getByRole("button", { name: "Copy JSON" }));

    expect(writeText).toHaveBeenCalledWith(JSON.stringify({ kind: "select", value: 2 }, null, 2));
  });

  it("shows bind parameters returned by render responses", () => {
    const noop = () => {};

    render(
      <ResultsPanel
        activeResultTab="renderedSql"
        onResultTabChange={noop}
        parseResponse={null}
        parseLoading={false}
        parseError={null}
        renderResponse={PARAMETERIZED_RENDER_RESPONSE}
        renderedSqlDialect="postgresql"
        renderedSqlTimestamp={null}
        renderLoading={false}
        renderError={null}
        transpileResponse={null}
        transpileLoading={false}
        transpileError={null}
        validateResponse={null}
        validateLoading={false}
        validateError={null}
        onDiagnosticSelect={noop}
      />
    );

    const renderedPanel = screen.getByRole("tabpanel", { name: "Rendered SQL" });

    expect(renderedPanel).toHaveTextContent("SELECT ?");
    expect(renderedPanel).toHaveTextContent("Parameters");
    expect(renderedPanel).toHaveTextContent("alice");
  });
});
