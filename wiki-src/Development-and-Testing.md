# Development and Testing

Use Java 21 or later and Maven. Build from the repository root:

```bash
git clone https://github.com/icher-g/sqm.git
cd sqm
mvn -B verify
```

Use `mvn -B install` when other local projects need the built SQM artifacts.
Development rules and story review/push/merge gates are in
[AGENTS.md](https://github.com/icher-g/sqm/blob/main/AGENTS.md).

## Test tracks

SQM includes:
- Round-trip tests: SQL -> Model -> SQL (golden files)
- Fuzz & property tests: verify idempotency and equivalence
- Renderer compatibility checks per dialect
- JSON serialization consistency tests
- Middleware runtime integration and NFR suites
- Live DB syntax execution suites in `sqm-db-it` for PostgreSQL, MySQL, SQL Server, and Oracle

| Track | Run |
|---|---|
| Unit tests | `mvn -B test` |
| Full verification, coverage, and JavaDoc | `mvn -B verify` |
| Runnable examples | `mvn -B -pl examples -am test` |
| Codegen and plugin checks | `mvn -B -pl sqm-codegen-maven-plugin -am test` |
| Docker/live engines | See [Integration Testing with Docker](Integration-Testing-with-Docker) |
| Playground frontend/backend | See [SQM Playground](SQM-Playground) |

`sqm-it` and `sqm-middleware-it` provide cross-module and transport integration
coverage. Docker-backed execution lives in `sqm-db-it` under the `docker-it`
profile, with one CI job per database. Oracle's baseline, pattern recognition,
MODEL, and flashback regression share one container.

## Documentation and wiki publishing

Edit `wiki-src` in the main repository. Its page names are the published wiki
slugs; add new pages to `Home.md` and `_Sidebar.md`. The README links to these
versioned guide sources so they can be reviewed with the code.

After review and merge, the maintainer can publish the approved sources with:

```powershell
powershell -File scripts/publish-wiki.ps1
```

The publisher copies `wiki-src` to the separate GitHub wiki repository and pushes
it. Review the source changes before publishing.

## Project references

- [Architecture](Architecture)
- [Examples Module Guide](Examples-Module-Guide)
- [Roadmap](https://github.com/icher-g/sqm/blob/main/docs/planning/ROADMAP.md)
- [Downstream support matrix](https://github.com/icher-g/sqm/blob/main/docs/downstream/DOWNSTREAM_SUPPORT_MATRIX.md)
