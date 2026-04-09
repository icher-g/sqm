import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import App from "./App";

describe("App", () => {
  it("renders the frontend shell placeholders", () => {
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
    expect(screen.getByLabelText("SQL text")).toHaveValue(
      "select id, name\nfrom customer\nwhere id = 1\norder by name"
    );
  });
});
