# Maven Central Publishing

SQM's Maven groupId is `io.github.icher-g`, under the verified GitHub namespace.
Java packages and imports remain `io.sqm.*`. Existing `io.sqm` coordinates are
the historical GitHub Packages coordinates; new consumer POMs and Maven plugin
declarations should use the new groupId.

The `central-release` profile stages a complete reactor deployment through the
Sonatype Central Publisher Portal. It includes `sqm-parent`, the library modules,
middleware, playground API, and codegen plugin. It excludes the standalone
`sqm-playground-rest` application, which is distributed through the
[playground Docker image](SQM-Playground.md#build-and-publish-the-image), as well
as `examples`, `sqm-it`, `sqm-db-it`, and `sqm-middleware-it`.
The four example/integration modules belong to the default `development` profile. They run in the
ordinary reactor; selecting `central-release` leaves them out of the release
reactor. Run `mvn -B verify` before preparing a release to include their checks.
The playground REST application still builds in the release reactor; only its
Central publication is excluded.

## Publishing usage

The Central plugin uses `<checksums>required</checksums>`: MD5 and SHA-1 checksums
are uploaded; the optional SHA-256 and SHA-512 checksum files are omitted.
The main artifact, POM, sources, Javadoc, and GPG signatures remain included.
A standard library module therefore contributes 16 files rather than 24.
With the current module set and the playground application excluded, a signed
release contains 596 files rather than the 918 files in v0.5.1. Counts will change
when modules or attached artifacts change.

Before publishing, review the bundle and the organization's
[Usage Center](https://central.sonatype.org/publish/maven-central-publishing-limits/#how-to-monitor-usage).
Limits are measured per calendar month; the smaller bundle can still exceed the
default file limit when multiple full releases are published in one month.
For SQM's legitimate multi-module open-source releases, consider requesting
higher limits or an exemption under
[Sonatype's community publishing policy](https://central.sonatype.org/publish/maven-central-publishing-limits/#community-open-source-publishing).
Include the namespace `io.github.icher-g`, module count, expected release frequency,
and whether a burst of releases is due to security fixes. Changing future bundle
settings does not reduce usage from releases already published.

## Credentials and signing

Generate a [Central Portal user token](https://central.sonatype.org/publish/generate-portal-token/).
Its generated username and password go in a separate `central` server entry in
your Maven user settings (`%USERPROFILE%\.m2\settings.xml` on Windows):

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0">
  <servers>
    <server>
      <id>central</id>
      <username>${env.CENTRAL_TOKEN_USERNAME}</username>
      <password>${env.CENTRAL_TOKEN_PASSWORD}</password>
    </server>
  </servers>
</settings>
```

Merge this server into existing settings if other repositories are configured.
Set those environment variables in the process that runs Maven; IntelliJ Maven
runners must receive them too.

Create a GPG signing key and publish its public key following
[Sonatype's signing guide](https://central.sonatype.org/publish/requirements/gpg/).
The release profile uses Maven GPG Plugin and the local GPG executable/agent.
For unattended signing, its standard `MAVEN_GPG_PASSPHRASE` environment variable
is supported. Select a particular local key with `"-Dgpg.keyname=FINGERPRINT"`
when needed.

## Build and upload a release

From the approved release checkout, run:

```powershell
mvn -B -ntp -Pcentral-release clean deploy
```

The profile attaches sources and Javadoc, signs the POM and attached artifacts,
and lets the Central plugin generate checksums and a deployment bundle.
Flattening runs before packaging/signing so installed and published POMs have
resolved `${revision}` values. The parent POM is published because its children
reference it.

The upload waits for Central validation. `autoPublish` is false: after successful
validation, review the deployment in the
[Central Portal](https://central.sonatype.com/publishing/deployments) and publish
it there. A successful upload/validation does not mean the release is public yet.

The ordinary local build keeps its existing GitHub Packages deployment target.
Use `-Pcentral-release` explicitly to select Central when publishing locally.

## GitHub Actions

The `publish-maven` workflow routes versions to two destinations:

| Trigger/version                               | Destination                                                      |
|-----------------------------------------------|------------------------------------------------------------------|
| Push to `main`                                | GitHub Packages, using the POM version with a `-SNAPSHOT` suffix |
| Release tag such as `v0.6.0` or `v0.6.0-rc.1` | Central Portal                                                   |
| Manual run with an explicit release version   | Central Portal                                                   |
| Manual run with a `-SNAPSHOT` version         | GitHub Packages                                                  |

For a manual run with a blank version, a selected tag supplies the version; a
selected branch uses the POM-based snapshot version. If a version is supplied
while selecting a tag, it must match that tag. Snapshots stay in GitHub Packages.

Configure these repository Actions secrets:

| Secret                   | Value                                               |
|--------------------------|-----------------------------------------------------|
| `CENTRAL_TOKEN_USERNAME` | Generated Central Portal token username             |
| `CENTRAL_TOKEN_PASSWORD` | Generated Central Portal token password             |
| `MAVEN_GPG_PRIVATE_KEY`  | Complete ASCII-armored exported private signing key |
| `MAVEN_GPG_PASSPHRASE`   | Signing key passphrase, if set                      |

The Central job generates Maven settings for server `central` and uses Maven GPG
Plugin's Bouncy Castle signer. The exported key is supplied through
`MAVEN_GPG_KEY` in the signing step; no interactive key import is needed.
The public signing key must already be published as described above.

Both jobs run the full default verification and installation reactor before
publishing, making the codegen plugin available to the example module. They use
`-Drevision` to apply the selected version consistently, and serialize publication
of the same version to avoid concurrent uploads. The Central job archives its
deployment bundle and waits for validation. Final publication remains manual in
the Portal, as with local `central-release` builds. GitHub release notes are
managed independently.

To publish an existing release whose old tag predates this workflow, select
`main` (or another reviewed ref containing the Central configuration) in
Actions → `publish-maven` → Run workflow, and enter the release version, such as
`0.5.0`. The selected checkout is the source being published.

## Check a bundle locally

To build a signed bundle without uploading:

```powershell
mvn -B -ntp -Pcentral-release "-Dcentral.skipUpload=true" clean deploy
```

For a packaging check before a signing key is configured:

```powershell
mvn -B -ntp -Pcentral-release "-Dcentral.skipUpload=true" "-Dgpg.skip=true" clean deploy
```

The second command produces an unsigned test bundle, which is not suitable for
publication. The bundle is under `target/central-publishing/central-bundle.zip`.
No Central credentials are required when upload is skipped.

## Consumers

After the release has been published to Central, consumers can use standard Maven
dependencies without GitHub credentials or a custom repository entry:

```xml
<dependency>
  <groupId>io.github.icher-g</groupId>
  <artifactId>sqm-core</artifactId>
  <version>${sqm.version}</version>
</dependency>
```

See [Central publishing requirements](https://central.sonatype.org/publish/requirements/)
and the [Central Maven plugin guide](https://central.sonatype.org/publish/publish-portal-maven/).
