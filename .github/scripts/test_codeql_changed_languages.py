import unittest

from codeql_changed_languages import select_languages


class CodeQLChangedLanguagesTest(unittest.TestCase):
    def test_selects_individual_languages_and_build_inputs(self):
        cases = {
            "src/main/java/example/App.java": ["java-kotlin"],
            "build.gradle": ["java-kotlin"],
            "gradle/wrapper/gradle-wrapper.properties": ["java-kotlin"],
            ".github/workflows/ash.yml": ["actions"],
            "src/main/resources/static/app.js": ["javascript-typescript"],
            "src/main/resources/templates/index.html": ["javascript-typescript"],
            "docs/example.js": ["javascript-typescript"],
        }
        for path, expected in cases.items():
            with self.subTest(path=path):
                self.assertEqual(select_languages([path]), expected)

    def test_combines_languages_for_multiple_changes(self):
        self.assertEqual(
            select_languages(["src/main/App.java", ".github/actions/custom/action.yml"]),
            ["actions", "java-kotlin"],
        )

    def test_documentation_only_has_no_languages(self):
        self.assertEqual(select_languages(["README.md", "docs/design.md"]), [])

    def test_unknown_and_codeql_configuration_changes_scan_all(self):
        expected = ["actions", "java-kotlin", "javascript-typescript"]
        for path in ("src/main/resources/application.yml", "docs/example.yaml", ".github/workflows/codeql.yml", ".github/scripts/codeql_changed_languages.py"):
            with self.subTest(path=path):
                self.assertEqual(select_languages([path]), expected)


if __name__ == "__main__":
    unittest.main()
