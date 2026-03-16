#!/usr/bin/env python3
"""Delete artifacts listed in a scan result CSV file.

Reads CSV rows one at a time to avoid loading everything into memory.
"""

import argparse
import csv
import os
import sys
import urllib.error
from concurrent.futures import ThreadPoolExecutor, as_completed

from common import (
    DEFAULT_URL, DEFAULT_WORKERS,
    Stats, delete_path, log, set_stats, setup_logging,
)


def _delete_task(base_url, repo, path, token, stats):
    try:
        delete_path(base_url, repo, path, token)
        stats.add_deleted()
    except urllib.error.HTTPError as e:
        log.error("Failed to delete %s/%s: HTTP %d", repo, path, e.code)
        stats.add_error()


def main():
    p = argparse.ArgumentParser(description="Delete artifacts from scan result")
    p.add_argument("--url", default=DEFAULT_URL)
    p.add_argument("--token", default=os.environ.get("REPOSILITE_TOKEN"))
    p.add_argument("--repo", required=True)
    p.add_argument("--workers", type=int, default=DEFAULT_WORKERS)
    p.add_argument("--input", default="scan_result.csv")
    args = p.parse_args()

    setup_logging()

    if not args.token:
        log.error("--token or REPOSILITE_TOKEN is required")
        sys.exit(1)

    stats = Stats()
    set_stats(stats)
    stats.start_progress_timer(args.repo)

    with ThreadPoolExecutor(max_workers=args.workers) as executor:
        futures = []
        with open(args.input, newline="") as f:
            reader = csv.DictReader(f)
            for row in reader:
                log.info("DELETE %s %s/%s", row["type"], args.repo, row["path"])
                futures.append(executor.submit(
                    _delete_task, args.url, args.repo, row["path"], args.token, stats))
        for future in as_completed(futures):
            future.result()

    stats.stop_progress_timer()
    stats.log_summary(args.repo)


if __name__ == "__main__":
    main()
