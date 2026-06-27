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
import json
import os
import sys
from pathlib import Path

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
    p.add_argument("--json", type=Path, default=None, metavar="PATH",
                   help="dump the raw estimate as JSON to PATH and exit")
    p.add_argument("--compare", type=Path, default=None, metavar="PATH",
                   help="baseline JSON (from --json) to render deltas against")
    p.add_argument("--md", type=Path, default=None, metavar="PATH",
                   help="also write a Markdown report to PATH")
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


def collect(root):
    """Scan a source root, returning (per-area stripped chars, total, file count)."""
    areas, total_chars, files = {}, 0, 0
    for p in root.rglob("*.java"):
        code = strip_comments(p.read_text(encoding="utf-8"))
        chars = len(code)
        total_chars += chars
        files += 1
        area = area_of(p.relative_to(root))
        areas[area] = areas.get(area, 0) + chars
    return areas, total_chars, files


def signed(n):
    """Render a delta with an explicit sign: +1,234 / -567 / ±0."""
    return f"{n:+,}" if n else "±0"


def main(args):
    areas, total_chars, files = collect(args.root)
    est = round(total_chars / args.ratio)
    pct = est / args.limit * 100

    if args.json:
        args.json.write_text(json.dumps({
            "est": est, "files": files, "total_chars": total_chars,
            "ratio": args.ratio, "areas": areas,
        }))
        # --json is a pure data dump to capture a baseline for later --compare.
        # It writes nothing else.
        return 0

    base = json.loads(args.compare.read_text()) if args.compare else None
    base_areas = base["areas"] if base else {}
    # Measure the baseline with the current ratio so a ratio change never
    # shows up as a phantom delta.
    base_est = round(base["total_chars"] / args.ratio) if base else None
    delta = est - base_est if base else None

    def tok(ch):
        return round(ch / args.ratio)

    if base is not None:
        # Compare mode: only surface areas that actually moved, largest first.
        dmap = {a: tok(areas.get(a, 0)) - tok(base_areas.get(a, 0))
                for a in set(areas) | set(base_areas)}
        rows = sorted((a for a, dd in dmap.items() if dd), key=lambda a: -abs(dmap[a]))
    else:
        rows = sorted(areas, key=lambda a: -areas[a])

    # Console report
    head = f"Estimated auto-review tokens: ~{est:,} / {args.limit:,} ({pct:.0f}%)"
    if base is not None:
        head += f"   {signed(delta)} vs baseline (~{base_est:,})"
    lines = [
        head,
        f"  files: {files}",
        f"  stripped chars: {total_chars:,}",
        f"  ratio: {args.ratio} chars/tok",
        f"  warn at {args.warn:,}" +
        (f", fail at {args.fail:,}" if args.fail else ", fail disabled"),
        "",
        "  by area (changes only):" if base is not None else "  by area:",
    ]
    if base is not None and not rows:
        lines.append("    (no per-area changes)")
    for area in rows:
        row = f"    {tok(areas.get(area, 0)):>7,}  {area}"
        if base is not None:
            row += f"   {signed(dmap[area])}"
        lines.append(row)
    print("\n".join(lines))

    # Markdown (job summary and/or PR comment body)
    bar = "🟥" if est >= args.warn else "🟩"
    md = [f"### {bar} Auto-review token estimate: ~{est:,} / {args.limit:,} ({pct:.0f}%)", ""]
    if base is not None:
        arrow = "🔺" if delta > 0 else "🔻" if delta < 0 else "▬"
        md += [f"**{arrow} {signed(delta)} tokens vs base** (was ~{base_est:,})", ""]
        if rows:
            md += ["| area | est. tokens | Δ vs base |", "|---|--:|--:|"]
            for area in rows:
                md.append(f"| {area} | {tok(areas.get(area, 0)):,} | {signed(dmap[area])} |")
        else:
            md.append("_No per-area changes._")
    else:
        md += ["| area | est. tokens |", "|---|--:|"]
        for area in rows:
            md.append(f"| {area} | {tok(areas[area]):,} |")
    report_md = "\n".join(md) + "\n"

    summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary:
        with open(summary, "a") as fh:
            fh.write(report_md)
    if args.md:
        args.md.write_text(report_md)

    msg = (
        f"Plugin is ~{est:,} estimated tokens ({pct:.0f}% "
        f"of the {args.limit:,} auto-review limit)."
    )
    if args.fail and est >= args.fail:
        print(f"::error::{msg}" if IN_GHA else f"\nERROR: {msg}")
        return 1
    if est >= args.warn:
        print(f"::warning::{msg}" if IN_GHA else f"\nWARNING: {msg}")
    return 0


if __name__ == "__main__":
    sys.exit(main(parse_args()))