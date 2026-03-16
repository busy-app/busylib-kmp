#!/usr/bin/env python3
"""Delete empty directories from a list produced by scan."""

import argparse
import csv
import os
import sys

from common import (
    DEFAULT_URL, DEFAULT_WORKERS,
    Stats, delete_path, list_details,
    log, set_stats, setup_logging,
)


def main():
    p = argparse.ArgumentParser(description="Remove empty directories from Reposilite")
    p.add_argument("--url", default=DEFAULT_URL)
    p.add_argument("--token", default=os.environ.get("REPOSILITE_TOKEN"))
    p.add_argument("--repo", required=True)
    p.add_argument("--input", default="cleanup_dirs.csv")
    args = p.parse_args()

    setup_logging()

    if not args.token:
        log.error("--token or REPOSILITE_TOKEN is required")
        sys.exit(1)

    stats = Stats()
    set_stats(stats)
    stats.start_progress_timer(args.repo)

    with open(args.input, newline="") as f:
        reader = csv.DictReader(f)
        for row in reader:
            dir_path = row["path"]
            # Verify actually empty before deleting
            details = list_details(args.url, args.repo, dir_path, args.token)
            if details is None:
                continue
            remaining = details.get("files", None)
            if remaining is not None and len(remaining) == 0:
                stats.add_empty_dir()
                log.info("DELETE empty dir: %s/%s", args.repo, dir_path)
                try:
                    delete_path(args.url, args.repo, dir_path, args.token)
                except Exception as e:
                    log.error("Failed to delete dir %s/%s: %s", args.repo, dir_path, e)

    stats.stop_progress_timer()
    stats.log_summary(args.repo)


if __name__ == "__main__":
    main()
