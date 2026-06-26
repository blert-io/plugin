#!/usr/bin/env python3
# Estimates the token count RuneLite's auto-review sees for this plugin.
#
# RuneLite counts only the main source set, with comments, unit tests, and
# resource files excluded, against a token limit. This script strips comments
# and estimates tokens from a calibrated chars/token ratio, so it needs no
# third-party dependencies.
#
# The ratio was calibrated on 2026-06-26 against OpenAI's o200k/cl100k
# tokenizers (4.35 chars/token for this codebase's stripped Java).

import argparse
from pathlib import Path
import os
import sys

IN_GHA = os.environ.get("GITHUB_ACTIONS") == "true"


def parse_args():
    p = argparse.ArgumentParser(
        description="Estimate the RuneLite-facing token count of the plugin.")

    p.add_argument("root", nargs="?", type=Path,
                   default=Path("src/main/java"),
                   help="source root to scan")
    p.add_argument("--limit", type=int, default=int(os.environ.get("BLERT_TOKEN_LIMIT", "200000")),
                   help="token ceiling the estimate is measured against")
    p.add_argument("--warn", type=int, default=int(os.environ.get("BLERT_TOKEN_WARN", "160000")),
                   help="emit a warning at or above this estimate")
    p.add_argument("--fail", type=int, default=int(os.environ.get("BLERT_TOKEN_FAIL", "0")),
                   help="fail at or above this estimate; disabled by default")
    p.add_argument("--ratio", type=float, default=float(os.environ.get("BLERT_CHARS_PER_TOKEN", "4.35")),
                   help="chars-per-token calibration ratio (default: 4.35)")
    return p.parse_args()


def strip_comments(s):
    out = []
    i, n, st = 0, len(s), "code"
    while i < n:
        c = s[i]
        d = s[i + 1] if i + 1 < n else ""
        if st == "code":
            if c == "/" and d == "/":
                st = "line"; i += 2; continue
            if c == "/" and d == "*":
                st = "block"; i += 2; continue
            if c == '"':
                out.append(c); st = "str"; i += 1; continue
            if c == "'":
                out.append(c); st = "char"; i += 1; continue
            out.append(c); i += 1
        elif st == "line":
            if c == "\n":
                out.append(c); st = "code"
            i += 1
        elif st == "block":
            if c == "*" and d == "/":
                st = "code"; i += 2; continue
            if c == "\n":
                out.append(c)
            i += 1
        else:  # str or char
            out.append(c)
            if c == "\\":
                out.append(d); i += 2; continue
            if (st == "str" and c == '"') or (st == "char" and c == "'"):
                st = "code"
            i += 1
    # drop blank lines left behind by stripped comments
    return "\n".join(l for l in "".join(out).splitlines() if l.strip())


def area_of(rel):
    parts = rel.parts
    if parts[:2] == ("io", "blert") and len(parts) > 3:
        if parts[2] == "challenges":
            return "challenges/" + parts[3]
        return parts[2]
    return "io/blert"


def main(args):
    areas, total_chars, files = {}, 0, 0
    for p in args.root.rglob("*.java"):
        code = strip_comments(p.read_text(encoding="utf-8"))
        chars = len(code)
        total_chars += chars
        files += 1
        area = area_of(p.relative_to(args.root))
        areas[area] = areas.get(area, 0) + chars

    est = round(total_chars / args.ratio)
    pct = est / args.limit * 100

    lines = [
        f"Estimated auto-review tokens: ~{est:,} / {args.limit:,} ({pct:.0f}%)",
        f"  files: {files}",
        f"  stripped chars: {total_chars:,}",
        f"  ratio: {args.ratio} chars/tok",
        f"  warn at {args.warn:,}" +
        (f", fail at {args.fail:,}" if args.fail else ", fail disabled"),
        "",
        "  by area:",
    ]
    for area, ch in sorted(areas.items(), key=lambda x: -x[1]):
        lines.append(f"    {round(ch / args.ratio):>7,}  {area}")
    report = "\n".join(lines)
    print(report)

    summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary:
        with open(summary, "a") as fh:
            bar = "🟥" if est >= args.warn else "🟩"
            fh.write(f"### {bar} Auto-review token estimate: ~"
                     f"{est:,} / {args.limit:,} ({pct:.0f}%)\n\n")
            fh.write("| area | est. tokens |\n|---|--:|\n")
            for area, ch in sorted(areas.items(), key=lambda x: -x[1]):
                fh.write(f"| {area} | {round(ch / args.ratio):,} |\n")

    msg = (
        f"Plugin is ~{est:,} estimated tokens ({pct:.0f}% "
        "of the {args.limit} auto-review limit)."
    )
    if args.fail and est >= args.fail:
        print(f"::error::{msg}" if IN_GHA else f"\nERROR: {msg}")
        return 1
    if est >= args.warn:
        print(f"::warning::{msg}" if IN_GHA else f"\nWARNING: {msg}")
    return 0


if __name__ == "__main__":
    sys.exit(main(parse_args()))