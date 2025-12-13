"""
Core logic for verifying and counting T-invariants in transition logs.
"""

from __future__ import annotations

import re
from dataclasses import dataclass
from typing import Tuple

T_INVARIANT_REGEX = re.compile(
    r"(T0)(.*?)(T1)(.*?)"
    r"((T2)(.*?)(T3)(.*?)(T4)|(T5)(.*?)(T6)|(T7)(.*?)(T8)(.*?)(T9)(.*?)(T10))"
    r"(.*?)(T11)",
    re.S,
)

BranchCounts = Tuple[int, int, int]

_T4_GROUP_INDEX = 10  # has T4
_T6_GROUP_INDEX = 13  # has T6
_T10_GROUP_INDEX = 20  # has T10

# Keep only the "gaps" between the required tokens. Missing groups become ''.
_INTER_TOKEN_GROUP_INDICES = (2, 4, 7, 9, 12, 15, 17, 19, 21)

INVARIANT_DEFINITIONS = {
    1: ("T0", "T1", "T2", "T3", "T4", "T11"),
    2: ("T0", "T1", "T5", "T6", "T11"),
    3: ("T0", "T1", "T7", "T8", "T9", "T10", "T11"),
}


@dataclass(frozen=True)
class InvariantCheckResult:
    """Result of checking invariants on a transition log."""

    fully_consumed: bool
    leftover_transitions: str
    invariant_counts: BranchCounts
    log_length: int
    leftover_length: int


def _detect_invariant(match: re.Match[str]) -> int:
    if match.group(_T4_GROUP_INDEX) is not None:
        return 1
    if match.group(_T6_GROUP_INDEX) is not None:
        return 2
    if match.group(_T10_GROUP_INDEX) is not None:
        return 3
    return 0  # should not happen


def _collect_preserved_gaps(match: re.Match[str]) -> str:
    """
    Collects and concatenates all preserved gap groups from a regex match object.

    Returns:
      str: A string formed by joining all non-None preserved gap groups.
    """
    return "".join((match.group(i) or "") for i in _INTER_TOKEN_GROUP_INDICES)


def consume_one_invariant(transition_log: str) -> Tuple[str, int, bool]:
    """
    Find and remove a single invariant match from `transition_log`.

    Returns (remaining_log, matched_invariant, did_consume).
      matched_invariant: 1,2,3 for which invariant matched; 0 if no match.
    """
    match = T_INVARIANT_REGEX.search(transition_log)
    if not match:
        return transition_log, 0, False

    matched_branch = _detect_invariant(match)
    preserved_gaps = _collect_preserved_gaps(match)
    remaining_log = (
        transition_log[: match.start()] + preserved_gaps + transition_log[match.end() :]
    )
    return remaining_log, matched_branch, True


def check_invariants(transition_log: str) -> InvariantCheckResult:
    """
    Repeatedly remove invariant matches and count each branch.
    """
    transition_log_length = len(transition_log)

    invariant_1_count = 0
    invariant_2_count = 0
    invariant_3_count = 0

    remaining_log = transition_log

    while True:
        remaining_log, matched_branch, did_consume = consume_one_invariant(
            remaining_log
        )
        if not did_consume:
            break
        if matched_branch == 1:
            invariant_1_count += 1
        elif matched_branch == 2:
            invariant_2_count += 1
        elif matched_branch == 3:
            invariant_3_count += 1

    leftover_transitions = remaining_log.strip()

    fully_consumed = leftover_transitions == ""

    return InvariantCheckResult(
        fully_consumed=fully_consumed,
        leftover_transitions=leftover_transitions,
        invariant_counts=(invariant_1_count, invariant_2_count, invariant_3_count),
        log_length=transition_log_length,
        leftover_length=len(leftover_transitions),
    )
