package io.sqm.playground.rest.service;

import org.springframework.stereotype.Service;

import javax.tools.*;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;

/**
 * An in memory compiler used to validate the DSL generated code.
 */
@Service
public class InMemoryCompiler {

    public boolean compile(String className, String source, DiagnosticCollector<JavaFileObject> diagnostics) {
        var compiler = ToolProvider.getSystemJavaCompiler();
        var standardFileManager = compiler.getStandardFileManager(diagnostics, null, null);
        var fileManager = new InMemoryFileManager(standardFileManager);
        var sourceFile = new SourceFile(className, source);
        var task = compiler.getTask(
            null,
            fileManager,
            diagnostics,
            null,
            null,
            List.of(sourceFile)
        );
        return Boolean.TRUE.equals(task.call());
    }

    private static class SourceFile extends SimpleJavaFileObject {
        private final String code;

        SourceFile(String className, String code) {
            super(
                URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension),
                Kind.SOURCE
            );
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }

    private static class ClassFile extends SimpleJavaFileObject {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();

        ClassFile(String className) {
            super(
                URI.create("mem:///" + className.replace('.', '/') + Kind.CLASS.extension),
                Kind.CLASS
            );
        }

        @Override
        public OutputStream openOutputStream() {
            return out;
        }
    }

    private static class InMemoryFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {

        InMemoryFileManager(StandardJavaFileManager fileManager) {
            super(fileManager);
        }

        @Override
        public JavaFileObject getJavaFileForOutput(
            Location location,
            String className,
            JavaFileObject.Kind kind,
            FileObject sibling
        ) {
            return new ClassFile(className);
        }
    }
}
