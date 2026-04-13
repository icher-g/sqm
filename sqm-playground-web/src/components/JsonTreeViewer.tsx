import { useEffect, useMemo, useRef, useState } from "react";

export interface JsonTreeCommand {
  type: "expand" | "collapse";
  version: number;
}

interface JsonTreeViewerProps {
  json: string;
  command?: JsonTreeCommand | null;
}

type JsonValue = null | boolean | number | string | JsonValue[] | { [key: string]: JsonValue };

/**
 * Renders parsed JSON as a collapsible tree view.
 */
export function JsonTreeViewer(props: JsonTreeViewerProps) {
  const parsedValue = useMemo(() => JSON.parse(props.json) as JsonValue, [props.json]);

  return <JsonTreeNode label={null} value={parsedValue} depth={0} command={props.command} isRoot />;
}

interface JsonTreeNodeProps {
  label: string | null;
  value: JsonValue;
  depth: number;
  command?: JsonTreeCommand | null;
  isRoot?: boolean;
}

function JsonTreeNode(props: JsonTreeNodeProps) {
  const isArray = Array.isArray(props.value);
  const isObject = typeof props.value === "object" && props.value !== null && !isArray;
  const isExpandable = isArray || isObject;
  const primitiveValue: null | boolean | number | string = !isExpandable
    ? (props.value as null | boolean | number | string)
    : null;
  const [expanded, setExpanded] = useState(props.isRoot || props.depth < 2);
  const lastAppliedCommandVersion = useRef<number | null>(props.command?.version ?? null);

  useEffect(() => {
    if (!props.command || !isExpandable || props.command.version === lastAppliedCommandVersion.current) {
      return;
    }
    lastAppliedCommandVersion.current = props.command.version;
    setExpanded(props.command.type === "expand");
  }, [isExpandable, props.command]);

  if (!isExpandable) {
    return (
      <div className="json-node json-node-leaf">
        <div className="json-node-header">
          {props.label !== null ? <span className="json-key">{props.label}</span> : null}
          {props.label !== null ? <span className="json-colon">:</span> : null}
          <span className={`json-value json-value-${jsonPrimitiveType(primitiveValue)}`}>
            {formatJsonPrimitive(primitiveValue)}
          </span>
        </div>
      </div>
    );
  }

  const entries: ReadonlyArray<readonly [string, JsonValue]> = isArray
    ? (props.value as JsonValue[]).map((item: JsonValue, index: number) => [String(index), item] as const)
    : Object.entries(props.value as { [key: string]: JsonValue });

  return (
    <div className="json-node">
      <div className="json-node-header">
        <button
          type="button"
          className="json-toggle"
          aria-expanded={expanded}
          onClick={() => setExpanded((current) => !current)}
        >
          <span className="tree-toggle-glyph">{expanded ? "-" : "+"}</span>
        </button>
        {props.label !== null ? <span className="json-key">{props.label}</span> : null}
        {props.label !== null ? <span className="json-colon">:</span> : null}
        <span className="json-bracket">{isArray ? "[" : "{"}</span>
        {!expanded ? <span className="json-summary">{formatJsonSummary(props.value)}</span> : null}
        {!expanded ? <span className="json-bracket">{isArray ? "]" : "}"}</span> : null}
      </div>

      {expanded ? (
        <div className="json-node-body">
          {entries.map(([entryKey, entryValue]) => (
            <JsonTreeNode key={entryKey} label={entryKey} value={entryValue} depth={props.depth + 1} command={props.command} />
          ))}
          <div className="json-node-footer">
            <span className="json-bracket">{isArray ? "]" : "}"}</span>
          </div>
        </div>
      ) : null}
    </div>
  );
}

function formatJsonPrimitive(value: null | boolean | number | string) {
  if (typeof value === "string") {
    return `"${value}"`;
  }
  return String(value);
}

function jsonPrimitiveType(value: null | boolean | number | string) {
  if (value === null) {
    return "null";
  }
  return typeof value;
}

function formatJsonSummary(value: JsonValue) {
  if (Array.isArray(value)) {
    return `${value.length} item${value.length === 1 ? "" : "s"}`;
  }
  if (value && typeof value === "object") {
    const size = Object.keys(value).length;
    return `${size} propert${size === 1 ? "y" : "ies"}`;
  }
  return "";
}
