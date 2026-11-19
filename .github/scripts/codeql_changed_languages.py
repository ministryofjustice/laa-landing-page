import json
import os
from pathlib import PurePosixPath
import subprocess
import sys


LANGUAGES = ("actions", "java-kotlin", "javascript-typescript")
JAVA_SUFFIXES = {".java", ".kt", ".kts"}
JS_SUFFIXES = {".js", ".jsx", ".mjs", ".cjs", ".ts", ".tsx", ".mts", ".cts", ".html", ".json"}
DOC_SUFFIXES = {".md", ".rst", ".adoc"}


def select_languages(paths):
    selected = set()
    for path in paths:
        name = PurePosixPath(path).name
        suffix = PurePosixPath(path).suffix.lower()
        if (
            path == ".github/workflows/codeql.yml"
            or path.startswith((".github/codeql/", ".github/scripts/codeql_changed_languages"))
            or suffix in {".ql", ".qll", ".qls"}
        ):
            return list(LANGUAGES)
        if suffix in DOC_SUFFIXES:
            continue

        matched = False
        if path.startswith((".github/workflows/", ".github/actions/")) or name in {"action.yml", "action.yaml"}:
            selected.add("actions")
            matched = True
        if (
            suffix in JAVA_SUFFIXES
            or name.endswith((".gradle", ".gradle.kts"))
            or name in {"gradle.properties", "gradlew", "gradlew.bat", "pom.xml", "mvnw", "mvnw.cmd"}
            or path.startswith(("gradle/", "buildSrc/"))
        ):
            selected.add("java-kotlin")
            matched = True
        if suffix in JS_SUFFIXES or name in {"yarn.lock", "pnpm-lock.yaml", "bun.lock", "bun.lockb"}:
            selected.add("javascript-typescript")
            matched = True
        if not matched:
            return list(LANGUAGES)
    return [language for language in LANGUAGES if language in selected]


def main():
    if len(sys.argv) != 2 or not sys.argv[1]:
        raise ValueError("A pull request base commit is required")
    diff = subprocess.run(
        ["git", "diff", "--name-only", "-z", "--no-renames", sys.argv[1], "HEAD", "--"],
        check=True,
        stdout=subprocess.PIPE,
    )
    paths = [os.fsdecode(path) for path in diff.stdout.split(b"\0") if path]
    languages = select_languages(paths)
    print(f"Changed files: {len(paths)}; CodeQL languages: {', '.join(languages) or 'none'}")
    with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as output:
        output.write(f"languages={json.dumps(languages, separators=(',', ':'))}\n")


if __name__ == "__main__":
    main()
