#!/usr/bin/env python3

"""
Module for verifying and counting T-invariants in log files using regular expressions.

This module provides functions to:
- Parse and match specific T-invariant patterns in text using a compiled
  regular expression.
- Count occurrences of three distinct invariant branches.
- Remove matched invariants from the input text and report any leftover content.

Typical usage involves reading a log file, verifying invariants, and reporting
counts and leftover content.

Functions:
    _apply_once(s: str) -> Tuple[str, int, int]:
        Finds and removes one invariant match from the string, returning the
        modified string, branch ID, and a flag indicating replacement.
    verify_invariants_text(text: str)
        -> Tuple[bool, str, Tuple[int, int, int]]:
        Repeatedly removes invariant matches, counts each branch, and returns a
        success flag, leftover text, and branch counts.
    main():
        Parses command-line arguments, reads a log file, verifies invariants,
        prints counts, and displays warnings if leftover content remains.
"""
import argparse
import os
import re
import sys
from dataclasses import dataclass
from typing import Tuple

_PATTERN = re.compile(
    r"(T0)(.*?)(T1)(.*?)(?:"
    r"(T2)(.*?)(T3)(.*?)(T4)"  # branch 1 groups: 5,6,7,8,9
    r"|"
    r"(T5)(.*?)(T6)"  # branch 2 groups: 10,11,12
    r"|"
    r"(T7)(.*?)(T8)(.*?)(T9)(.*?)(T10)"  # branch 3 groups: 13,14,15,16,17,18,19
    r")"
    r"(.*?)(T11)",  # tail groups: 20,21
    re.S,
)


# ------------------------------ UI helpers ------------------------------ #
VERSION = "1.3.0"


class Colors:  # pylint: disable=too-few-public-methods
    """ANSI color escape codes for styled terminal output.

    This class intentionally holds only constants; no public methods are needed.
    The pylint rule is disabled to avoid forcing artificial methods here.
    """

    RESET = "\033[0m"
    BOLD = "\033[1m"
    DIM = "\033[2m"
    RED = "\033[31m"
    GREEN = "\033[32m"
    YELLOW = "\033[33m"
    BLUE = "\033[34m"
    MAGENTA = "\033[35m"
    CYAN = "\033[36m"


def _supports_color(stream) -> bool:
    if not hasattr(stream, "isatty") or not stream.isatty():
        return False
    if os.environ.get("NO_COLOR"):
        return False
    # Basic Windows support when using modern terminals
    if sys.platform == "win32":
        return bool(os.environ.get("WT_SESSION") or os.environ.get("ANSICON"))
    return True


@dataclass
class UI:
    """Lightweight console UI with optional colors, banner, and verbosity."""

    color: bool = True
    fun: bool = True
    quiet: bool = False
    verbose: bool = False

    def style(self, text: str, color_code: str) -> str:
        """Apply a color style to text if colors are enabled."""
        if self.color:
            return f"{color_code}{text}{Colors.RESET}"
        return text

    def bold(self, text: str) -> str:
        """Return bold-styled text when colors are enabled."""
        return self.style(text, Colors.BOLD)

    def dim(self, text: str) -> str:
        """Return dim-styled text when colors are enabled."""
        return self.style(text, Colors.DIM)

    def green(self, text: str) -> str:
        """Return green-styled text when colors are enabled."""
        return self.style(text, Colors.GREEN)

    def yellow(self, text: str) -> str:
        """Return yellow-styled text when colors are enabled."""
        return self.style(text, Colors.YELLOW)

    def red(self, text: str) -> str:
        """Return red-styled text when colors are enabled."""
        return self.style(text, Colors.RED)

    def cyan(self, text: str) -> str:
        """Return cyan-styled text when colors are enabled."""
        return self.style(text, Colors.CYAN)

    def info(self, text: str = "") -> None:
        """Print informational text respecting the quiet flag."""
        if not self.quiet:
            print(text)

    def debug(self, text: str) -> None:
        """Print debug text when verbose and not quiet."""
        if self.verbose and not self.quiet:
            print(self.dim(text))

    def warn(self, text: str) -> None:
        """Print a warning (yellow) respecting the quiet flag."""
        if not self.quiet:
            print(self.yellow(text))

    def banner(self) -> None:
        """Print a fun banner unless disabled or in quiet mode."""
        if self.quiet or not self.fun:
            return
        logo = [
            "┌────────────────────────────────────────────────────────┐",
            "│  T‑Invariant Regex Checker                             │",
            "│  Verify and count invariants quickly and clearly       │",
            "└────────────────────────────────────────────────────────┘",
        ]
        colored = [self.cyan(self.bold(line)) for line in logo]
        print("\n".join(colored))


def _apply_once(s: str) -> Tuple[str, int, int]:
    """
    Find ONE match, count the specific branch, and replace the match with the concatenation
    of only the 'gap' groups, preserving the rest of the string.
    Returns (new_string, branch_id, did_replace[0/1]).
      branch_id: 1,2,3 for which invariant matched (0 if none).
    """
    m = _PATTERN.search(s)
    if not m:
        return s, 0, 0

    # Which branch matched?
    if m.group(9) is not None:  # has T4 => branch 1 (T2..T3..T4)
        branch = 1
    elif m.group(12) is not None:  # has T6 => branch 2 (T5..T6)
        branch = 2
    elif m.group(19) is not None:  # has T10 => branch 3 (T7..T8..T9..T10)
        branch = 3
    else:
        branch = 0  # should not happen

    # Keep only the gaps: 2,4,6,8,11,14,16,18,20 (missing ones become '')
    kept = (
        (m.group(2) or "")
        + (m.group(4) or "")
        + (m.group(6) or "")
        + (m.group(8) or "")
        + (m.group(11) or "")
        + (m.group(14) or "")
        + (m.group(16) or "")
        + (m.group(18) or "")
        + (m.group(20) or "")
    )

    new_s = s[: m.start()] + kept + s[m.end() :]
    return new_s, branch, 1


