#!/usr/bin/env python3
"""Extract per-run simulation durations from Petri-Sim output logs.

For each input log file, a CSV is produced containing one row per run with the
columns: run_number,duration_ms. Statistics blocks at the end of the log are
ignored so only individual simulation run durations are captured.
"""

from __future__ import annotations

import argparse
import csv
import re
import sys
from pathlib import Path
from typing import Iterable, List, Tuple

RUN_START_RE = re.compile(r"^=+ Starting Run (\d+) of (\d+) =+$")
RUN_COMPLETE_RE = re.compile(r"^--- Simulation Run Complete \((\d+) ms\) ---$")


def _parse_runs(path: Path) -> Tuple[List[Tuple[int, int]], int | None]:
    """Parse a log file and return (runs, expected_total)."""

    runs: List[Tuple[int, int]] = []
    expected_total: int | None = None
    current_run: int | None = None

    with path.open("r", encoding="utf-8", errors="replace") as handle:
        for raw_line in handle:
            # Drop whitespace and stray NULs that may appear in truncated logs.
            line = raw_line.replace("\x00", "").strip()

            start_match = RUN_START_RE.match(line)
            if start_match:
                current_run = int(start_match.group(1))
                if expected_total is None:
                    expected_total = int(start_match.group(2))
                continue

            complete_match = RUN_COMPLETE_RE.match(line)
            if complete_match:
                duration_ms = int(complete_match.group(1))
                run_number = current_run if current_run is not None else len(runs) + 1
                runs.append((run_number, duration_ms))
                current_run = None

    return runs, expected_total


def _write_csv(out_path: Path, rows: Iterable[Tuple[int, int]]) -> None:
    out_path.parent.mkdir(parents=True, exist_ok=True)
    with out_path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle)
        writer.writerow(["run_number", "duration_ms"])
        writer.writerows(rows)


def parse_and_write(log_path: Path, out_dir: Path | None) -> Path:
    """Parse a single log file and write its per-run durations to CSV."""
    runs, expected_total = _parse_runs(log_path)
    if not runs:
        raise ValueError(f"No runs found in {log_path}")

    out_name = log_path.with_suffix(".csv").name
    out_path = (out_dir or log_path.parent) / out_name
    _write_csv(out_path, sorted(runs, key=lambda pair: pair[0]))

    if expected_total is not None and len(runs) != expected_total:
        print(
            f"Warning: parsed {len(runs)} runs but expected {expected_total} "
            f"from {log_path.name}",
            file=sys.stderr,
        )

    return out_path


def _build_arg_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Extract per-run durations from Petri-Sim run output logs.",
    )
    parser.add_argument(
        "logs",
        nargs="+",
        type=Path,
        help="Paths to log files to parse.",
    )
    parser.add_argument(
        "--out-dir",
        type=Path,
        default=None,
        help="Optional directory for CSV outputs (defaults to each log's directory).",
    )
    return parser


def main(argv: List[str] | None = None) -> int:
    """CLI entry point."""
    args = _build_arg_parser().parse_args(argv)
    for log_path in args.logs:
        try:
            csv_path = parse_and_write(log_path, args.out_dir)
            print(f"Wrote {csv_path}")
        except Exception as exc:  # pylint: disable=broad-except
            print(f"Failed to parse {log_path}: {exc}", file=sys.stderr)
            return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
