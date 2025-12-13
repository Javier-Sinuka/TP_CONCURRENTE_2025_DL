"""
Terminal presentation helpers for the invariant checker.
"""

from __future__ import annotations

import os
import re
import sys
from dataclasses import dataclass
from typing import Iterable, TextIO, Tuple

from .invariant_checker_core import InvariantCheckResult, INVARIANT_DEFINITIONS

VERSION = "1.3.0"

_TOKEN_RE = re.compile(r"T\d+")
_TOKEN_CAPTURE_RE = re.compile(r"(T\d+)")


class Colors:  # pylint: disable=too-few-public-methods
    """ANSI color escape codes for styled terminal output."""

    RESET = "\033[0m"
    BOLD = "\033[1m"
    DIM = "\033[2m"
    RED = "\033[31m"
    GREEN = "\033[32m"
    YELLOW = "\033[33m"
    CYAN = "\033[36m"


def supports_color(stream) -> bool:
    """Return True when `stream` likely supports ANSI colors."""
    if not hasattr(stream, "isatty") or not stream.isatty():
        return False
    if os.environ.get("NO_COLOR"):
        return False
    if sys.platform == "win32":  # pyright: ignore[reportUnreachableCode]
        return bool(os.environ.get("WT_SESSION") or os.environ.get("ANSICON"))
    return True


@dataclass
class UI:  # pylint: disable=missing-function-docstring
    """Small console UI facade used by the CLI renderer."""

    color: bool = True
    decorations_enabled: bool = True
    quiet: bool = False
    verbose: bool = False
    stream: TextIO = sys.stdout

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
            self.stream.write(f"{text}\n")

    def debug(self, text: str) -> None:
        if self.verbose and not self.quiet:
            self.stream.write(f"{self.dim(text)}\n")

    def warn(self, text: str) -> None:
        if not self.quiet:
            self.stream.write(f"{self.yellow(text)}\n")


def _render_bar(n: int, total: int, width: int, color_fn) -> str:
    """Render a proportional bar with optional coloring."""
    filled = int(round((n / total) * width)) if total else 0
    empty = width - filled
    return color_fn("█" * filled) + ("." * empty)


def _count_tokens(text: str) -> int:
    """Count how many `T<number>` tokens exist in `text`."""
    return len(_TOKEN_RE.findall(text))


def render_banner(ui: UI) -> Iterable[str]:
    """Return the banner lines (already styled), or an empty iterable if disabled."""
    if ui.quiet or not ui.decorations_enabled:
        return []
    logo = [
        "┌────────────────────────────────────────────────────────┐",
        "│               T‑Invariant Regex Checker                │",
        "└────────────────────────────────────────────────────────┘",
    ]
    return [ui.cyan(ui.bold(line)) for line in logo]


def _format_invariant_label(tokens: Tuple[str, ...]) -> str:
    """Format an invariant definition like ('T0','T1','T5') as 'T0..T1..T5'."""
    return "..".join(tokens)


def render_counts(ui: UI, counts: Tuple[int, int, int], width: int) -> Iterable[str]:
    """Return formatted count lines for each invariant branch."""
    total = sum(counts)
    markers = ("①", "②", "③") if ui.decorations_enabled else ("1", "2", "3")
    labels = tuple(
        f"{markers[invariant_id - 1]} {_format_invariant_label(tokens)}"
        for invariant_id, tokens in sorted(INVARIANT_DEFINITIONS.items())
    )
    max_label = max(len(l) for l in labels)
    fmt = f"  {{:<{max_label}}}  {{invariant_bar}}  {{count}}  ({{pct:.1f}}%)"

    lines: list[str] = ["", ui.bold("Counts:")]
    for label, n in zip(labels, counts):
        pct = (n * 100.0 / total) if total else 0.0
        invariant_bar = _render_bar(n, total, width, ui.green)
        lines.append(
            fmt.format(
                label, invariant_bar=invariant_bar, count=ui.green(str(n)), pct=pct
            )
        )
    lines.append("")
    lines.append(ui.bold("Total invariants matched: ") + ui.green(str(total)))
    return lines


def render_diagnostics(
    ui: UI, original_text: str, result: InvariantCheckResult
) -> Iterable[str]:
    """Return verbose diagnostic lines, or an empty iterable when disabled."""
    if not ui.verbose or ui.quiet:
        return []

    original_token_count = _count_tokens(original_text)
    leftover_token_count = _count_tokens(result.leftover_transitions)

    input_len = result.log_length
    left_len = result.leftover_length
    ratio = (1.0 - (left_len / input_len)) if input_len else 1.0

    return [
        "",
        ui.dim(
            f"Input length: {input_len}, leftover length: {left_len}, consumed: {ratio:.1%}"
        ),
        ui.dim(
            "Token occurrences in input: "
            f"{original_token_count}, in leftover: {leftover_token_count}"
        ),
    ]


DEFAULT_PREVIEW_LIMIT = 200


def render_leftover_preview(
    ui: UI, leftover: str, limit: int = DEFAULT_PREVIEW_LIMIT
) -> Iterable[str]:
    """Return a warning + highlighted preview of leftover transitions."""
    if limit <= 0:
        preview = ""
    elif len(leftover) <= limit:
        preview = leftover
    else:
        head = leftover[: max(1, limit // 2)]
        tail = leftover[-max(1, limit - len(head)) :]
        preview = head + " … " + tail

    if ui.color:
        preview = _TOKEN_CAPTURE_RE.sub(lambda m: ui.bold(ui.red(m.group(1))), preview)
    return [
        "",
        ui.yellow(
            "NOTE: leftover transitions found "
            "(this can happen if the simulation stops mid-invariant)."
        ),
        "",
        preview,
    ]


def render_status(ui: UI, result: InvariantCheckResult) -> Iterable[str]:
    """Return either a success line or a leftover preview."""
    if result.fully_consumed:
        msg = "No leftover transitions."
        if ui.decorations_enabled:
            msg += " ✨"
        return ["", ui.green(msg)]

    return render_leftover_preview(ui, result.leftover_transitions)


def print_report(
    ui: UI,
    *,
    original_text: str,
    result: InvariantCheckResult,
    bar_width: int,
) -> None:
    """Print the full report (banner, counts, optional diagnostics, and status)."""
    for line in render_banner(ui):
        ui.info(line)
    for line in render_counts(ui, result.invariant_counts, bar_width):
        ui.info(line)
    for line in render_diagnostics(ui, original_text, result):
        ui.info(line)
    for line in render_status(ui, result):
        ui.info(line)
