import { loader } from "@monaco-editor/react";
import * as monaco from "monaco-editor";
import EditorWorker from "monaco-editor/editor/editor.worker?worker";

// Use the local editor bundle so Vite also replaces Monaco's vendored sanitizer.
const editorGlobal = globalThis as typeof globalThis & { MonacoEnvironment: monaco.Environment };
editorGlobal.MonacoEnvironment = {
  getWorker: () => new EditorWorker()
};
loader.config({ monaco });
