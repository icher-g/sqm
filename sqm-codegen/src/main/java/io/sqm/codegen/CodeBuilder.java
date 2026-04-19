package io.sqm.codegen;

import java.util.List;
import java.util.function.Consumer;

/**
 * Small fluent writer for generated Java source snippets.
 */
public interface CodeBuilder {

    /**
     * Creates a builder using the default indentation size.
     *
     * @return a new code builder
     */
    static CodeBuilder of() {
        return of(4);
    }

    /**
     * Creates a builder using the provided indentation size.
     *
     * @param indentSize number of spaces per indentation level
     * @return a new code builder
     */
    static CodeBuilder of(int indentSize) {
        return new Impl(indentSize);
    }

    /**
     * Resets the builder to initial state.
     */
    void reset();

    /**
     * Gets the current line number within the builder.
     *
     * @return a current line number;
     */
    int currentLineNumber();

    /**
     * Appends a string to the builder.
     *
     * @param s a string to append.
     * @return this.
     */
    CodeBuilder append(String s);

    /**
     * Appends a quoted string to the builder.
     *
     * @param s a string to append.
     * @return this.
     */
    default CodeBuilder quote(String s) {
        return quote(s, '"');
    }

    /**
     * Appends a quoted string to the builder.
     *
     * @param s     a string to append.
     * @param quote a character to quote with.
     * @return this.
     */
    CodeBuilder quote(String s, Character quote);

    /**
     * Appends an escaped string to the builder.
     *
     * @param s a string to append.
     * @return this.
     */
    CodeBuilder escape(String s);

    /**
     * Appends a space to the builder.
     *
     * @return this.
     */
    CodeBuilder space();

    /**
     * Appends new line to the builder.
     *
     * @return this.
     */
    CodeBuilder nl();

    /**
     * Increases the current indent used by the builder.
     *
     * @return this.
     */
    CodeBuilder in();

    /**
     * Decreases the current indent used by the builder.
     *
     * @return this.
     */
    CodeBuilder out();

    /**
     * Appends a list of entities separated by comma.
     *
     * @param parts a list of entities to append.
     * @return this.
     */
    default CodeBuilder comma(List<String> parts) {
        return comma(parts, p -> append(p), false);
    }

    /**
     * Appends a list of entities separated by comma.
     *
     * @param parts   a list of entities to append.
     * @param newLine indicates whether each part needs to be started from new line.
     * @return this.
     */
    default CodeBuilder comma(List<String> parts, boolean newLine) {
        return comma(parts, p -> append(p), newLine);
    }

    /**
     * Appends a list of entities separated by comma and optionally each one on new line.
     *
     * @param parts  a list of entities to append.
     * @param writer a writer to use to write the part.
     * @param <T>    the type of the part.
     * @return this.
     */
    default <T> CodeBuilder comma(List<T> parts, Consumer<T> writer) {
        return comma(parts, writer, false);
    }

    /**
     * Appends a list of entities separated by comma and optionally each one on new line.
     *
     * @param parts   a list of entities to append.
     * @param writer  a writer to use to write the part.
     * @param newLine indicates whether each part needs to be started from new line.
     * @param <T>     the type of the part.
     * @return this.
     */
    default <T> CodeBuilder comma(List<T> parts, Consumer<T> writer, boolean newLine) {
        if (parts == null || parts.isEmpty()) return this;
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                append(",");
                if (newLine) nl();
                else append(" ");
            }
            writer.accept(parts.get(i));
        }
        return this;
    }

    /**
     * Default mutable {@link CodeBuilder} implementation.
     */
    class Impl implements CodeBuilder {

        private final int indentSize;
        private StringBuilder sb;
        private int indentLevel = 0;
        private int currentLineNumber = 0;
        private boolean atLineStart = true;

        public Impl(int indentSize) {
            this.sb = new StringBuilder();
            this.indentSize = Math.max(0, indentSize);
        }

        private static String escapeJava(String value) {
            return value.replace("\\", "\\\\").replace("\"", "\\\"");
        }

        /**
         * Resets the builder.
         */
        @Override
        public void reset() {
            this.sb = new StringBuilder();
            this.indentLevel = 0;
            this.atLineStart = true;
        }

        /**
         * Gets the current line number within the builder.
         *
         * @return a current line number;
         */
        @Override
        public int currentLineNumber() {
            return currentLineNumber;
        }

        /**
         * Appends a string to the query.
         *
         * @param s a string to append.
         * @return this.
         */
        @Override
        public CodeBuilder append(String s) {
            if (s == null || s.isEmpty()) {
                return this;
            }
            indent(s);
            return this;
        }

        /**
         * Appends a string to the query.
         *
         * @param s a string to append.
         * @return this.
         */
        @Override
        public CodeBuilder quote(String s, Character quote) {
            if (s == null || s.isEmpty()) {
                return this;
            }
            append(quote + escapeJava(s) + quote);
            return this;
        }

        /**
         * Appends an escaped string to the builder.
         *
         * @param s a string to append.
         * @return this.
         */
        @Override
        public CodeBuilder escape(String s) {
            if (s == null || s.isEmpty()) {
                return this;
            }
            append(escapeJava(s));
            return this;
        }

        /**
         * Appends a space to the query.
         *
         * @return this.
         */
        @Override
        public CodeBuilder space() {
            // no leading spaces at the start of a line
            if (atLineStart) return this;

            int len = sb.length();
            if (len == 0) return this;

            char last = sb.charAt(len - 1);
            if (!Character.isWhitespace(last)) {
                sb.append(' ');
            }
            return this;
        }

        /**
         * Appends new line to the query if ignore new line is set to false.
         *
         * @return this.
         */
        @Override
        public CodeBuilder nl() {
            sb.append('\n');
            atLineStart = true;
            currentLineNumber++;
            return this;
        }

        /**
         * Increases the current indent used by the writer.
         *
         * @return this.
         */
        @Override
        public CodeBuilder in() {
            indentLevel++;
            return this;
        }

        /**
         * Decreases the current indent used by the writer.
         *
         * @return this.
         */
        @Override
        public CodeBuilder out() {
            if (indentLevel > 0) indentLevel--;
            return this;
        }

        @Override
        public String toString() {
            return sb.toString();
        }

        private void indent(String s) {
            if (atLineStart) {
                if (indentLevel > 0 && indentSize > 0) {
                    int spaces = indentLevel * indentSize;
                    var indent = " ".repeat(Math.max(0, spaces));
                    sb.append(indent);
                    s = s.replace("\n", "\n" + indent);
                }
                atLineStart = false;
            }
            sb.append(s);
        }
    }
}
