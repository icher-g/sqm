import { useEffect, useState } from "react";
import { fetchExamples, parseSql } from "./api/playgroundApi";
import { ControlBar } from "./components/ControlBar";
import { ResultsPanel } from "./components/ResultsPanel";
import { SqlEditorPanel } from "./components/SqlEditorPanel";
import type { ExampleDto, ParseResponseDto, SqlDialect } from "./types/api";

/**
 * Root application component for the frontend shell.
 */
export default function App() {
  const [sqlText, setSqlText] = useState("Loading example...");
  const [examples, setExamples] = useState<ExampleDto[]>([]);
  const [selectedExampleId, setSelectedExampleId] = useState("");
  const [sourceDialect, setSourceDialect] = useState<SqlDialect>("ansi");
  const [targetDialect, setTargetDialect] = useState<SqlDialect>("postgresql");
  const [examplesLoading, setExamplesLoading] = useState(true);
  const [examplesError, setExamplesError] = useState<string | null>(null);
  const [parseLoading, setParseLoading] = useState(false);
  const [parseError, setParseError] = useState<string | null>(null);
  const [parseResponse, setParseResponse] = useState<ParseResponseDto | null>(null);

  useEffect(() => {
    void loadExamples();
  }, []);

  async function loadExamples() {
    setExamplesLoading(true);
    setExamplesError(null);

    try {
      const payload = await fetchExamples();
      setExamples(payload.examples);

      const defaultExample = payload.examples.find((example) => example.dialect === "ansi") ?? payload.examples[0];
      if (defaultExample) {
        setSelectedExampleId(defaultExample.id);
        setSqlText(defaultExample.sql);
        setSourceDialect(defaultExample.dialect);
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
      setSourceDialect(example.dialect);
    }
  }

  async function handleParse() {
    setParseLoading(true);
    setParseError(null);

    try {
      const response = await parseSql({
        sql: sqlText,
        dialect: sourceDialect
      });
      setParseResponse(response);
    } catch (error) {
      setParseResponse(null);
      setParseError(error instanceof Error ? error.message : "Failed to parse SQL");
    } finally {
      setParseLoading(false);
    }
  }

  return (
    <main className="app-shell">
      <header className="hero">
        <p className="eyebrow">SQM Playground</p>
        <h1>Frontend shell</h1>
        <p className="hero-copy">This slice loads examples and lets you parse SQL into real AST and JSON results.</p>
      </header>

      <ControlBar
        examples={examples}
        selectedExampleId={selectedExampleId}
        examplesLoading={examplesLoading}
        examplesError={examplesError}
        sourceDialect={sourceDialect}
        targetDialect={targetDialect}
        parseLoading={parseLoading}
        canParse={sqlText.trim().length > 0}
        onExampleChange={handleExampleChange}
        onSourceDialectChange={setSourceDialect}
        onTargetDialectChange={setTargetDialect}
        onParse={handleParse}
      />

      <section className="workspace-grid">
        <div className="workspace-column">
          <SqlEditorPanel sqlText={sqlText} onSqlTextChange={setSqlText} />
        </div>

        <div className="workspace-column">
          <ResultsPanel parseResponse={parseResponse} parseLoading={parseLoading} parseError={parseError} />
        </div>
      </section>
    </main>
  );
}
