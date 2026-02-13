# SQL File Codegen

`sqm-codegen-maven-plugin` generates Java query classes from `src/main/sql/**/*.sql`.

## Basic Maven Config

```xml
<plugin>
  <groupId>io.sqm</groupId>
  <artifactId>sqm-codegen-maven-plugin</artifactId>
  <version>${project.version}</version>
  <executions>
    <execution>
      <goals><goal>generate</goal></goals>
    </execution>
  </executions>
  <configuration>
    <dialect>postgresql</dialect>
    <basePackage>com.acme.generated</basePackage>
    <sqlDirectory>${project.basedir}/src/main/sql</sqlDirectory>
    <generatedSourcesDirectory>${project.build.directory}/generated-sources/sqm-codegen</generatedSourcesDirectory>
  </configuration>
</plugin>
```

## Input Convention

- Folder becomes class group
- File name becomes method name

Example:

- `src/main/sql/user/find_by_id.sql` -> `UserQueries.findById()`

## Output

- Query factory methods
- `...Params()` methods with named parameter names

## Next

- [SQL File Codegen Schema Validation](SQL-File-Codegen-Schema-Validation)
- [Examples Module Guide](Examples-Module-Guide)