def verify_invariants_text(text: str) -> Tuple[bool, str, Tuple[int, int, int]]:
    """
    Repeatedly remove invariant matches and count each branch.
    Returns (ok, leftover, (count1, count2, count3)).
    """
    # Flatten: drop newlines/spaces between tokens; keep everything else as-is.
    flat = "".join(part.strip() for part in text.splitlines())

    count1 = count2 = count3 = 0
    while True:
        flat, branch, changed = _apply_once(flat)
        if not changed:
            break
        if branch == 1:
            count1 += 1
        elif branch == 2:
            count2 += 1
        elif branch == 3:
            count3 += 1

    leftover = flat.strip()
    ok = leftover == ""
    return ok, leftover, (count1, count2, count3)


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


def _render_bar(n: int, tot: int, width: int, color_fn) -> str:
    """Render a proportional bar with optional color."""
    filled = int(round((n / tot) * width)) if tot else 0
    empty = width - filled
    return color_fn("█" * filled) + ("." * empty)


def _print_counts(ui: UI, counts: Tuple[int, int, int], width: int) -> None:
    """Print formatted counts, percentages, and bars for each branch."""
    total = sum(counts)
    emojis = ("①", "②", "③") if ui.fun else ("1", "2", "3")
    labels = (
        f"{emojis[0]} T0..T1..T2..T3..T4..T11",
        f"{emojis[1]} T0..T1..T5..T6..T11",
        f"{emojis[2]} T0..T1..T7..T8..T9..T10..T11",
    )
    max_label = max(len(l) for l in labels)
    fmt = f"  {{:<{max_label}}}  {{invariant_bar}}  {{count}}  ({{pct:.1f}}%)"

    ui.info("")
    ui.info(ui.bold("Counts:"))
    for label, n in zip(labels, counts):
        pct = (n * 100.0 / total) if total else 0.0
        invariant_bar = _render_bar(n, total, width, ui.green)
        ui.info(
            fmt.format(
                label, invariant_bar=invariant_bar, count=ui.green(str(n)), pct=pct
            )
        )
    ui.info("")
    ui.info(ui.bold("Total invariants matched: ") + ui.green(str(total)))


def _print_diagnostics(ui: UI, content: str, leftover: str) -> None:
    """Print optional verbose diagnostic details about consumption and tokens."""
    if not ui.verbose or ui.quiet:
        return
    flattened = "".join(part.strip() for part in content.splitlines())
    flat_len = len(flattened)
    left_len = len(leftover)
    ratio = (1.0 - (left_len / flat_len)) if flat_len else 1.0
    tok_in = len(re.findall(r"T\d+", content))
    tok_left = len(re.findall(r"T\d+", leftover))
    ui.info("")
    ui.debug(
        f"Flat length: {flat_len}, leftover length: {left_len}, consumed: {ratio:.1%}"
    )
    ui.debug(f"Token occurrences in input: {tok_in}, in leftover: {tok_left}")


DEFAULT_PREVIEW_LIMIT = 200


def _preview_leftover(
    ui: UI, leftover: str, limit: int = DEFAULT_PREVIEW_LIMIT
) -> None:
    """Show a preview of leftover content with highlighted tokens if colored."""
    ui.info("")
    ui.warn("WARNING: leftover content not consumed.")
    if limit <= 0:
        preview = ""
    elif len(leftover) <= limit:
        preview = leftover
    else:
        head = leftover[: max(1, limit // 2)]
        tail = leftover[-max(1, limit - len(head)) :]
        preview = head + " … " + tail
    if ui.color:
        preview = re.sub(r"(T\d+)", lambda m: ui.bold(ui.red(m.group(1))), preview)
    ui.info("")
    ui.info(preview)


def main():
    """
    Parses command-line arguments to verify T-invariants in a log file and count how many times each
    invariant appears.

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

    use_color = (not args.no_color) and _supports_color(sys.stdout)
    use_fun = (not args.no_fun) and use_color
    ui = UI(color=use_color, fun=use_fun, quiet=args.quiet, verbose=args.verbose)

    ui.banner()

    try:
        content = _read_input(args.input)
    except FileNotFoundError:
        ui.warn("Input file not found. Please check the path.")
        sys.exit(1)
    except OSError as e:
        ui.warn(f"Could not read input: {e}")
        sys.exit(1)

    ok, leftover, (c1, c2, c3) = verify_invariants_text(content)

    width = max(0, int(getattr(args, "bar_width", 40)))
    _print_counts(ui, (c1, c2, c3), width)

    _print_diagnostics(ui, content, leftover)

    if ok:
        msg = "All invariants consumed."
        if ui.fun:
            msg += " ✨"
        ui.info("")
        ui.info(ui.green(msg))
        sys.exit(0)
    else:
        _preview_leftover(ui, leftover)
        sys.exit(0)


if __name__ == "__main__":
    main()
