package io.sqm.codegen;

import java.nio.file.Path;
import java.util.List;

/**
 * Groups SQL source files from one folder into a single generated Java class.
 *
 * @param folder source folder represented by the generated class
 * @param className generated Java class name
 * @param files SQL source files included in the class
 */
public record SqlFolderGroup(Path folder, String className, List<SqlSourceFile> files) {
}
