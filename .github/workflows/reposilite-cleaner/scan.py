#!/usr/bin/env python3
"""Scan repository and output optimized delete list as CSV.

Performs a full scan to build per-directory metadata in memory, then
writes an optimized delete list to the output file. If all files in a
directory are old, emits a single directory delete instead of individual
file deletes.

Output CSV columns: type,path
"""

import argparse
import csv
import os
import sys
import time
from collections import defaultdict
from concurrent.futures import ThreadPoolExecutor, as_completed

from common import (
    DEFAULT_URL, DEFAULT_WORKERS,
    Stats, is_excluded, list_details,
    load_exclude_file, log, set_stats, setup_logging,
)

DEFAULT_EXCLUDE_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "exclude.txt")
DEFAULT_MAX_AGE_DAYS = 30

def scan(base_url, repo, start_path, token, cutoff_ts, stats, exclude_paths, executor):
    """BFS scan. Returns per-directory metadata for optimization."""
    dirs_to_scan = [start_path]
    dir_info = defaultdict(lambda: {"total_files": 0, "old_files": 0, "old_file_paths": [], "subdirs": [], "has_excluded": False})

    while dirs_to_scan:
        listing_futures = {
            executor.submit(list_details, base_url, repo, d, token): d
            for d in dirs_to_scan
        }
        dirs_to_scan = []

        for future in as_completed(listing_futures):
            parent = listing_futures[future]
            details = future.result()
            if details is None:
                continue

            for entry in details.get("files", []):
                name = entry["name"]
                entry_path = f"{parent}/{name}" if parent else name

                if is_excluded(repo, entry_path, exclude_paths):
                    log.info("SKIP (excluded): %s/%s", repo, entry_path)
                    stats.add_skipped()
                    dir_info[parent]["has_excluded"] = True
                    continue

                if entry["type"] == "DIRECTORY":
                    dirs_to_scan.append(entry_path)
                    dir_info[parent]["subdirs"].append(entry_path)
                    _ = dir_info[entry_path]
                elif entry["type"] == "FILE":
                    size = entry.get("contentLength", 0)
                    stats.add_scanned(size)
                    dir_info[parent]["total_files"] += 1

                    if entry.get("lastModifiedTime", 0) < cutoff_ts:
                        stats.add_old(size)
                        dir_info[parent]["old_files"] += 1
                        dir_info[parent]["old_file_paths"].append(entry_path)

    return dict(dir_info)


def is_fully_deletable(path, dir_info, cache):
    if path in cache:
        return cache[path]

    info = dir_info.get(path)
    if info is None:
        cache[path] = False
        return False

    if info["has_excluded"]:
        cache[path] = False
        return False

    if info["old_files"] < info["total_files"]:
        cache[path] = False
        return False

    for sub in info["subdirs"]:
        if not is_fully_deletable(sub, dir_info, cache):
            cache[path] = False
            return False

    has_content = info["total_files"] > 0 or any(
        is_fully_deletable(s, dir_info, cache) for s in info["subdirs"]
    )
    cache[path] = has_content
    return has_content


def will_be_empty(path, dir_info, deletable_cache):
    """Check if a dir will be empty after delete phase (but isn't fully deletable itself)."""
    info = dir_info.get(path)
    if info is None:
        return False
    # All direct files must be old
    if info["old_files"] < info["total_files"]:
        return False
    # All subdirs must be either fully deletable or will themselves be empty
    for sub in info["subdirs"]:
        if not is_fully_deletable(sub, dir_info, deletable_cache) and not will_be_empty(sub, dir_info, deletable_cache):
            return False
    return True


