"""Offline checks for publication routing and version-input handling."""

import os
import unittest
from unittest.mock import mock_open, patch

from maven_publish_version import main, normalize_version, project_version, publication


class PublicationVersionTest(unittest.TestCase):
    def test_main_preserves_snapshot_version(self):
        self.assertEqual(("0.5.0-SNAPSHOT", "github"),
                         publication("branch", "main", "", "0.5.0-SNAPSHOT"))

    def test_branch_converts_release_pom_to_snapshot(self):
        self.assertEqual(("0.5.0-SNAPSHOT", "github"),
                         publication("branch", "main", "", "0.5.0"))

    def test_release_tags_go_to_central(self):
        for tag, expected in [("v0.5.0", "0.5.0"), ("v0.6.0-rc.1", "0.6.0-rc.1")]:
            with self.subTest(tag=tag):
                self.assertEqual((expected, "central"), publication("tag", tag, "", "0.5.0-SNAPSHOT"))

    def test_manual_release_overrides_branch_snapshot(self):
        self.assertEqual(("0.5.0", "central"),
                         publication("branch", "main", " v0.5.0 ", "0.5.0-SNAPSHOT"))

    def test_manual_snapshot_goes_to_github(self):
        self.assertEqual(("0.6.0-SNAPSHOT", "github"),
                         publication("branch", "main", "0.6.0-SNAPSHOT", "0.5.0-SNAPSHOT"))

    def test_snapshot_tag_stays_in_snapshot_repository(self):
        self.assertEqual(("0.6.0-SNAPSHOT", "github"),
                         publication("tag", "v0.6.0-SNAPSHOT", "", "0.5.0-SNAPSHOT"))

    def test_manual_version_cannot_relabel_a_tag(self):
        with self.assertRaisesRegex(ValueError, "match"):
            publication("tag", "v0.5.0", "0.6.0", "0.5.0-SNAPSHOT")

    def test_matching_manual_version_and_tag_are_accepted(self):
        self.assertEqual(("0.5.0", "central"), publication("tag", "v0.5.0", "0.5.0", "0.5.0-SNAPSHOT"))

    def test_invalid_versions_are_rejected(self):
        for version in ("latest", "0.5", "0.5.0; echo bad", "$(echo 0.5.0)",
                        "0.5.0\ndestination=github", "0.5 .0", "${revision}", ""):
            with self.subTest(version=version):
                with self.assertRaises(ValueError):
                    normalize_version(version)

    def test_invalid_tag_does_not_fall_back_to_pom_version(self):
        with self.assertRaises(ValueError):
            publication("tag", "vlatest", "", "0.5.0-SNAPSHOT")

    def test_snapshot_marker_cannot_be_hidden_by_build_metadata(self):
        with self.assertRaisesRegex(ValueError, "Snapshot"):
            normalize_version("0.5.0-SNAPSHOT+build")

    def test_build_metadata_is_preserved(self):
        self.assertEqual("0.5.0-rc.1+build.4", normalize_version("V0.5.0-rc.1+build.4"))

    def test_ci_friendly_project_version(self):
        xml = ('<project xmlns="http://maven.apache.org/POM/4.0.0">'
               '<version>${revision}</version><properties><revision>0.5.0-SNAPSHOT</revision>'
               '</properties></project>')
        self.assertEqual("0.5.0-SNAPSHOT", project_version(xml))

    def test_literal_project_version(self):
        xml = '<project xmlns="http://maven.apache.org/POM/4.0.0"><version>0.5.0</version></project>'
        self.assertEqual("0.5.0", project_version(xml))

    def test_missing_project_revision_fails(self):
        xml = '<project xmlns="http://maven.apache.org/POM/4.0.0"><version>${revision}</version></project>'
        with self.assertRaisesRegex(ValueError, "declare"):
            project_version(xml)

    def test_main_emits_normalized_job_outputs(self):
        xml = '<project xmlns="http://maven.apache.org/POM/4.0.0"><version>0.5.0-SNAPSHOT</version></project>'
        output = mock_open()
        with patch.dict(os.environ, {"GITHUB_REF_TYPE": "branch", "GITHUB_REF_NAME": "main",
                                     "INPUT_VERSION": "v0.5.0", "GITHUB_OUTPUT": "job-output"}), \
             patch("maven_publish_version.Path.read_text", return_value=xml), \
             patch("maven_publish_version.Path.open", output), patch("builtins.print"):
            main()
        output().write.assert_called_once_with("version=0.5.0\ndestination=central\n")

    def test_main_does_not_emit_outputs_for_invalid_input(self):
        xml = '<project xmlns="http://maven.apache.org/POM/4.0.0"><version>0.5.0-SNAPSHOT</version></project>'
        with patch.dict(os.environ, {"GITHUB_REF_TYPE": "branch", "GITHUB_REF_NAME": "main",
                                     "INPUT_VERSION": "0.5.0\ndestination=github", "GITHUB_OUTPUT": "job-output"}), \
             patch("maven_publish_version.Path.read_text", return_value=xml), \
             patch("maven_publish_version.Path.open") as output:
            with self.assertRaises(ValueError):
                main()
            output.assert_not_called()


if __name__ == "__main__":
    unittest.main()
