#!/usr/bin/env python3

"""CLI entrypoint for the T-invariant checker (presentation + I/O only)."""

from __future__ import annotations

import argparse
import sys

try:
    # Preferred path when executed as a module/package.
    from .invariant_checker_core import check_invariants
    from .invariant_checker_ui import (
        UI,
        VERSION,
        print_report,
        supports_color,
        supports_unicode,
    )
except ImportError:  # pragma: no cover
    # Fallback for direct execution: `python invariant_checker/invariant_checker.py ...`
    # pylint: disable=import-error,wrong-import-position
    import os
    import importlib

    sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))
    _core = importlib.import_module("invariant_checker.invariant_checker_core")
    _ui = importlib.import_module("invariant_checker.invariant_checker_ui")

    check_invariants = _core.check_invariants
    UI = _ui.UI
    VERSION = _ui.VERSION
    print_report = _ui.print_report
    supports_color = _ui.supports_color
    supports_unicode = _ui.supports_unicode


def _create_parser() -> argparse.ArgumentParser:
    """Create and return the CLI argument parser."""
    parser = argparse.ArgumentParser(
        description="Verify T-invariants in a log and count how many times each invariant appears.",
    )
    parser.add_argument(
        "input",
        nargs="?",
        default="-",
        help="Path to the input log file, or '-' for stdin (default).",
    )
    parser.add_argument(
        "--no-color", action="store_true", help="Disable colorized output"
    )
    parser.add_argument(
        "--no-fun", action="store_true", help="Disable emojis and banner"
    )
    parser.add_argument(
        "-q", "--quiet", action="store_true", help="Suppress non-essential output"
    )
    parser.add_argument(
        "-v", "--verbose", action="store_true", help="Show additional diagnostic output"
    )
    parser.add_argument(
        "--bar-width",
        type=int,
        default=40,
        metavar="N",
        help="Width of the count bars (default: 40)",
    )
    parser.add_argument("--version", action="store_true", help="Print version and exit")
    return parser


def _read_input(path: str) -> str:
    """Read content from the given path, or stdin if path is '-'."""
    if path == "-":
        return sys.stdin.read()
    with open(path, "r", encoding="utf-8") as f:
        return f.read()


def main():
    """
    Parses command-line arguments to verify T-invariants in a log file and count how many times
    each invariant appears.

    Arguments:
        input (str): Path to the input log file, or '-' to read from stdin.
        --no-color: Disable ANSI colors (also respected if NO_COLOR env var is set).
        --no-fun: Disable banner and emojis.
        -q/--quiet: Suppress non-essential output.
        -v/--verbose: Print extra diagnostic information.
        --version: Print version and exit.

    Reads the log file, verifies invariants, prints counts for each invariant, and displays a
    warning if leftover content remains.
    """
    parser = _create_parser()
    args = parser.parse_args()

    if args.version:
        print(f"invariant_checker v{VERSION}")
        return

    stream = sys.stdout
    use_color = (not args.no_color) and supports_color(stream)
    decorations_enabled = (not args.no_fun) and supports_unicode(stream)
    ui = UI(
        color=use_color,
        decorations_enabled=decorations_enabled,
        quiet=args.quiet,
        verbose=args.verbose,
        stream=stream,
    )

    try:
        content = _read_input(args.input)
    except FileNotFoundError:
        ui.warn("Input file not found. Please check the path.")
        sys.exit(1)
    except OSError as e:
        ui.warn(f"Could not read input: {e}")
        sys.exit(1)

    result = check_invariants(content)

    width = max(0, int(getattr(args, "bar_width", 40)))
    print_report(ui, original_text=content, result=result, bar_width=width)
    sys.exit(0)


if __name__ == "__main__":
    main()
