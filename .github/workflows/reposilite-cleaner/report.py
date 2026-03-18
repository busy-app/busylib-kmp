#!/usr/bin/env python3
"""Generate a human-readable Markdown report from scan and cleanup CSVs."""

import argparse
import csv
import os
import sys
from collections import defaultdict


def read_scan_csv(path):
    """Read scan_result.csv and return counts and top-level path groups."""
    files = []
    dirs = []
    if not os.path.isfile(path):
        return files, dirs
    with open(path, newline="") as f:
        reader = csv.DictReader(f)
        for row in reader:
            entry_type = row["type"]
            entry_path = row["path"]
            if entry_type == "file":
                files.append(entry_path)
            elif entry_type == "dir":
                dirs.append(entry_path)
    return files, dirs


def read_cleanup_csv(path):
    """Read cleanup_dirs.csv and return list of directory paths."""
    result = []
    if not os.path.isfile(path):
        return result
    with open(path, newline="") as f:
        reader = csv.DictReader(f)
        for row in reader:
            result.append(row["path"])
    return result


def group_by_top_level(paths):
    """Group paths by their top-level directory component."""
    groups = defaultdict(list)
    for p in paths:
        parts = p.split("/")
        top = parts[0] if parts else p
        groups[top].append(p)
    return dict(groups)


def generate_report(repos, artifacts_dir):
    """Generate Markdown report string.

    Expects artifacts laid out as:
      <artifacts_dir>/scan-<repo>/scan_result.csv
      <artifacts_dir>/cleanup-<repo>/cleanup_dirs.csv
    """
    lines = []
    lines.append("# 🧹 Reposilite Cleanup Report\n")

    total_del_files = 0
    total_del_dirs = 0
    total_cleanup_dirs = 0

    repo_data = {}
    for repo in repos:
        scan_path = os.path.join(artifacts_dir, f"scan-{repo}", "scan_result.csv")
        cleanup_path = os.path.join(artifacts_dir, f"cleanup-{repo}", "cleanup_dirs.csv")

        del_files, del_dirs = read_scan_csv(scan_path)
        cleanup_dirs = read_cleanup_csv(cleanup_path)

        total_del_files += len(del_files)
        total_del_dirs += len(del_dirs)
        total_cleanup_dirs += len(cleanup_dirs)

        repo_data[repo] = {
            "del_files": del_files,
            "del_dirs": del_dirs,
            "cleanup_dirs": cleanup_dirs,
        }

    # Overall summary
    lines.append("## Summary\n")
    lines.append("| Metric | Count |")
    lines.append("|---|---|")
    lines.append(f"| Files deleted | {total_del_files} |")
    lines.append(f"| Directories deleted | {total_del_dirs} |")
    lines.append(f"| Empty directories cleaned up | {total_cleanup_dirs} |")
    lines.append(f"| **Total operations** | **{total_del_files + total_del_dirs + total_cleanup_dirs}** |")
    lines.append("")

    # Per-repo details
    for repo in repos:
        data = repo_data[repo]
        del_files = data["del_files"]
        del_dirs = data["del_dirs"]
        cleanup_dirs = data["cleanup_dirs"]

        lines.append(f"## Repository: `{repo}`\n")

        if not del_files and not del_dirs and not cleanup_dirs:
            lines.append("✅ Nothing to clean up.\n")
            continue

        lines.append(f"- **{len(del_files)}** file(s) deleted")
        lines.append(f"- **{len(del_dirs)}** directory(ies) deleted")
        lines.append(f"- **{len(cleanup_dirs)}** empty directory(ies) cleaned up")
        lines.append("")

        # Group deleted dirs by top-level
        if del_dirs:
            lines.append("### Deleted directories\n")
            grouped = group_by_top_level(del_dirs)
            for top in sorted(grouped):
                paths = grouped[top]
                if len(paths) <= 5:
                    for p in sorted(paths):
                        lines.append(f"- `{p}`")
                else:
                    for p in sorted(paths)[:3]:
                        lines.append(f"- `{p}`")
                    lines.append(f"- … and {len(paths) - 3} more under `{top}/`")
            lines.append("")

        # Group deleted files by top-level
        if del_files:
            lines.append("### Deleted files\n")
            grouped = group_by_top_level(del_files)
            for top in sorted(grouped):
                paths = grouped[top]
                lines.append(f"- `{top}/` — {len(paths)} file(s)")
            lines.append("")

        # Cleanup dirs
        if cleanup_dirs:
            lines.append("### Cleaned up empty directories\n")
            if len(cleanup_dirs) <= 10:
                for p in cleanup_dirs:
                    lines.append(f"- `{p}`")
            else:
                for p in cleanup_dirs[:7]:
                    lines.append(f"- `{p}`")
                lines.append(f"- … and {len(cleanup_dirs) - 7} more")
            lines.append("")

    return "\n".join(lines)


def main():
    p = argparse.ArgumentParser(description="Generate cleanup report")
    # Default repos must match the matrix in reposilite-clean.yml
    p.add_argument("--repos", nargs="+", default=["releases", "snapshots"],
                   help="Repository names to report on (must match workflow matrix)")
    p.add_argument("--artifacts-dir", default=".",
                   help="Directory containing downloaded artifacts")
    p.add_argument("--output", default=None,
                   help="Output file (default: stdout)")
    args = p.parse_args()

    report = generate_report(args.repos, args.artifacts_dir)

    if args.output:
        with open(args.output, "w") as f:
            f.write(report)
    else:
        print(report)


if __name__ == "__main__":
    main()
