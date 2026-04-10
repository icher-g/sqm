import { useEffect, useState } from "react";
import { ControlBar } from "./components/ControlBar";
import { ResultsPanel } from "./components/ResultsPanel";
import { SqlEditorPanel } from "./components/SqlEditorPanel";
import type { ExampleDto, ExamplesResponseDto } from "./types/api";

const PLAYGROUND_API_BASE_URL = import.meta.env.VITE_PLAYGROUND_API_BASE_URL ?? "http://localhost:8080/sqm/playground/api/v1";

/**
 * Root application component for the frontend shell.
 */
export default function App() {
  const [sqlText, setSqlText] = useState("Loading example...");
  const [examples, setExamples] = useState<ExampleDto[]>([]);
  const [selectedExampleId, setSelectedExampleId] = useState("");
  const [examplesLoading, setExamplesLoading] = useState(true);
  const [examplesError, setExamplesError] = useState<string | null>(null);

  useEffect(() => {
    void loadExamples();
  }, []);

  async function loadExamples() {
    setExamplesLoading(true);
    setExamplesError(null);

    try {
      const response = await fetch(`${PLAYGROUND_API_BASE_URL}/examples`, {
        headers: {
          Accept: "application/json"
        }
      });

      if (!response.ok) {
        throw new Error(`Example request failed with status ${response.status}`);
      }

      const payload = (await response.json()) as ExamplesResponseDto;
      setExamples(payload.examples);

      const defaultExample = payload.examples.find((example) => example.dialect === "ansi") ?? payload.examples[0];
      if (defaultExample) {
        setSelectedExampleId(defaultExample.id);
        setSqlText(defaultExample.sql);
      } else {
        setSqlText("");
      }
    } catch (error) {
      setExamplesError(error instanceof Error ? error.message : "Failed to load examples");
      setSqlText("");
    } finally {
      setExamplesLoading(false);
    }
  }

  function handleExampleChange(nextExampleId: string) {
    setSelectedExampleId(nextExampleId);
    const example = examples.find((item) => item.id === nextExampleId);
    if (example) {
      setSqlText(example.sql);
    }
  }

  return (
    <main className="app-shell">
      <header className="hero">
        <p className="eyebrow">SQM Playground</p>
        <h1>Frontend shell</h1>
        <p className="hero-copy">
          This is the first learning slice. It only proves that the React app starts and renders a placeholder
          workstation.
        </p>
      </header>

      <section className="shell-grid">
        <ControlBar
          examples={examples}
          selectedExampleId={selectedExampleId}
          examplesLoading={examplesLoading}
          examplesError={examplesError}
          onExampleChange={handleExampleChange}
        />

        <SqlEditorPanel sqlText={sqlText} onSqlTextChange={setSqlText} />

        <ResultsPanel />
      </section>
    </main>
  );
}
