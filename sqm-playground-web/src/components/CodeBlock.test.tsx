import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { CodeBlock } from "./CodeBlock";

describe("CodeBlock", () => {
  it("renders highlighted code with the requested language class", () => {
    render(<CodeBlock code={`class Example {\n  int value = 1;\n}`} language="java" />);

    const codeElement = screen.getByText("class").closest("code");
    const preElement = screen.getByText("class").closest("pre");

    expect(codeElement).toHaveClass("language-java");
    expect(preElement).toHaveClass("language-java");
    expect(screen.getByText("class")).toBeInTheDocument();
    expect(screen.getByText("Example")).toBeInTheDocument();
  });
});
