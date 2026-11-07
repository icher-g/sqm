import { afterEach, expect, it, vi } from "vitest";

const { configure, worker } = vi.hoisted(() => ({
  configure: vi.fn(),
  worker: { terminate: vi.fn() }
}));

vi.mock("@monaco-editor/react", () => ({ loader: { config: configure } }));
vi.mock("monaco-editor", () => ({ editor: {} }));
vi.mock("monaco-editor/editor/editor.worker?worker", () => ({
  default: class { constructor() { return worker; } }
}));

afterEach(() => vi.unstubAllGlobals());

it("configures the local editor and its worker without loading the CDN distribution", async () => {
  vi.stubGlobal("MonacoEnvironment", undefined);
  await import("./monaco");

  expect(configure).toHaveBeenCalledWith({ monaco: expect.objectContaining({ editor: {} }) });
  const environment = (globalThis as typeof globalThis & {
    MonacoEnvironment: { getWorker: () => unknown }
  }).MonacoEnvironment;
  expect(environment.getWorker()).toBe(worker);
});
