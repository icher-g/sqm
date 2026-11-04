# Maven Central Publishing

SQM's Maven groupId is `io.github.icher-g`, under the verified GitHub namespace.
Java packages and imports remain `io.sqm.*`. Existing `io.sqm` coordinates are
the historical GitHub Packages coordinates; new consumer POMs and Maven plugin
declarations should use the new groupId.

The `central-release` profile stages a complete reactor deployment through the
Sonatype Central Publisher Portal. It includes `sqm-parent`, the library modules,
middleware, playground, and codegen plugin. It excludes `examples`, `sqm-it`,
`sqm-db-it`, and `sqm-middleware-it`.
Those four modules belong to the default `development` profile. They run in the
ordinary reactor; selecting `central-release` leaves them out of the release
reactor. Run `mvn -B verify` before preparing a release to include their checks.

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

The ordinary build keeps its existing GitHub Packages deployment target.
Use `-Pcentral-release` explicitly to select Central. Publishing profile changes
alone do not create Central credentials, signing keys, or a GitHub Actions workflow.

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
