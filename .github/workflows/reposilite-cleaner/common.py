import base64
import json
import logging
import os
import ssl
import threading
import time
import urllib.request
import urllib.error

DEFAULT_URL = "https://reposilite.flipp.dev"
DEFAULT_WORKERS = 8
PROGRESS_INTERVAL = 60

log = logging.getLogger("reposilite-cleaner")
_stats_ref = None


def setup_logging():
    fmt = logging.Formatter("%(asctime)s %(levelname)s %(message)s", datefmt="%Y-%m-%d %H:%M:%S")
    handler = logging.StreamHandler()
    handler.setFormatter(fmt)
    log.addHandler(handler)
    log.setLevel(logging.INFO)


def set_stats(stats):
    global _stats_ref
    _stats_ref = stats


def load_exclude_file(path):
    if not os.path.isfile(path):
        return set()
    with open(path) as f:
        return {line.strip() for line in f if line.strip() and not line.startswith("#")}


def make_request(url, method="GET", token=None):
    req = urllib.request.Request(url, method=method)
    req.add_header("User-Agent", "reposilite-cleaner/1.0")
    if token:
        token = token.strip()
        if method == "DELETE":
            credentials = base64.b64encode(token.encode()).decode()
            req.add_header("Authorization", "Basic " + credentials)
        else:
            req.add_header("Authorization", "Bearer " + token)
    ctx = ssl.create_default_context()
    if _stats_ref:
        _stats_ref.add_request()
    try:
        with urllib.request.urlopen(req, context=ctx) as resp:
            if method == "GET":
                return json.loads(resp.read().decode())
            return resp.status
    except urllib.error.HTTPError as e:
        if method == "GET":
            log.warning("HTTP %d fetching %s", e.code, url)
            return None
        raise


def list_details(base_url, repository, path="", token=None):
    url = f"{base_url}/api/maven/details/{repository}"
    if path:
        url += f"/{path}"
    return make_request(url, token=token)


def delete_path(base_url, repository, path, token=None):
    url = f"{base_url}/{repository}/{path}"
    return make_request(url, method="DELETE", token=token)


def is_excluded(repository, entry_path, exclude_paths):
    return f"{repository}/{entry_path}" in exclude_paths


def fmt_size(n):
    for unit in ("B", "KB", "MB", "GB"):
        if abs(n) < 1024:
            return f"{n:.1f} {unit}"
        n /= 1024
    return f"{n:.1f} TB"


class Stats:
    def __init__(self):
        self._lock = threading.Lock()
        self.requests = 0
        self.total_files = 0
        self.total_size = 0
        self.old_files = 0
        self.old_size = 0
        self.deleted = 0
        self.errors = 0
        self.skipped = 0
        self.empty_dirs = 0
        self.delete_dirs = 0
        self.delete_files = 0
        self.start_time = time.time()

    def _inc(self, attr, val=1):
        with self._lock:
            setattr(self, attr, getattr(self, attr) + val)

    def add_request(self):     self._inc("requests")
    def add_deleted(self):     self._inc("deleted")
    def add_error(self):       self._inc("errors")
    def add_skipped(self):     self._inc("skipped")
    def add_empty_dir(self):   self._inc("empty_dirs")
    def add_delete_dir(self):  self._inc("delete_dirs")
    def add_delete_file(self): self._inc("delete_files")

    def add_scanned(self, size):
        with self._lock:
            self.total_files += 1
            self.total_size += size

    def add_old(self, size):
        with self._lock:
            self.old_files += 1
            self.old_size += size

    def log_progress(self, label):
        with self._lock:
            elapsed = int(time.time() - self.start_time)
            log.info(
                "PROGRESS [%s] %dm%02ds elapsed | requests: %d | scanned: %d files (%s) | old: %d | deleted: %d | errors: %d",
                label, elapsed // 60, elapsed % 60,
                self.requests, self.total_files, fmt_size(self.total_size),
                self.old_files, self.deleted, self.errors,
            )

    def start_progress_timer(self, label):
        self._stop = threading.Event()
        def run():
            while not self._stop.wait(PROGRESS_INTERVAL):
                self.log_progress(label)
        self._timer = threading.Thread(target=run, daemon=True)
        self._timer.start()

    def stop_progress_timer(self):
        self._stop.set()
        self._timer.join()

    def log_summary(self, label):
        log.info("%s summary:", label)
        log.info("  HTTP requests:       %d", self.requests)
        log.info("  Total files scanned: %d (%s)", self.total_files, fmt_size(self.total_size))
        log.info("  Old files found:     %d (%s)", self.old_files, fmt_size(self.old_size))
        log.info("  Deleted:             %d", self.deleted)
        log.info("  Errors:              %d", self.errors)
        log.info("  Skipped (excluded):  %d", self.skipped)
        log.info("  To delete (dirs):    %d", self.delete_dirs)
        log.info("  To delete (files):   %d", self.delete_files)
        log.info("  Empty dirs removed:  %d", self.empty_dirs)
