import Prism from "prismjs";
import "prismjs/components/prism-clike";
import "prismjs/components/prism-java";
import "prismjs/components/prism-json";
import "prismjs/components/prism-sql";

type CodeLanguage = "java" | "json" | "sql";

interface CodeBlockProps {
  code: string;
  language: CodeLanguage;
}

/**
 * Renders read-only code with lightweight Prism syntax highlighting.
 */
export function CodeBlock(props: CodeBlockProps) {
  const grammar = Prism.languages[props.language];
  const highlightedCode = grammar
    ? Prism.highlight(props.code, grammar, props.language)
    : Prism.util.encode(props.code).toString();

  return (
    <pre className={`result-code-block language-${props.language}`}>
      <code
        className={`language-${props.language}`}
        dangerouslySetInnerHTML={{ __html: highlightedCode }}
      />
    </pre>
  );
}
