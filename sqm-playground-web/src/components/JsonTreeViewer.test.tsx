import { cleanup, render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it } from "vitest";
import { JsonTreeViewer } from "./JsonTreeViewer";

describe("JsonTreeViewer", () => {
  afterEach(() => {
    cleanup();
  });

  it("renders JSON keys and primitive values as a tree", () => {
    render(<JsonTreeViewer json={'{"kind":"select","details":{"limit":10}}'} />);

    expect(screen.getByText("kind")).toBeInTheDocument();
    expect(screen.getByText('"select"')).toBeInTheDocument();
    expect(screen.getByText("details")).toBeInTheDocument();
    expect(screen.getByText("limit")).toBeInTheDocument();
    expect(screen.getByText("10")).toBeInTheDocument();
  });

  it("collapses and expands nested nodes locally", async () => {
    render(<JsonTreeViewer json={'{"details":{"limit":10,"offset":0}}'} />);

    await userEvent.click(getToggleForKey("details"));

    expect(screen.queryByText("limit")).not.toBeInTheDocument();
    expect(screen.queryByText("offset")).not.toBeInTheDocument();
    expect(screen.getByText("2 properties")).toBeInTheDocument();

    await userEvent.click(getToggleForKey("details"));

    expect(screen.getByText("limit")).toBeInTheDocument();
    expect(screen.getByText("offset")).toBeInTheDocument();
  });

  it("responds to global expand and collapse commands", () => {
    const { rerender } = render(
      <JsonTreeViewer
        json={'{"details":{"limit":10,"offset":0}}'}
        command={null}
      />
    );

    rerender(
      <JsonTreeViewer
        json={'{"details":{"limit":10,"offset":0}}'}
        command={{ type: "collapse", version: 1 }}
      />
    );

    expect(screen.queryByText("limit")).not.toBeInTheDocument();
    expect(screen.getByText("1 property")).toBeInTheDocument();

    rerender(
      <JsonTreeViewer
        json={'{"details":{"limit":10,"offset":0}}'}
        command={{ type: "expand", version: 2 }}
      />
    );

    expect(screen.getByText("limit")).toBeInTheDocument();
    expect(screen.getByText("offset")).toBeInTheDocument();
  });
});

function getToggleForKey(key: string) {
  const keyElements = screen.getAllByText(key);
  const keyElement = keyElements[0];
  const header = keyElement.closest(".json-node-header");
  if (!header) {
    throw new Error(`Could not find JSON header for key ${key}`);
  }

  const toggle = within(header as HTMLElement).queryByRole("button");
  if (!(toggle instanceof HTMLButtonElement)) {
    throw new Error(`Could not find JSON toggle for key ${key}`);
  }

  return toggle;
}
