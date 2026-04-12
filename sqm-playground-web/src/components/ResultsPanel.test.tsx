import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ResultsPanel } from "./ResultsPanel";
import type { ParseResponseDto } from "../types/api";

const PARSE_RESPONSE: ParseResponseDto = {
  requestId: "req-parse",
  success: true,
  durationMs: 12,
  statementKind: "query",
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
        renderLoading={false}
        renderError={null}
        transpileResponse={null}
        transpileLoading={false}
        transpileError={null}
        validateResponse={null}
        validateLoading={false}
        validateError={null}
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
        renderLoading={false}
        renderError={null}
        transpileResponse={null}
        transpileLoading={false}
        transpileError={null}
        validateResponse={null}
        validateLoading={false}
        validateError={null}
      />
    );

    await userEvent.click(screen.getByRole("button", { name: "Copy JSON" }));

    expect(writeText).toHaveBeenNthCalledWith(2, PARSE_RESPONSE.sqmJson);
    expect(screen.getByRole("button", { name: "Copied JSON" })).toBeInTheDocument();
  });
});
