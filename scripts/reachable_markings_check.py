#!/usr/bin/env python3
import re
import sys
from typing import Dict, List, Set, Tuple

try:
    import yaml
except ImportError:  # pragma: no cover - dependency optional
    yaml = None  # type: ignore
import json

DEFAULT_SOURCE: Dict[str, Tuple[int, ...]] = {
    "S0": (3, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0),
    "S1": (2, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0),
    "S2": (2, 0, 1, 1, 0, 0, 1, 0, 0, 0, 0, 0),
    "S3": (2, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0),
    "S4": (2, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0),
    "S5": (2, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0),
    "S6": (1, 1, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0),
    "S7": (1, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0),
    "S8": (1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0),
    "S9": (1, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0),
    "S10": (1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0),
    "S11": (1, 0, 1, 1, 0, 0, 0, 0, 1, 0, 0, 0),
    "S12": (1, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1),
    "S13": (1, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0, 0),
    "S14": (1, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0),
    "S15": (1, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0),
    "S16": (1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0),
    "S17": (1, 0, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0),
    "S18": (0, 1, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0),
    "S19": (0, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0),
    "S20": (1, 0, 1, 1, 0, 1, 0, 0, 0, 0, 0, 0),
    "S21": (0, 1, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0),
    "S22": (1, 0, 1, 1, 0, 0, 0, 0, 0, 0, 1, 0),
    "S23": (0, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0),
    "S24": (0, 0, 1, 2, 0, 0, 0, 0, 1, 0, 0, 0),
    "S25": (0, 1, 0, 1, 0, 0, 1, 0, 0, 0, 0, 1),
    "S26": (0, 0, 1, 2, 0, 0, 0, 1, 0, 0, 0, 0),
    "S27": (0, 1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0),
    "S28": (0, 0, 1, 2, 1, 0, 0, 0, 0, 0, 0, 0),
    "S29": (0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0),
    "S30": (0, 0, 1, 2, 0, 0, 0, 0, 0, 1, 0, 0),
    "S31": (0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1),
    "S32": (0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1),
    "S33": (0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1),
    "S34": (0, 0, 1, 2, 0, 0, 1, 0, 0, 0, 0, 1),
    "S35": (0, 0, 1, 2, 0, 1, 0, 0, 0, 0, 0, 0),
    "S36": (0, 0, 1, 2, 0, 0, 0, 0, 0, 0, 1, 0),
    "S37": (0, 0, 1, 1, 0, 0, 0, 0, 1, 0, 0, 1),
    "S38": (0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0, 1),
    "S39": (0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 1),
    "S40": (1, 0, 1, 2, 0, 0, 1, 0, 0, 0, 0, 0),
    "S41": (0, 1, 0, 2, 0, 0, 1, 0, 0, 0, 0, 0),
}

# ---------- Log parsing ----------
VEC_RE = re.compile(r"\[((?:\s*\d+\s*,){11}\s*\d+)\]")


def parse_log_markings(log_path: str) -> List[Tuple[int, ...]]:
    markings: List[Tuple[int, ...]] = []
    with open(log_path, "r") as f:
        for line in f:
            for m in VEC_RE.findall(line):
                vec = tuple(int(x.strip()) for x in m.split(","))
                if len(vec) == 12:
                    markings.append(vec)
    return markings


def check_token_limit(
    markings: Set[Tuple[int, ...]], limit: int = 5
) -> List[Tuple[int, ...]]:
    return [m for m in markings if sum(m) > limit]


def load_source(path: str) -> Dict[str, Tuple[int, ...]]:
    with open(path, "r") as f:
        if yaml is not None:
            raw = yaml.safe_load(f)
        else:
            raw = json.load(f)
    if not isinstance(raw, dict):
        raise ValueError("Source YAML must map labels to 12-int arrays")
    source: Dict[str, Tuple[int, ...]] = {}
    for k, v in raw.items():
        if not isinstance(k, str):
            raise ValueError(f"Invalid label key {k!r}: must be string")
        if (
            not isinstance(v, list)
            or len(v) != 12
            or not all(isinstance(x, int) for x in v)
        ):
            raise ValueError(f"Invalid vector for {k}: must be list of 12 integers")
        source[k] = tuple(v)
    return source


def write_default_source(path: str) -> None:
    payload = {k: list(v) for k, v in DEFAULT_SOURCE.items()}
    with open(path, "w") as f:
        if yaml is not None:
            yaml.safe_dump(payload, f, sort_keys=True)
        else:
            json.dump(payload, f, indent=2)
    print(f"Wrote default source to {path}")


def main() -> None:
    # CLI
    if len(sys.argv) >= 2 and sys.argv[1] == "--init-source":
        if len(sys.argv) != 3:
            print(f"Usage: {sys.argv[0]} --init-source <source.yml>")
            sys.exit(1)
        write_default_source(sys.argv[2])
        sys.exit(0)

    if len(sys.argv) != 3:
        print(f"Usage: {sys.argv[0]} <source.yml> <log_file>")
        sys.exit(1)

    source_path = sys.argv[1]
    log_path = sys.argv[2]

    # Load data
    SOURCE = load_source(source_path)
    SOURCE_SET: Set[Tuple[int, ...]] = set(SOURCE.values())
    inv_index = {v: k for k, v in SOURCE.items()}
    log_markings_list = parse_log_markings(log_path)
    log_set: Set[Tuple[int, ...]] = set(log_markings_list)

    # A) Source -> Log: which source markings are missing in the log?
    missing_in_log = SOURCE_SET - log_set

    # B) Log -> Source: which log markings are not present in the source?
    extra_in_log = log_set - SOURCE_SET

    # C) Token-limit check (sum > 5) on both sets
    invalid_source = check_token_limit(SOURCE_SET, limit=5)
    invalid_log = check_token_limit(log_set, limit=5)

    # ----- Report -----
    print("=== Source markings missing in LOG ===")
    if not missing_in_log:
        print("(none)")
    else:
        for m in sorted(missing_in_log):
            label = inv_index.get(m, "?")
            print(f"{label}\t{list(m)}")

    print("\n=== LOG markings not present in SOURCE ===")
    if not extra_in_log:
        print("(none)")
    else:
        for m in sorted(extra_in_log):
            print(list(m))

    print("\n=== Token sum > 5 (SOURCE) ===")
    if not invalid_source:
        print("(none)")
    else:
        for m in sorted(invalid_source):
            label = inv_index.get(m, "?")
            print(f"{label}\t{list(m)} (sum={sum(m)})")

    print("\n=== Token sum > 5 (LOG) ===")
    if not invalid_log:
        print("(none)")
    else:
        for m in sorted(invalid_log):
            print(f"{list(m)} (sum={sum(m)})")


if __name__ == "__main__":
    main()
