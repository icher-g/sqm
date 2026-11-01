# SQL File Codegen

`sqm-codegen-maven-plugin` generates Java query classes from `src/main/sql/**/*.sql`.

## Basic Maven Config

```xml
<plugin>
  <groupId>io.sqm</groupId>
  <artifactId>sqm-codegen-maven-plugin</artifactId>
  <version>${sqm.version}</version>
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
- `...Params()` methods returning an immutable `Set<String>` of named parameter names

## Next

- [SQL File Codegen Schema Validation](SQL-File-Codegen-Schema-Validation)
- [Examples Module Guide](Examples-Module-Guide)

## Worked SQL-file example

```sql
-- src/main/sql/user/find_by_id.sql
select u.id, u.user_name
from users u
where u.id = :id
```

Generated usage:

```java
import com.acme.generated.UserQueries;

var query = UserQueries.findById();
var params = UserQueries.findByIdParams(); // ["id"]
```

Multi-statement `.sql` files are parsed as `StatementSequence`. Generated code
exposes a sequence-level method plus per-statement methods with stable names,
and parameter metadata is collected across the whole script.

The plugin runs in `generate-sources`, writes generated Java files under `target/generated-sources/sqm-codegen`, and Maven compiles them together with your source code.
