import { useEffect, useRef, useState } from "react";
import type { AstChildSlotDto, AstNodeDto } from "../types/api";

export interface AstTreeCommand {
  type: "expand" | "collapse";
  version: number;
}

interface AstNodeTreeProps {
  node: AstNodeDto;
  depth?: number;
  command?: AstTreeCommand | null;
}

/**
 * Renders a single AST node and its recursive children.
 */
export function AstNodeTree(props: AstNodeTreeProps) {
  const depth = props.depth ?? 0;
  const isExpandable = props.node.children.length > 0;
  const [expanded, setExpanded] = useState(depth < 2);
  const lastAppliedCommandVersion = useRef<number | null>(props.command?.version ?? null);

  useEffect(() => {
    if (!props.command || !isExpandable || props.command.version === lastAppliedCommandVersion.current) {
      return;
    }
    lastAppliedCommandVersion.current = props.command.version;
    setExpanded(props.command.type === "expand");
  }, [isExpandable, props.command]);

  return (
    <div className="ast-node">
      <div className="ast-node-header">
        {isExpandable ? (
          <button
            type="button"
            className="ast-toggle"
            aria-expanded={expanded}
            onClick={() => setExpanded((current) => !current)}
          >
            <span className="tree-toggle-glyph">{expanded ? "-" : "+"}</span>
          </button>
        ) : (
          <span className="ast-toggle ast-toggle-placeholder" aria-hidden="true">
            <span className="tree-toggle-glyph">.</span>
          </span>
        )}
        <strong>{props.node.label}</strong>
        {props.node.label !== props.node.nodeType ? (
          <span className="ast-node-meta">{props.node.nodeType}</span>
        ) : null}
        {!expanded ? <span className="ast-collapsed-summary">{formatCollapsedNodeSummary(props.node)}</span> : null}
      </div>

      {expanded ? (
        <div className="ast-node-body">
          {props.node.details.length > 0 ? (
            <dl className="ast-detail-list">
              {props.node.details.map((detail) => (
                <div key={`${detail.name}-${detail.value}`} className="ast-detail-row">
                  <dt>{detail.name}</dt>
                  <dd>{detail.value}</dd>
                </div>
              ))}
            </dl>
          ) : null}

          {isExpandable ? (
            <div className="ast-children">
              {props.node.children.map((slot) => (
                <AstSlotSection key={slot.slot} slot={slot} depth={depth} command={props.command} />
              ))}
            </div>
          ) : null}
        </div>
      ) : null}
    </div>
  );
}

interface AstSlotSectionProps {
  slot: AstChildSlotDto;
  depth: number;
  command?: AstTreeCommand | null;
}

function AstSlotSection(props: AstSlotSectionProps) {
  const isInline = shouldRenderInline(props.slot.nodes);
  const isExpandable = !isInline && props.slot.nodes.length > 0;
  const [expanded, setExpanded] = useState(props.depth < 1);
  const lastAppliedCommandVersion = useRef<number | null>(props.command?.version ?? null);

  useEffect(() => {
    if (!props.command || !isExpandable || props.command.version === lastAppliedCommandVersion.current) {
      return;
    }
    lastAppliedCommandVersion.current = props.command.version;
    setExpanded(props.command.type === "expand");
  }, [isExpandable, props.command]);

  return (
    <section className="ast-slot">
      {isInline ? (
        <dl className="ast-inline-list">
          <div className="ast-detail-row">
            <dt>
              {props.slot.slot}
              {props.slot.multiple ? "[]" : ""}
            </dt>
            <dd className="ast-inline-value">
              <span className="ast-node-meta">{formatInlineType(props.slot.nodes)}</span>
              <span>{formatInlineValue(props.slot.nodes)}</span>
            </dd>
          </div>
        </dl>
      ) : (
        <>
          <div className="ast-slot-header">
            {isExpandable ? (
              <button
                type="button"
                className="ast-toggle"
                aria-expanded={expanded}
                onClick={() => setExpanded((current) => !current)}
              >
                <span className="tree-toggle-glyph">{expanded ? "-" : "+"}</span>
              </button>
            ) : (
              <span className="ast-toggle ast-toggle-placeholder" aria-hidden="true">
                <span className="tree-toggle-glyph">.</span>
              </span>
            )}
            <h4 className="ast-slot-title">
              {props.slot.slot}
              {props.slot.multiple ? "[]" : ""}
            </h4>
            {!expanded ? <span className="ast-collapsed-summary">{formatCollapsedSlotSummary(props.slot)}</span> : null}
          </div>

          {expanded ? (
            <div className="ast-slot-body">
              {props.slot.nodes.length > 0 ? (
                props.slot.nodes.map((childNode, index) => (
                  <AstNodeTree
                    key={`${props.slot.slot}-${childNode.nodeType}-${index}`}
                    node={childNode}
                    depth={props.depth + 1}
                    command={props.command}
                  />
                ))
              ) : (
                <p className="result-placeholder">No nodes in this slot.</p>
              )}
            </div>
          ) : null}
        </>
      )}
    </section>
  );
}

