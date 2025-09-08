#!/usr/bin/env python3

"""
Module for verifying and counting T-invariants in log files using regular expressions.

This module provides functions to:
- Parse and match specific T-invariant patterns in text using a compiled
  regular expression.
- Count occurrences of three distinct invariant branches.
- Remove matched invariants from the input text and report any leftover content.
- Optionally strip dashes from the input before final verification.

Typical usage involves reading a log file, verifying invariants, and reporting
counts and leftover content.

Functions:
    _apply_once(s: str) -> Tuple[str, int, int]:
        Finds and removes one invariant match from the string, returning the
        modified string, branch ID, and a flag indicating replacement.
    verify_invariants_text(text: str, strip_dashes: bool = True)
        -> Tuple[bool, str, Tuple[int, int, int]]:
        Repeatedly removes invariant matches, counts each branch, and returns a
        success flag, leftover text, and branch counts.
    main():
        Parses command-line arguments, reads a log file, verifies invariants,
        prints counts, and displays warnings if leftover content remains.
"""
import argparse
import json
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
VERSION = "1.2.0"


class Colors:
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
    color: bool = True
    fun: bool = True
    quiet: bool = False
    verbose: bool = False

    def style(self, text: str, color_code: str) -> str:
        if self.color:
            return f"{color_code}{text}{Colors.RESET}"
        return text

    def bold(self, text: str) -> str:
        return self.style(text, Colors.BOLD)

    def dim(self, text: str) -> str:
        return self.style(text, Colors.DIM)

    def green(self, text: str) -> str:
        return self.style(text, Colors.GREEN)

    def yellow(self, text: str) -> str:
        return self.style(text, Colors.YELLOW)

    def red(self, text: str) -> str:
        return self.style(text, Colors.RED)

    def cyan(self, text: str) -> str:
        return self.style(text, Colors.CYAN)

    def info(self, text: str = "") -> None:
        if not self.quiet:
            print(text)

    def debug(self, text: str) -> None:
        if self.verbose and not self.quiet:
            print(self.dim(text))

    def warn(self, text: str) -> None:
        if not self.quiet:
            print(self.yellow(text))

    def banner(self) -> None:
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


def verify_invariants_text(
    text: str, strip_dashes: bool = True
) -> Tuple[bool, str, Tuple[int, int, int]]:
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

    if strip_dashes:
        flat = re.sub(r"-", "", flat)

    leftover = flat.strip()
    ok = leftover == ""
    return ok, leftover, (count1, count2, count3)


