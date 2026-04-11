import type { AstNodeDto } from "../types/api";

interface AstNodeTreeProps {
  node: AstNodeDto;
}

/**
 * Renders a single AST node and its recursive children.
 */
export function AstNodeTree(props: AstNodeTreeProps) {
  return (
    <div className="ast-node">
      <div className="ast-node-header">
        <strong>{props.node.label}</strong>
        {props.node.label !== props.node.nodeType ? (
          <span className="ast-node-meta">{props.node.nodeType}</span>
        ) : null}
      </div>

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

      {props.node.children.length > 0 ? (
        <div className="ast-children">
          {props.node.children.map((slot) => (
            <section key={slot.slot} className="ast-slot">
              {shouldRenderInline(slot.nodes) ? (
                <dl className="ast-inline-list">
                  <div className="ast-detail-row">
                    <dt>
                      {slot.slot}
                      {slot.multiple ? "[]" : ""}
                    </dt>
                    <dd className="ast-inline-value">
                      <span className="ast-node-meta">{formatInlineType(slot.nodes)}</span>
                      <span>{formatInlineValue(slot.nodes)}</span>
                    </dd>
                  </div>
                </dl>
              ) : (
                <>
                  <h4 className="ast-slot-title">
                    {slot.slot}
                    {slot.multiple ? "[]" : ""}
                  </h4>

                  {slot.nodes.length > 0 ? (
                    slot.nodes.map((childNode, index) => (
                      <AstNodeTree key={`${slot.slot}-${childNode.nodeType}-${index}`} node={childNode} />
                    ))
                  ) : (
                    <p className="result-placeholder">No nodes in this slot.</p>
                  )}
                </>
              )}
            </section>
          ))}
        </div>
      ) : null}
    </div>
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
