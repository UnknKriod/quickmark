#!/usr/bin/env python3
"""Generate release notes from Conventional Commits.

Zero dependencies: stdlib only, so it runs in this repo's toolchain (no Node,
no Rust, no network). Works against any git repository.

Commit format understood (Conventional Commits v1.0.0):

    <type>(<scope>)!: <subject>
    <BLANK LINE>
    <body>
    <BLANK LINE>
    BREAKING CHANGE: <what breaks>

A commit is treated as breaking when the header carries `!` or the body has a
`BREAKING CHANGE:` / `BREAKING-CHANGE:` footer. Merge commits and version-bump
commits are never listed.

Examples:
    python3 tools/release_notes.py                    # HEAD since the last tag
    python3 tools/release_notes.py --version 2.1
    python3 tools/release_notes.py --from 2.0 --to HEAD
    python3 tools/release_notes.py --output build/release-notes.md
    python3 tools/release_notes.py --github           # body only, for a GitHub release
    python3 tools/release_notes.py --all              # include chore/test/ci/style
"""

from __future__ import annotations

import argparse
import datetime as dt
import os
import re
import subprocess
import sys

# --- configuration ---------------------------------------------------------

# Output order. Every type listed here gets its own section, in this order.
SECTIONS: list[tuple[str, str]] = [
    ("breaking", "Breaking Changes"),
    ("feat", "Features"),
    ("fix", "Fixes"),
    ("perf", "Performance"),
    ("refactor", "Refactoring"),
    ("revert", "Reverts"),
    ("build", "Build"),
    ("docs", "Documentation"),
    ("test", "Tests"),
    ("ci", "CI"),
    ("style", "Style"),
    ("chore", "Chores"),
]

# Types hidden unless --all is given: bookkeeping, not user-facing.
QUIET_TYPES = {"chore", "style", "test", "ci"}

# Never listed, even with --all: merge commits and release bookkeeping.
SKIP_ALWAYS = re.compile(r"^(Merge\b|chore\(release\))", re.IGNORECASE)

HEADER = re.compile(
    r"^(?P<type>[A-Za-z]+)"
    r"(?:\((?P<scope>[^)]*)\))?"
    r"(?P<breaking>!)?"
    r":\s*(?P<subject>.+)$"
)
BREAKING_FOOTER = re.compile(r"^BREAKING[ -]CHANGE:\s*(?P<text>.+)$", re.IGNORECASE | re.MULTILINE)

RECORD_SEP = "\x1e"
FIELD_SEP = "\x1f"


# --- git helpers -----------------------------------------------------------

def git(repo: str, *args: str) -> str:
    result = subprocess.run(
        ["git", "-C", repo, *args],
        capture_output=True,
        text=True,
        check=False,
    )
    if result.returncode != 0:
        raise SystemExit(f"git {' '.join(args)} failed:\n{result.stderr.strip()}")
    return result.stdout


def last_tag(repo: str) -> str | None:
    result = subprocess.run(
        ["git", "-C", repo, "describe", "--tags", "--abbrev=0"],
        capture_output=True,
        text=True,
        check=False,
    )
    return result.stdout.strip() or None if result.returncode == 0 else None


def repo_url(repo: str) -> str | None:
    result = subprocess.run(
        ["git", "-C", repo, "remote", "get-url", "origin"],
        capture_output=True,
        text=True,
        check=False,
    )
    url = result.stdout.strip()
    if not url:
        return None
    # git@host:owner/repo.git -> https://host/owner/repo
    url = re.sub(r"^git@([^:]+):", r"https://\1/", url)
    return re.sub(r"\.git$", "", url)


def read_gradle_version(repo: str) -> str | None:
    """Read mod_version from gradle.properties, the project's source of truth."""
    path = os.path.join(repo, "gradle.properties")
    if not os.path.isfile(path):
        return None
    with open(path, encoding="utf-8") as handle:
        for line in handle:
            match = re.match(r"\s*mod_version\s*=\s*(\S+)", line)
            if match:
                return match.group(1)
    return None


# --- parsing --------------------------------------------------------------

class Commit:
    def __init__(self, sha: str, header: str, body: str) -> None:
        self.sha = sha
        self.short = sha[:7]
        self.header = header
        self.body = body

        match = HEADER.match(header)
        if match:
            self.type = match.group("type").lower()
            self.scope = match.group("scope")
            self.subject = match.group("subject").strip()
            self.breaking = bool(match.group("breaking"))
        else:
            # Not conventional: keep it, but file it under "Other".
            self.type = "other"
            self.scope = None
            self.subject = header.strip()
            self.breaking = False

        footer = BREAKING_FOOTER.search(body)
        self.breaking_note = footer.group("text").strip() if footer else None
        if footer:
            self.breaking = True

    @property
    def is_skippable(self) -> bool:
        return bool(SKIP_ALWAYS.match(self.header))


