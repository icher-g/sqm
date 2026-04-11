import { useEffect, useState } from "react";
import { fetchExamples, parseSql, renderSql, validateSql } from "./api/playgroundApi";
import { ResultsPanel, type ResultTab } from "./components/ResultsPanel";
import { SqlEditorPanel } from "./components/SqlEditorPanel";
import type { ExampleDto, ParseResponseDto, RenderResponseDto, SqlDialect, ValidateResponseDto } from "./types/api";

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
  const [activeAction, setActiveAction] = useState<"parse" | "render" | "validate" | null>(null);
  const [parseLoading, setParseLoading] = useState(false);
  const [parseError, setParseError] = useState<string | null>(null);
  const [parseResponse, setParseResponse] = useState<ParseResponseDto | null>(null);
  const [renderLoading, setRenderLoading] = useState(false);
  const [renderError, setRenderError] = useState<string | null>(null);
  const [renderResponse, setRenderResponse] = useState<RenderResponseDto | null>(null);
  const [validateLoading, setValidateLoading] = useState(false);
  const [validateError, setValidateError] = useState<string | null>(null);
  const [validateResponse, setValidateResponse] = useState<ValidateResponseDto | null>(null);
  const [activeResultTab, setActiveResultTab] = useState<ResultTab>("ast");

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
    setActiveAction("parse");
    setParseLoading(true);
    setParseError(null);
    setParseResponse(null);
    setValidateError(null);
    setValidateResponse(null);
    setRenderError(null);
    setRenderResponse(null);
    setActiveResultTab("ast");

    try {
      const response = await parseSql({
        sql: sqlText,
        dialect: sourceDialect
      });
      setParseResponse(response);
      setActiveResultTab(response.success ? "ast" : "diagnostics");
    } catch (error) {
      setParseResponse(null);
      setParseError(error instanceof Error ? error.message : "Failed to parse SQL");
      setActiveResultTab("diagnostics");
    } finally {
      setParseLoading(false);
    }
  }

  async function handleRender() {
    setActiveAction("render");
    setRenderLoading(true);
    setRenderError(null);
    setRenderResponse(null);
    setParseError(null);
    setParseResponse(null);
    setValidateError(null);
    setValidateResponse(null);
    setActiveResultTab("renderedSql");

    try {
      const response = await renderSql({
        sql: sqlText,
        sourceDialect,
        targetDialect
      });
      setRenderResponse(response);
      setActiveResultTab(response.success ? "renderedSql" : "diagnostics");
    } catch (error) {
      setRenderResponse(null);
      setRenderError(error instanceof Error ? error.message : "Failed to render SQL");
      setActiveResultTab("diagnostics");
    } finally {
      setRenderLoading(false);
    }
  }

  async function handleValidate() {
    setActiveAction("validate");
    setValidateLoading(true);
    setValidateError(null);
    setValidateResponse(null);
    setParseError(null);
    setParseResponse(null);
    setRenderError(null);
    setRenderResponse(null);
    setActiveResultTab("diagnostics");

    try {
      const response = await validateSql({
        sql: sqlText,
        dialect: sourceDialect
      });
      setValidateResponse(response);
      setActiveResultTab("diagnostics");
    } catch (error) {
      setValidateResponse(null);
      setValidateError(error instanceof Error ? error.message : "Failed to validate SQL");
      setActiveResultTab("diagnostics");
    } finally {
      setValidateLoading(false);
    }
  }

  return (
    <main className="app-shell">
      <header className="hero">
        <p className="eyebrow">SQM Playground</p>
        <h1>Frontend shell</h1>
        <p className="hero-copy">This slice loads examples and lets you parse SQL into real AST and JSON results.</p>
      </header>

      <section className="workspace-grid">
        <div className="workspace-column">
          <SqlEditorPanel
            sqlText={sqlText}
            examples={examples}
            selectedExampleId={selectedExampleId}
            examplesLoading={examplesLoading}
            examplesError={examplesError}
            sourceDialect={sourceDialect}
            targetDialect={targetDialect}
            activeAction={activeAction}
            parseLoading={parseLoading}
            renderLoading={renderLoading}
            validateLoading={validateLoading}
            canParse={sqlText.trim().length > 0}
            canRender={sqlText.trim().length > 0}
            canValidate={sqlText.trim().length > 0}
            onSqlTextChange={setSqlText}
            onExampleChange={handleExampleChange}
            onSourceDialectChange={setSourceDialect}
            onTargetDialectChange={setTargetDialect}
            onParse={handleParse}
            onRender={handleRender}
            onValidate={handleValidate}
          />
        </div>

        <div className="workspace-column">
          <ResultsPanel
            activeResultTab={activeResultTab}
            onResultTabChange={setActiveResultTab}
            parseResponse={parseResponse}
            parseLoading={parseLoading}
            parseError={parseError}
            renderResponse={renderResponse}
            renderLoading={renderLoading}
            renderError={renderError}
            validateResponse={validateResponse}
            validateLoading={validateLoading}
            validateError={validateError}
          />
        </div>
      </section>
    </main>
  );
}
