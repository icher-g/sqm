import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import App from "./App";

describe("App", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("renders the frontend shell placeholders", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(
        JSON.stringify({
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
        }),
        {
          status: 200,
          headers: {
            "Content-Type": "application/json"
          }
        }
      )
    );

    render(<App />);

    expect(screen.getByText("Frontend shell")).toBeInTheDocument();
    expect(screen.getByText("Controls")).toBeInTheDocument();
    expect(screen.getByText("Editor")).toBeInTheDocument();
    expect(screen.getByText("Results")).toBeInTheDocument();
    expect(screen.getByLabelText("Source dialect")).toHaveValue("ansi");
    expect(screen.getByLabelText("Target dialect")).toHaveValue("postgresql");
    expect(screen.getByRole("button", { name: "Parse" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Render" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Validate" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Transpile" })).toBeDisabled();
    await waitFor(() => {
      expect(screen.getByLabelText("Example")).toHaveValue("basic-select");
    });
    expect(screen.getByLabelText("SQL text")).toHaveValue("select id, name\nfrom customer\nwhere id = 1\norder by name");
    expect(screen.getByRole("tab", { name: "AST" })).toHaveAttribute("aria-selected", "true");
    expect(screen.getByRole("tabpanel", { name: "AST" })).toBeInTheDocument();

    await userEvent.selectOptions(screen.getByLabelText("Example"), "pg-distinct");
    await userEvent.click(screen.getByRole("tab", { name: "Diagnostics" }));

    expect(screen.getByLabelText("SQL text")).toHaveValue("select distinct on (id) id\nfrom customer\norder by id");
    expect(screen.getByRole("tab", { name: "Diagnostics" })).toHaveAttribute("aria-selected", "true");
    expect(screen.getByRole("tabpanel", { name: "Diagnostics" })).toBeInTheDocument();
    expect(screen.queryByRole("tabpanel", { name: "AST" })).not.toBeInTheDocument();
  });
});
