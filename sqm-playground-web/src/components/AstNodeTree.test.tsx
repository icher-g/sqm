import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it } from "vitest";
import { AstNodeTree, type AstTreeCommand } from "./AstNodeTree";
import type { AstNodeDto } from "../types/api";

describe("AstNodeTree", () => {
  afterEach(() => {
    cleanup();
  });

  it("renders simple leaf child slots as inline values", () => {
    const node: AstNodeDto = {
      nodeType: "Assignment",
      nodeInterface: "io.sqm.core.Assignment",
      kind: null,
      category: "node",
      label: "Assignment",
      details: [],
      children: [
        {
          slot: "column",
          multiple: false,
          nodes: [
            {
              nodeType: "QualifiedName",
              nodeInterface: "io.sqm.core.QualifiedName",
              kind: null,
              category: "value",
              label: "QualifiedName",
              details: [
                {
                  name: "column",
                  value: "o.status"
                }
              ],
              children: []
            }
          ]
        },
        {
          slot: "value",
          multiple: false,
          nodes: [
            {
              nodeType: "LiteralExpr",
              nodeInterface: "io.sqm.core.LiteralExpr",
              kind: null,
              category: "expression",
              label: "LiteralExpr",
              details: [
                {
                  name: "value",
                  value: "priority"
                }
              ],
              children: []
            }
          ]
        }
      ]
    };

    render(<AstNodeTree node={node} />);

    expect(screen.getByText("column")).toBeInTheDocument();
    expect(screen.getByText("QualifiedName")).toBeInTheDocument();
    expect(screen.getByText("o.status")).toBeInTheDocument();
    expect(screen.getByText("value")).toBeInTheDocument();
    expect(screen.getByText("LiteralExpr")).toBeInTheDocument();
    expect(screen.getByText("'priority'")).toBeInTheDocument();
  });

  it("lets users collapse and re-expand nested AST nodes", async () => {
    const node: AstNodeDto = {
      nodeType: "SelectQuery",
      nodeInterface: "io.sqm.core.Query",
      kind: "select",
      category: "statement",
      label: "SelectQuery",
      details: [],
      children: [
        {
          slot: "items",
          multiple: true,
          nodes: [
            {
              nodeType: "ExprSelectItem",
              nodeInterface: "io.sqm.core.ExprSelectItem",
              kind: null,
              category: "expression",
              label: "ExprSelectItem",
              details: [],
              children: [
                {
                  slot: "expr",
                  multiple: false,
                  nodes: [
                    {
                      nodeType: "ColumnExpr",
                      nodeInterface: "io.sqm.core.ColumnExpr",
                      kind: null,
                      category: "expression",
                      label: "ColumnExpr",
                      details: [
                        {
                          name: "name",
                          value: "id"
                        }
                      ],
                      children: []
                    }
                  ]
                }
              ]
            }
          ]
        }
      ]
    };

    render(<AstNodeTree node={node} />);

    expect(screen.getByText("items[]")).toBeInTheDocument();

    const rootToggle = screen.getAllByRole("button", { expanded: true })[0];
    await userEvent.click(rootToggle);

    expect(screen.queryByText("items[]")).not.toBeInTheDocument();
    expect(screen.getAllByRole("button", { expanded: false })[0]).toBeInTheDocument();

    await userEvent.click(screen.getAllByRole("button", { expanded: false })[0]);

    expect(screen.getByText("items[]")).toBeInTheDocument();
  });

  it("hides detail rows when a node is collapsed", async () => {
    const node: AstNodeDto = {
      nodeType: "ComparisonPredicate",
      nodeInterface: "io.sqm.core.ComparisonPredicate",
      kind: null,
      category: "predicate",
      label: "ComparisonPredicate",
      details: [
        {
          name: "operator",
          value: "EQ"
        }
      ],
      children: [
        {
          slot: "lhs",
          multiple: false,
          nodes: [
            {
              nodeType: "ColumnExpr",
              nodeInterface: "io.sqm.core.ColumnExpr",
              kind: null,
              category: "expression",
              label: "ColumnExpr",
              details: [
                {
                  name: "name",
                  value: "c.id"
                }
              ],
              children: []
            }
          ]
        }
      ]
    };

    render(<AstNodeTree node={node} />);

    expect(screen.getByText("operator")).toBeInTheDocument();
    expect(screen.getByText("EQ")).toBeInTheDocument();

    await userEvent.click(screen.getAllByRole("button", { expanded: true })[0]);

    expect(screen.queryByText("operator")).not.toBeInTheDocument();
    expect(screen.queryByText("EQ")).not.toBeInTheDocument();
    expect(screen.queryByText("lhs")).not.toBeInTheDocument();
    expect(screen.getByText("{ operator: EQ, lhs }")).toBeInTheDocument();
  });

  it("lets users collapse and re-expand collection slots", async () => {
    const node: AstNodeDto = {
      nodeType: "UpdateStatement",
      nodeInterface: "io.sqm.core.UpdateStatement",
      kind: null,
      category: "statement",
      label: "UpdateStatement",
      details: [],
      children: [
        {
          slot: "joins",
          multiple: true,
          nodes: [
            {
              nodeType: "OnJoin",
              nodeInterface: "io.sqm.core.OnJoin",
              kind: null,
              category: "join",
              label: "OnJoin",
              details: [],
              children: []
            }
          ]
        }
      ]
    };

    render(<AstNodeTree node={node} />);

    expect(screen.getByText("joins[]")).toBeInTheDocument();
    expect(screen.getByText("OnJoin")).toBeInTheDocument();

    await userEvent.click(screen.getAllByRole("button", { expanded: true })[1]);

    expect(screen.getByText("joins[]")).toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "OnJoin" })).not.toBeInTheDocument();
    expect(screen.getByText("OnJoin")).toBeInTheDocument();

    await userEvent.click(screen.getAllByRole("button", { expanded: false })[0]);

    expect(screen.getByText("OnJoin")).toBeInTheDocument();
  });

  it("responds to global expand and collapse commands", () => {
    const node: AstNodeDto = {
      nodeType: "SelectQuery",
      nodeInterface: "io.sqm.core.Query",
      kind: "select",
      category: "statement",
      label: "SelectQuery",
      details: [],
      children: [
        {
          slot: "items",
          multiple: true,
          nodes: [
            {
              nodeType: "ExprSelectItem",
              nodeInterface: "io.sqm.core.ExprSelectItem",
              kind: null,
              category: "expression",
              label: "ExprSelectItem",
              details: [],
              children: []
            }
          ]
        }
      ]
    };

    const collapseCommand: AstTreeCommand = { type: "collapse", version: 1 };
    const expandCommand: AstTreeCommand = { type: "expand", version: 2 };

    const { rerender } = render(<AstNodeTree node={node} command={null} />);

    rerender(<AstNodeTree node={node} command={collapseCommand} />);

    expect(screen.queryByText("items[]")).not.toBeInTheDocument();

    rerender(<AstNodeTree node={node} command={expandCommand} />);

    expect(screen.getByText("items[]")).toBeInTheDocument();
  });
});