def find_cleanup_dirs(start_path, dir_info, deletable_cache):
    """Find dirs that will be empty after delete phase but aren't in the delete list.

    Returns paths deepest-first so they can be deleted in order.
    """
    result = []

    def walk(path):
        # If fully deletable, it's already in the delete list — skip it and its children
        if is_fully_deletable(path, dir_info, deletable_cache):
            return

        info = dir_info.get(path)
        if info is None:
            return

        # Recurse into subdirs first (we want deepest-first)
        for sub in info["subdirs"]:
            walk(sub)

        # Check if this dir will be empty after deletions
        if will_be_empty(path, dir_info, deletable_cache):
            result.append(path)

    info = dir_info.get(start_path, dir_info.get(""))
    if info:
        for sub in info.get("subdirs", []):
            walk(sub)

    return result


def write_delete_list(start_path, dir_info, cache, writer, stats):
    """Write optimized CSV rows based on precomputed directory metadata."""

    def walk(path):
        if is_fully_deletable(path, dir_info, cache):
            writer.writerow(("dir", path))
            stats.add_delete_dir()
            return

        info = dir_info.get(path)
        if info is None:
            return

        for file_path in info["old_file_paths"]:
            writer.writerow(("file", file_path))
            stats.add_delete_file()

        for sub in info["subdirs"]:
            walk(sub)

    # If the starting path itself (and everything under it) is fully deletable,
    # emit a single directory delete entry for it. For an empty start_path (the
    # repository root), preserve existing behavior and do not emit a root dir
    # delete; instead, fall back to listing its contents.
    if start_path and is_fully_deletable(start_path, dir_info, cache):
        writer.writerow(("dir", start_path))
        stats.add_delete_dir()
        return

    info = dir_info.get(start_path, dir_info.get(""))
    if info:
        for file_path in info.get("old_file_paths", []):
            writer.writerow(("file", file_path))
            stats.add_delete_file()
        for sub in info.get("subdirs", []):
            walk(sub)


def main():
    p = argparse.ArgumentParser(description="Scan Reposilite and list old artifacts")
    p.add_argument("--url", default=DEFAULT_URL)
    p.add_argument("--token", default=os.environ.get("REPOSILITE_TOKEN"))
    p.add_argument("--repo", required=True)
    p.add_argument("--max-age-days", type=int, default=DEFAULT_MAX_AGE_DAYS)
    p.add_argument("--workers", type=int, default=DEFAULT_WORKERS)
    p.add_argument("--path", default="")
    p.add_argument("--exclude-file", default=DEFAULT_EXCLUDE_FILE)
    p.add_argument("--output", default="scan_result.csv")
    p.add_argument("--cleanup-output", default="cleanup_dirs.csv")
    args = p.parse_args()

    setup_logging()

    if not args.token:
        log.error("--token or REPOSILITE_TOKEN is required")
        sys.exit(1)

    cutoff = time.time() - args.max_age_days * 86400
    exclude = load_exclude_file(args.exclude_file)

    log.info("scan phase | url=%s repo=%s max_age=%dd workers=%d",
             args.url, args.repo, args.max_age_days, args.workers)

    with ThreadPoolExecutor(max_workers=args.workers) as executor:
        stats = Stats()
        set_stats(stats)
        stats.start_progress_timer(args.repo)
        dir_info = scan(args.url, args.repo, args.path, args.token, cutoff, stats, exclude, executor)
        stats.stop_progress_timer()
        stats.log_summary(args.repo)

    cache = {}
    with open(args.output, "w", newline="") as f:
        writer = csv.writer(f)
        writer.writerow(("type", "path"))
        write_delete_list(args.path, dir_info, cache, writer, stats)

    log.info("To delete: %d dirs, %d files", stats.delete_dirs, stats.delete_files)

    cleanup_dirs = find_cleanup_dirs(args.path, dir_info, cache)
    with open(args.cleanup_output, "w", newline="") as f:
        writer = csv.writer(f)
        writer.writerow(("path",))
        for d in cleanup_dirs:
            writer.writerow((d,))

    log.info("Cleanup dirs: %d (written to %s)", len(cleanup_dirs), args.cleanup_output)


if __name__ == "__main__":
    main()
