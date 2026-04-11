import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { AstNodeTree } from "./AstNodeTree";
import type { AstNodeDto } from "../types/api";

describe("AstNodeTree", () => {
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
});
