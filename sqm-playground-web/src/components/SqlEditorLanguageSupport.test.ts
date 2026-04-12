import { describe, expect, it } from "vitest";
import { getSqlCompletionSpecs } from "./SqlEditorLanguageSupport";

describe("SqlEditorLanguageSupport", () => {
  it("includes shared SQL snippets and keywords", () => {
    const specs = getSqlCompletionSpecs("ansi");

    expect(specs.some((spec) => spec.label === "SELECT" && spec.isSnippet)).toBe(true);
    expect(specs.some((spec) => spec.label === "ORDER BY")).toBe(true);
    expect(specs.some((spec) => spec.label === "customer")).toBe(true);
    expect(specs.some((spec) => spec.label === "count(...)")).toBe(true);
  });

  it("includes PostgreSQL-specific completions", () => {
    const specs = getSqlCompletionSpecs("postgresql");

    expect(specs.some((spec) => spec.label === "DISTINCT ON")).toBe(true);
    expect(specs.some((spec) => spec.label === "RETURNING")).toBe(true);
  });

  it("includes MySQL-specific completions", () => {
    const specs = getSqlCompletionSpecs("mysql");

    expect(specs.some((spec) => spec.label === "STRAIGHT_JOIN")).toBe(true);
    expect(specs.some((spec) => spec.label === "ON DUPLICATE KEY UPDATE")).toBe(true);
  });

  it("includes SQL Server-specific completions", () => {
    const specs = getSqlCompletionSpecs("sqlserver");

    expect(specs.some((spec) => spec.label === "TOP")).toBe(true);
    expect(specs.some((spec) => spec.label === "OUTPUT")).toBe(true);
  });

  it("prefers table names after FROM-like clauses", () => {
    const specs = getSqlCompletionSpecs("ansi", "select * from cu", "select * from cu");

    expect(specs.some((spec) => spec.label === "customer")).toBe(true);
    expect(specs.some((spec) => spec.label === "orders")).toBe(true);
    expect(specs.some((spec) => spec.label === "SELECT")).toBe(false);
  });

  it("suggests alias columns after dotted access", () => {
    const specs = getSqlCompletionSpecs(
      "mysql",
      "update orders o join customer c on c.id = o.customer_id set o.",
      "update orders o join customer c on c.id = o.customer_id set o."
    );

    expect(specs.map((spec) => spec.label)).toEqual(["id", "customer_id", "status"]);
  });

  it("suggests aliases and expression helpers in expression contexts", () => {
    const specs = getSqlCompletionSpecs(
      "postgresql",
      "select * from orders o join customer c on c.id = o.customer_id where ",
      "select * from orders o join customer c on c.id = o.customer_id where "
    );

    expect(specs.some((spec) => spec.label === "o")).toBe(true);
    expect(specs.some((spec) => spec.label === "c.name")).toBe(true);
    expect(specs.some((spec) => spec.label === "coalesce(...)")).toBe(true);
  });
});
