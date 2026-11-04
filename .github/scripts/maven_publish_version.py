"""Resolve the Maven version and publication destination for publish-maven."""

import os
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

VERSION_PATTERN = re.compile(
    r"[0-9]+(?:\.[0-9]+){2}(?:[.-][0-9A-Za-z][0-9A-Za-z.-]*)?(?:\+[0-9A-Za-z.-]+)?"
)


def normalize_version(value):
    """Normalize an optional v prefix and reject unsafe or malformed versions."""
    version = value.strip()
    if version.startswith(("v", "V")):
        version = version[1:]
    if not VERSION_PATTERN.fullmatch(version):
        raise ValueError("Version must have three numeric components and an optional qualifier")
    if "SNAPSHOT" in version and not version.endswith("-SNAPSHOT"):
        raise ValueError("Snapshot versions must end with -SNAPSHOT")
    return version


def project_version(pom_xml):
    """Read either a literal root version or SQM's CI-friendly revision property."""
    root = ET.fromstring(pom_xml)
    namespace = {"m": "http://maven.apache.org/POM/4.0.0"}
    version = root.findtext("m:version", namespaces=namespace)
    if version == "${revision}":
        version = root.findtext("m:properties/m:revision", namespaces=namespace)
    if not version:
        raise ValueError("Root POM must declare a version or revision")
    return normalize_version(version)


def publication(ref_type, ref_name, input_version, pom_version):
    """Route tag/manual releases to Central and branch/default snapshots to GitHub."""
    requested = normalize_version(input_version) if input_version.strip() else None
    if ref_type == "tag":
        version = normalize_version(ref_name)
        if requested is not None and requested != version:
            raise ValueError("An explicit version must match the selected tag")
    elif requested is not None:
        version = requested
    else:
        version = normalize_version(pom_version)
        if not version.endswith("-SNAPSHOT"):
            version += "-SNAPSHOT"
    destination = "github" if version.endswith("-SNAPSHOT") else "central"
    return version, destination


def main():
    """Write validated, single-line job outputs without evaluating input as shell code."""
    pom = Path(__file__).resolve().parents[2] / "pom.xml"
    version, destination = publication(
        os.environ["GITHUB_REF_TYPE"],
        os.environ["GITHUB_REF_NAME"],
        os.environ.get("INPUT_VERSION", ""),
        project_version(pom.read_text(encoding="utf-8-sig")),
    )
    with Path(os.environ["GITHUB_OUTPUT"]).open("a", encoding="utf-8") as output:
        output.write(f"version={version}\ndestination={destination}\n")
    print(f"Maven version: {version}; destination: {destination}")


if __name__ == "__main__":
    try:
        main()
    except ValueError as error:
        print(f"::error::{error}", file=sys.stderr)
        sys.exit(1)