function shouldRenderInline(nodes: AstNodeDto[]) {
  return nodes.length > 0 && nodes.every((node) => node.children.length === 0 && node.details.length > 0);
}

function formatInlineValue(nodes: AstNodeDto[]) {
  if (nodes.length === 1) {
    return summarizeLeafNode(nodes[0]);
  }

  return `[${nodes.map(summarizeLeafNode).join(", ")}]`;
}

function formatInlineType(nodes: AstNodeDto[]) {
  if (nodes.length === 1) {
    return nodes[0].nodeType;
  }

  const distinctTypes = [...new Set(nodes.map((node) => node.nodeType))];
  if (distinctTypes.length === 1) {
    return distinctTypes[0];
  }

  return distinctTypes.join(" | ");
}

function summarizeLeafNode(node: AstNodeDto) {
  const detailMap = new Map(node.details.map((detail) => [detail.name, detail.value]));

  const value = detailMap.get("value");
  if (value) {
    return node.nodeType === "LiteralExpr" ? formatLiteralValue(value) : value;
  }

  const name = detailMap.get("name");
  const tableAlias = detailMap.get("tableAlias");
  if (name && tableAlias) {
    return `${tableAlias}.${name}`;
  }
  if (name) {
    return name;
  }

  const column = detailMap.get("column");
  if (column) {
    return column;
  }

  if (node.details.length === 1) {
    return node.details[0].value;
  }

  return node.label;
}

function formatLiteralValue(value: string) {
  if (value === "null" || value === "true" || value === "false") {
    return value;
  }

  if (/^[+-]?\d+(\.\d+)?$/.test(value)) {
    return value;
  }

  if (
    (value.startsWith("'") && value.endsWith("'")) ||
    (value.startsWith("\"") && value.endsWith("\""))
  ) {
    return value;
  }

  return `'${value}'`;
}

function formatCollapsedNodeSummary(node: AstNodeDto) {
  const summaryParts: string[] = [];

  for (const detail of node.details.slice(0, 2)) {
    summaryParts.push(`${detail.name}: ${detail.value}`);
  }

  const slotLabels = node.children.map((child) => `${child.slot}${child.multiple ? "[]" : ""}`);
  if (slotLabels.length > 0) {
    summaryParts.push(...slotLabels.slice(0, Math.max(0, 3 - summaryParts.length)));
  }

  const remainingCount = node.details.length + slotLabels.length - summaryParts.length;
  if (remainingCount > 0) {
    summaryParts.push("...");
  }

  if (summaryParts.length === 0) {
    return "";
  }

  return `{ ${summaryParts.join(", ")} }`;
}

function formatCollapsedSlotSummary(slot: AstChildSlotDto) {
  if (slot.nodes.length === 0) {
    return "[ 0 nodes ]";
  }

  if (shouldRenderInline(slot.nodes)) {
    return formatInlineValue(slot.nodes);
  }

  if (slot.nodes.length === 1) {
    return formatCollapsedNodeSummary(slot.nodes[0]) || slot.nodes[0].label;
  }

  const distinctTypes = [...new Set(slot.nodes.map((node) => node.nodeType))];
  if (distinctTypes.length === 1) {
    return `[ ${slot.nodes.length} ${distinctTypes[0]} node${slot.nodes.length === 1 ? "" : "s"} ]`;
  }

  return `[ ${slot.nodes.length} nodes ]`;
}