def main():
    """
    Parses command-line arguments to verify T-invariants in a log file and count how many times each
    invariant appears.

    Arguments:
        input (str): Path to the input log file, or '-' to read from stdin.
        --keep-dashes (bool): If set, do not strip '-' before final check (default strips '-').
        --no-color: Disable ANSI colors (also respected if NO_COLOR env var is set).
        --no-fun: Disable banner and emojis.
        -q/--quiet: Suppress non-essential output.
        -v/--verbose: Print extra diagnostic information.
        --leftover-preview N: Show up to N characters of leftover (default: 200).
        --version: Print version and exit.

    Reads the log file, verifies invariants, prints counts for each invariant, and displays a
    warning if leftover content remains.
    """
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
        "--keep-dashes",
        action="store_true",
        help="Do not strip '-' before final check (default strips '-')",
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
        "--leftover-preview",
        type=int,
        default=200,
        metavar="N",
        help="How many characters of leftover to show (default: 200)",
    )
    parser.add_argument(
        "--bar-width",
        type=int,
        default=24,
        metavar="N",
        help="Width of the count bars (default: 24)",
    )
    parser.add_argument(
        "--json",
        action="store_true",
        help="Print results as JSON (counts, total, ok, leftover) and exit",
    )
    parser.add_argument("--version", action="store_true", help="Print version and exit")
    args = parser.parse_args()

    if args.version:
        print(f"invariant_checker v{VERSION}")
        return

    use_color = (not args.no_color) and _supports_color(sys.stdout)
    use_fun = (not args.no_fun) and use_color
    ui = UI(color=use_color, fun=use_fun, quiet=args.quiet, verbose=args.verbose)

    ui.banner()

    try:
        if args.input == "-":
            content = sys.stdin.read()
        else:
            with open(args.input, "r", encoding="utf-8") as f:
                content = f.read()
    except FileNotFoundError:
        ui.warn("Input file not found. Please check the path.")
        sys.exit(1)
    except OSError as e:
        ui.warn(f"Could not read input: {e}")
        sys.exit(1)

    ok, leftover, (c1, c2, c3) = verify_invariants_text(
        content, strip_dashes=not args.keep_dashes
    )

    total = c1 + c2 + c3

    emoji1, emoji2, emoji3 = ("①", "②", "③") if ui.fun else ("1", "2", "3")

    # Optional JSON output
    if getattr(args, "json", False):
        print(
            json.dumps(
                {
                    "counts": {"branch1": c1, "branch2": c2, "branch3": c3},
                    "total": total,
                    "ok": ok,
                    "leftover": leftover,
                },
                ensure_ascii=False,
            )
        )
        sys.exit(0 if ok else 2)

    # Compute percentages and bars
    def pct(n, tot):
        return (n * 100.0 / tot) if tot else 0.0

    width = max(0, int(getattr(args, "bar_width", 24)))

    def bar(n, tot, color_fn):
        filled = int(round((n / tot) * width)) if tot else 0
        empty = width - filled
        # Use colored filled blocks and pad with spaces (or dots when no color)
        return color_fn("█" * filled) + ((" " * empty) if ui.color else ("." * empty))

    p1, p2, p3 = pct(c1, total), pct(c2, total), pct(c3, total)
    b1 = bar(c1, total, ui.green)
    b2 = bar(c2, total, ui.green)
    b3 = bar(c3, total, ui.green)

    label1 = f"{emoji1} T0..T1..T2..T3..T4..T11"
    label2 = f"{emoji2} T0..T1..T5..T6..T11"
    label3 = f"{emoji3} T0..T1..T7..T8..T9..T10..T11"

    max_label = max(len(label1), len(label2), len(label3))
    fmt = f"  {{:<{max_label}}}  {{bar}}  {{count}}  ({{pct:.1f}}%)"

    ui.info("")
    ui.info(ui.bold("Counts:"))
    ui.info(fmt.format(label1, bar=b1, count=ui.green(str(c1)), pct=p1))
    ui.info(fmt.format(label2, bar=b2, count=ui.green(str(c2)), pct=p2))
    ui.info(fmt.format(label3, bar=b3, count=ui.green(str(c3)), pct=p3))
    ui.info("")
    ui.info(ui.bold("Total invariants matched: ") + ui.green(str(total)))

    if ui.verbose and not ui.quiet:
        # Diagnostics
        flattened = "".join(part.strip() for part in content.splitlines())
        flat_len = (
            len(re.sub(r"-", "", flattened)) if not args.keep_dashes else len(flattened)
        )
        left_len = len(leftover)
        ratio = (1.0 - (left_len / flat_len)) if flat_len else 1.0
        tok_in = len(re.findall(r"T\d+", content))
        tok_left = len(re.findall(r"T\d+", leftover))
        ui.info("")
        ui.debug(
            f"Flat length: {flat_len}, leftover length: {left_len}, consumed: {ratio:.1%}"
        )
        ui.debug(f"Token occurrences in input: {tok_in}, in leftover: {tok_left}")

    if ok:
        msg = "All invariants consumed."
        if ui.fun:
            msg += " ✨"
        ui.info("")
        ui.info(ui.green(msg))
        sys.exit(0)
    else:
        ui.info("")
        ui.warn("WARNING: leftover content not consumed.")
        prev_len = args.leftover_preview
        if prev_len <= 0:
            preview = ""
        elif len(leftover) <= prev_len:
            preview = leftover
        else:
            head = leftover[: max(1, prev_len // 2)]
            tail = leftover[-max(1, prev_len - len(head)) :]
            preview = head + " … " + tail
        if ui.color:
            preview = re.sub(r"(T\d+)", lambda m: ui.bold(ui.red(m.group(1))), preview)
        ui.info("")
        ui.info(preview)
        sys.exit(0)


if __name__ == "__main__":
    main()