def collect_commits(repo: str, revision_range: str | None) -> list[Commit]:
    args = ["log", f"--pretty=format:%H{FIELD_SEP}%s{FIELD_SEP}%b{RECORD_SEP}"]
    if revision_range:
        args.append(revision_range)

    raw = git(repo, *args)
    commits: list[Commit] = []
    for record in raw.split(RECORD_SEP):
        record = record.strip("\n")
        if not record:
            continue
        parts = record.split(FIELD_SEP, 2)
        if len(parts) < 2:
            continue
        sha, header = parts[0], parts[1]
        body = parts[2] if len(parts) > 2 else ""
        commit = Commit(sha, header, body)
        if not commit.is_skippable:
            commits.append(commit)

    commits.reverse()  # chronological: oldest first
    return commits


# --- rendering ------------------------------------------------------------

def render(commits: list[Commit], version: str, date: str, base_url: str | None,
           show_all: bool, header: bool, compare: str | None) -> str:
    buckets: dict[str, list[Commit]] = {key: [] for key, _ in SECTIONS}
    buckets["other"] = []

    for commit in commits:
        if commit.breaking:
            buckets["breaking"].append(commit)
        if commit.type in QUIET_TYPES and not show_all:
            continue
        buckets.setdefault(commit.type, []).append(commit)

    titles = dict(SECTIONS)
    titles["other"] = "Other"

    lines: list[str] = []
    if header:
        lines.append(f"## {version} — {date}")
        lines.append("")

    for key, _ in SECTIONS:
        bucket = buckets.get(key, [])
        if not bucket:
            continue
        lines.append(f"### {titles[key]}")
        lines.append("")
        for commit in bucket:
            lines.append(format_bullet(commit, base_url))
            if key == "breaking" and commit.breaking_note:
                lines.append(f"  - {commit.breaking_note}")
        lines.append("")

    if show_all and buckets["other"]:
        lines.append("### Other")
        lines.append("")
        for commit in buckets["other"]:
            lines.append(format_bullet(commit, base_url))
        lines.append("")

    if compare and base_url:
        lines.append(f"**Full Changelog**: {base_url}/compare/{compare}")
        lines.append("")

    text = "\n".join(lines).rstrip() + "\n"
    if not any(buckets.values()):
        text = (f"## {version} — {date}\n\nNo user-facing changes.\n" if header
                else "No user-facing changes.\n")
    return text


def _capitalize(text: str) -> str:
    """Capitalize the first character, leaving the rest untouched."""
    if not text:
        return text
    return text[0].upper() + text[1:]


def format_bullet(commit: Commit, base_url: str | None) -> str:
    subject = commit.subject
    # Strip a redundant type prefix the author may have repeated in the subject.
    subject = re.sub(rf"^{re.escape(commit.type)}\s*:\s*", "", subject, flags=re.IGNORECASE)
    subject = _capitalize(subject)

    if commit.scope:
        subject = f"**{_capitalize(commit.scope)}:** {subject}"

    if base_url and commit.sha:
        return f"- {subject} ([{commit.short}]({base_url}/commit/{commit.sha}))"
    if commit.sha:
        return f"- {subject} ({commit.short})"
    return f"- {subject}"


# --- entry point ----------------------------------------------------------

def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        description="Generate release notes from Conventional Commits.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument("--repo", default=".", help="path to the git repository (default: .)")
    parser.add_argument("--from", dest="from_ref", help="start ref, excluded (default: last tag)")
    parser.add_argument("--to", dest="to_ref", default="HEAD", help="end ref (default: HEAD)")
    parser.add_argument("--version", help="version heading (default: mod_version, else Unreleased)")
    parser.add_argument("--date", help="date for the heading (default: today)")
    parser.add_argument("--output", help="write to this file instead of stdout")
    parser.add_argument("--github", action="store_true",
                        help="emit the release body only, without the version heading")
    parser.add_argument("--all", action="store_true",
                        help="also list chore, style, test and ci commits")
    parser.add_argument("--no-links", action="store_true", help="omit commit links")
    args = parser.parse_args(argv)

    repo = os.path.abspath(args.repo)
    base_url = None if args.no_links else repo_url(repo)

    from_ref = args.from_ref if args.from_ref is not None else last_tag(repo)
    revision_range = f"{from_ref}..{args.to_ref}" if from_ref else args.to_ref

    commits = collect_commits(repo, revision_range)

    version = args.version or read_gradle_version(repo) or "Unreleased"
    date = args.date or dt.date.today().isoformat()
    compare = f"{from_ref}...{args.to_ref}" if from_ref else None

    text = render(
        commits,
        version=version,
        date=date,
        base_url=base_url,
        show_all=args.all,
        header=not args.github,
        compare=compare,
    )

    if args.output:
        os.makedirs(os.path.dirname(os.path.abspath(args.output)), exist_ok=True)
        with open(args.output, "w", encoding="utf-8") as handle:
            handle.write(text)
        print(f"Wrote {args.output} ({len(commits)} commits, {from_ref or 'start'}..{args.to_ref})")
    else:
        sys.stdout.write(text)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
