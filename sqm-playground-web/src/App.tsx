import { useEffect, useState } from "react";
import { fetchExamples, parseSql, renderSql, transpileSql, validateSql } from "./api/playgroundApi";
import { ResultsPanel, type ResultTab } from "./components/ResultsPanel";
import { SqlEditorPanel } from "./components/SqlEditorPanel";
import type {
  ExampleDto,
  ParseResponseDto,
  PlaygroundDiagnosticDto,
  RenderParameterizationMode,
  RenderResponseDto,
  SqlDialect,
  TranspileResponseDto,
  ValidateResponseDto
} from "./types/api";

/**
 * Root application component for the frontend shell.
 */
export default function App() {
  const [sqlText, setSqlText] = useState("Loading example...");
  const [examples, setExamples] = useState<ExampleDto[]>([]);
  const [selectedExampleId, setSelectedExampleId] = useState("");
  const [sourceDialect, setSourceDialect] = useState<SqlDialect>("ansi");
  const [targetDialect, setTargetDialect] = useState<SqlDialect>("postgresql");
  const [renderParameterizationMode, setRenderParameterizationMode] = useState<RenderParameterizationMode>("inline");
  const [examplesLoading, setExamplesLoading] = useState(true);
  const [examplesError, setExamplesError] = useState<string | null>(null);
  const [activeAction, setActiveAction] = useState<"parse" | "format" | "render" | "validate" | "transpile" | null>(null);
  const [parseLoading, setParseLoading] = useState(false);
  const [parseError, setParseError] = useState<string | null>(null);
  const [parseResponse, setParseResponse] = useState<ParseResponseDto | null>(null);
  const [formatLoading, setFormatLoading] = useState(false);
  const [renderLoading, setRenderLoading] = useState(false);
  const [renderError, setRenderError] = useState<string | null>(null);
  const [renderResponse, setRenderResponse] = useState<RenderResponseDto | null>(null);
  const [renderedSqlDialect, setRenderedSqlDialect] = useState<SqlDialect | null>(null);
  const [renderedSqlTimestamp, setRenderedSqlTimestamp] = useState<number | null>(null);
  const [transpileLoading, setTranspileLoading] = useState(false);
  const [transpileError, setTranspileError] = useState<string | null>(null);
  const [transpileResponse, setTranspileResponse] = useState<TranspileResponseDto | null>(null);
  const [validateLoading, setValidateLoading] = useState(false);
  const [validateError, setValidateError] = useState<string | null>(null);
  const [validateResponse, setValidateResponse] = useState<ValidateResponseDto | null>(null);
  const [activeResultTab, setActiveResultTab] = useState<ResultTab>("ast");
  const [focusedDiagnostic, setFocusedDiagnostic] = useState<{ diagnostic: PlaygroundDiagnosticDto; version: number } | null>(null);
  const [initialShareState] = useState(readShareState);

  useEffect(() => {
    void loadExamples();
  }, []);

  useEffect(() => {
    const params = new URLSearchParams();
    params.set("sql", sqlText);
    params.set("source", sourceDialect);
    params.set("target", targetDialect);
    if (selectedExampleId) {
      params.set("example", selectedExampleId);
    }
    if (activeResultTab !== "ast") {
      params.set("tab", activeResultTab);
    }

    const nextQuery = params.toString();
    const nextUrl = nextQuery ? `${window.location.pathname}?${nextQuery}` : window.location.pathname;
    window.history.replaceState({}, "", nextUrl);
  }, [activeResultTab, selectedExampleId, sourceDialect, sqlText, targetDialect]);

  async function loadExamples() {
    setExamplesLoading(true);
    setExamplesError(null);

    try {
      const payload = await fetchExamples();
      setExamples(payload.examples);

      const sharedExample = initialShareState.exampleId
        ? payload.examples.find((example) => example.id === initialShareState.exampleId)
        : null;
      const defaultExample = payload.examples.find((example) => example.dialect === "ansi") ?? payload.examples[0];
      const initialExample = sharedExample ?? defaultExample;

      if (initialExample) {
        setSelectedExampleId(initialExample.id);
        setSqlText(initialShareState.sql ?? initialExample.sql);
        setSourceDialect(initialShareState.sourceDialect ?? initialExample.dialect);
      } else {
        setSelectedExampleId("");
        setSqlText(initialShareState.sql ?? "");
        if (initialShareState.sourceDialect) {
          setSourceDialect(initialShareState.sourceDialect);
        }
      }

      if (initialShareState.targetDialect) {
        setTargetDialect(initialShareState.targetDialect);
      }
      if (initialShareState.activeResultTab) {
        setActiveResultTab(initialShareState.activeResultTab);
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
    setTranspileError(null);
    setTranspileResponse(null);
    setRenderError(null);
    setRenderResponse(null);
    setRenderedSqlDialect(null);
    setRenderedSqlTimestamp(null);
    setFocusedDiagnostic(null);
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
    setTranspileError(null);
    setTranspileResponse(null);
    setRenderedSqlDialect(null);
    setRenderedSqlTimestamp(null);
    setFocusedDiagnostic(null);
    setActiveResultTab("renderedSql");

    try {
      const response = await renderSql({
        sql: sqlText,
        sourceDialect: targetDialect,
        targetDialect,
        parameterizationMode: renderParameterizationMode
      });
      setRenderResponse(response);
      setRenderedSqlDialect(response.success && response.renderedSql ? targetDialect : null);
      setRenderedSqlTimestamp(response.success && response.renderedSql ? Date.now() : null);
      setActiveResultTab(response.success ? "renderedSql" : "diagnostics");
    } catch (error) {
      setRenderResponse(null);
      setRenderedSqlDialect(null);
      setRenderedSqlTimestamp(null);
      setRenderError(error instanceof Error ? error.message : "Failed to render SQL");
      setActiveResultTab("diagnostics");
    } finally {
      setRenderLoading(false);
    }
  }

  async function handleFormat() {
    setActiveAction("format");
    setFormatLoading(true);
    setRenderError(null);
    setFocusedDiagnostic(null);

    try {
      const response = await renderSql({
        sql: sqlText,
        sourceDialect,
        targetDialect: sourceDialect,
        parameterizationMode: "inline"
      });

      if (response.success && response.renderedSql) {
        setSqlText(response.renderedSql);
        setRenderedSqlDialect(null);
        setRenderedSqlTimestamp(null);
      } else {
        setRenderResponse(response);
        setRenderedSqlDialect(null);
        setRenderedSqlTimestamp(null);
        setActiveResultTab("diagnostics");
      }
    } catch (error) {
      setRenderResponse(null);
      setRenderedSqlDialect(null);
      setRenderedSqlTimestamp(null);
      setRenderError(error instanceof Error ? error.message : "Failed to format SQL");
      setActiveResultTab("diagnostics");
    } finally {
      setFormatLoading(false);
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
    setRenderedSqlDialect(null);
    setRenderedSqlTimestamp(null);
    setTranspileError(null);
    setTranspileResponse(null);
    setFocusedDiagnostic(null);
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

  async function handleTranspile() {
    setActiveAction("transpile");
    setTranspileLoading(true);
    setTranspileError(null);
    setTranspileResponse(null);
    setParseError(null);
    setParseResponse(null);
    setRenderError(null);
    setRenderResponse(null);
    setValidateError(null);
    setValidateResponse(null);
    setRenderedSqlDialect(null);
    setRenderedSqlTimestamp(null);
    setFocusedDiagnostic(null);
    setActiveResultTab("renderedSql");

    try {
      const response = await transpileSql({
        sql: sqlText,
        sourceDialect,
        targetDialect,
        parameterizationMode: renderParameterizationMode
      });
      setTranspileResponse(response);
      setRenderedSqlDialect(response.success && response.renderedSql ? targetDialect : null);
      setRenderedSqlTimestamp(response.success && response.renderedSql ? Date.now() : null);
      setActiveResultTab(response.success ? "renderedSql" : "diagnostics");
    } catch (error) {
      setTranspileResponse(null);
      setRenderedSqlDialect(null);
      setRenderedSqlTimestamp(null);
      setTranspileError(error instanceof Error ? error.message : "Failed to transpile SQL");
      setActiveResultTab("diagnostics");
    } finally {
      setTranspileLoading(false);
    }
  }

  function handleDiagnosticSelect(diagnostic: PlaygroundDiagnosticDto) {
    setFocusedDiagnostic((current) => ({
      diagnostic,
      version: (current?.version ?? 0) + 1
    }));
  }

  const editorDiagnostics =
    transpileResponse?.diagnostics.length
      ? transpileResponse.diagnostics
      : validateResponse?.diagnostics.length
        ? validateResponse.diagnostics
        : renderResponse?.diagnostics.length
          ? renderResponse.diagnostics
          : parseResponse?.diagnostics.length
            ? parseResponse.diagnostics
            : [];

  return (
    <main className="app-shell">
      <header className="hero">
        <p className="eyebrow">SQM Playground</p>
        <h1>Explore SQL Through SQM</h1>
        <p className="hero-copy">
          Load example queries, inspect the SQM AST and JSON, generate DSL, and compare rendered or transpiled SQL
          across dialects.
        </p>
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
            renderParameterizationMode={renderParameterizationMode}
            editorDiagnostics={editorDiagnostics}
            focusedDiagnostic={focusedDiagnostic}
            activeAction={activeAction}
            parseLoading={parseLoading}
            formatLoading={formatLoading}
            renderLoading={renderLoading}
            transpileLoading={transpileLoading}
            validateLoading={validateLoading}
            canParse={sqlText.trim().length > 0}
            canFormat={sqlText.trim().length > 0}
            canRender={sqlText.trim().length > 0}
            canTranspile={sqlText.trim().length > 0}
            canValidate={sqlText.trim().length > 0}
            onSqlTextChange={setSqlText}
            onExampleChange={handleExampleChange}
            onSourceDialectChange={setSourceDialect}
            onTargetDialectChange={setTargetDialect}
            onRenderParameterizationModeChange={setRenderParameterizationMode}
            onParse={handleParse}
            onFormat={handleFormat}
            onRender={handleRender}
            onTranspile={handleTranspile}
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
            renderedSqlDialect={renderedSqlDialect}
            renderedSqlTimestamp={renderedSqlTimestamp}
            renderLoading={renderLoading}
            renderError={renderError}
            transpileResponse={transpileResponse}
            transpileLoading={transpileLoading}
            transpileError={transpileError}
            validateResponse={validateResponse}
            validateLoading={validateLoading}
            validateError={validateError}
            onDiagnosticSelect={handleDiagnosticSelect}
          />
        </div>
      </section>
    </main>
  );
}

function readShareState(): {
  sql: string | null;
  sourceDialect: SqlDialect | null;
  targetDialect: SqlDialect | null;
  exampleId: string | null;
  activeResultTab: ResultTab | null;
} {
  const params = new URLSearchParams(window.location.search);
  return {
    sql: params.get("sql"),
    sourceDialect: readDialect(params.get("source")),
    targetDialect: readDialect(params.get("target")),
    exampleId: params.get("example"),
    activeResultTab: readResultTab(params.get("tab"))
  };
}

function readDialect(value: string | null): SqlDialect | null {
  if (value === "ansi" || value === "postgresql" || value === "mysql" || value === "sqlserver") {
    return value;
  }

  return null;
}

function readResultTab(value: string | null): ResultTab | null {
  if (
    value === "ast"
    || value === "dsl"
    || value === "json"
    || value === "renderedSql"
    || value === "diagnostics"
    || value === "about"
  ) {
    return value;
  }

  return null;
}
