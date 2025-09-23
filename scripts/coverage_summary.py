#!/usr/bin/env python3
"""Pretty-print a JaCoCo CSV summary with per-package and per-class coverage.

This script reads `target/site/jacoco/jacoco.csv` produced by JaCoCo and prints
an aligned table showing coverage for each package and, under each package,
coverage for each class. It also prints a totals row at the bottom.
"""

import csv
from collections import defaultdict
from typing import Dict, Iterable, List, Tuple

# Table layout configuration
NAME_COL_WIDTH = 45
CELL_WIDTH = 20


def _fit_name(name: str, width: int) -> str:
    """Return ``name`` truncated with an ellipsis to fit within ``width``.

    Keeps the left-most characters and appends a single Unicode ellipsis if the
    string would exceed the given width.
    """

    if len(name) <= width:
        return name
    return name[: width - 1] + "…"


def _format_cell(missed: int, covered: int) -> str:
    """Return a formatted coverage cell 'pp.pp% (covered/total)'."""

    total = missed + covered
    percentage = (covered / total) * 100 if total > 0 else 0
    return f"{percentage:6.2f}% ({covered}/{total})"


def _build_header() -> Tuple[str, str]:
    """Return the header string and the rule line based on configured widths."""

    header = (
        f"{'Package':<{NAME_COL_WIDTH}} | "
        f"{'Instructions':^{CELL_WIDTH}} | {'Branches':^{CELL_WIDTH}} | "
        f"{'Lines':^{CELL_WIDTH}} | {'Complexity':^{CELL_WIDTH}} | "
        f"{'Methods':^{CELL_WIDTH}} | {'Classes':^{CELL_WIDTH}}"
    )
    return header, "-" * len(header)


def _parse_csv(
    csv_file_path: str,
) -> Tuple[
    List[str],
    Dict[str, Dict[str, Dict[str, int]]],
    Dict[str, Dict[str, int]],
    Dict[str, Dict[str, Dict[str, Dict[str, int]]]],
    Dict[str, Dict[str, int]],
    Dict[str, int],
]:
    """Parse JaCoCo CSV and aggregate counts for package and class levels."""

    metrics = ["INSTRUCTION", "BRANCH", "LINE", "COMPLEXITY", "METHOD"]

    package_coverage: Dict[str, Dict[str, Dict[str, int]]] = defaultdict(
        lambda: {metric: {"missed": 0, "covered": 0} for metric in metrics}
    )
    package_classes: Dict[str, Dict[str, int]] = defaultdict(
        lambda: {"missed": 0, "covered": 0}
    )
    package_class_coverage: Dict[str, Dict[str, Dict[str, Dict[str, int]]]] = (
        defaultdict(
            lambda: defaultdict(
                lambda: {metric: {"missed": 0, "covered": 0} for metric in metrics}
            )
        )
    )

    total_coverage: Dict[str, Dict[str, int]] = {
        metric: {"missed": 0, "covered": 0} for metric in metrics
    }
    total_classes: Dict[str, int] = {"missed": 0, "covered": 0}

    # Use explicit encoding for portability/lint
    with open(csv_file_path, "r", encoding="utf-8") as csvfile:
        reader = csv.DictReader(csvfile)
        for row in reader:
            package = row["PACKAGE"]
            class_name = row["CLASS"]

            for metric in metrics:
                missed = int(row[f"{metric}_MISSED"])
                covered = int(row[f"{metric}_COVERED"])

                package_coverage[package][metric]["missed"] += missed
                package_coverage[package][metric]["covered"] += covered

                package_class_coverage[package][class_name][metric]["missed"] += missed
                package_class_coverage[package][class_name][metric][
                    "covered"
                ] += covered

                total_coverage[metric]["missed"] += missed
                total_coverage[metric]["covered"] += covered

            # Class covered is inferred (CSV lacks CLASS_* counters)
            if int(row["METHOD_COVERED"]) > 0 or int(row["INSTRUCTION_COVERED"]) > 0:
                package_classes[package]["covered"] += 1
                total_classes["covered"] += 1
            else:
                package_classes[package]["missed"] += 1
                total_classes["missed"] += 1

    return (
        metrics,
        package_coverage,
        package_classes,
        package_class_coverage,
        total_coverage,
        total_classes,
    )


def _print_header(header: str, rule: str) -> None:
    """Print the table header and title."""

    print(rule)
    print("Code Coverage Summary".center(len(header)))
    print(rule)
    print(header)
    print(rule)


def _print_package_block(
    package: str,
    metrics: Iterable[str],
    package_coverage: Dict[str, Dict[str, Dict[str, int]]],
    package_classes: Dict[str, Dict[str, int]],
    package_class_coverage: Dict[str, Dict[str, Dict[str, Dict[str, int]]]],
) -> None:
    """Print a package summary line and its per-class table."""

    data = package_coverage[package]
    pkg_display = _fit_name(package, NAME_COL_WIDTH)
    print(f"{pkg_display:<{NAME_COL_WIDTH}}", end="")
    for metric in metrics:
        missed = data[metric]["missed"]
        covered = data[metric]["covered"]
        cell = _format_cell(missed, covered)
        print(f" | {cell:^{CELL_WIDTH}}", end="")

    class_missed = package_classes[package]["missed"]
    class_covered = package_classes[package]["covered"]
    class_cell = _format_cell(class_missed, class_covered)
    print(f" | {class_cell:^{CELL_WIDTH}}", end="")
    print()

    _print_class_table(package, metrics, package_class_coverage)


def _print_class_table(
    package: str,
    metrics: Iterable[str],
    package_class_coverage: Dict[str, Dict[str, Dict[str, Dict[str, int]]]],
) -> None:
    """Print the per-class coverage table for the given package."""

    classes = sorted(package_class_coverage[package].keys())
    if not classes:
        return

    class_header = (
        f"{_fit_name('  Class', NAME_COL_WIDTH):<{NAME_COL_WIDTH}} | "
        f"{'Instructions':^{CELL_WIDTH}} | {'Branches':^{CELL_WIDTH}} | "
        f"{'Lines':^{CELL_WIDTH}} | {'Complexity':^{CELL_WIDTH}} | "
        f"{'Methods':^{CELL_WIDTH}} | {'':^{CELL_WIDTH}}"
    )
    print(class_header)

    for cls in classes:
        class_display = "  " + _fit_name(cls, NAME_COL_WIDTH - 2)
        print(f"{class_display:<{NAME_COL_WIDTH}}", end="")
        cdata = package_class_coverage[package][cls]
        for metric in metrics:
            missed = cdata[metric]["missed"]
            covered = cdata[metric]["covered"]
            cell = _format_cell(missed, covered)
            print(f" | {cell:^{CELL_WIDTH}}", end="")
        # Empty Classes column to keep alignment with package rows
        print(f" | {'':^{CELL_WIDTH}}", end="")
        print()


def _print_totals(
    metrics: Iterable[str],
    total_coverage: Dict[str, Dict[str, int]],
    total_classes: Dict[str, int],
) -> None:
    """Print the totals row with the same alignment as package rows."""

    print(f"{'Total':<{NAME_COL_WIDTH}}", end="")
    for metric in metrics:
        missed = total_coverage[metric]["missed"]
        covered = total_coverage[metric]["covered"]
        cell = _format_cell(missed, covered)
        print(f" | {cell:^{CELL_WIDTH}}", end="")

    class_missed = total_classes["missed"]
    class_covered = total_classes["covered"]
    class_cell = _format_cell(class_missed, class_covered)
    print(f" | {class_cell:^{CELL_WIDTH}}", end="")
    print()


def generate_coverage_summary(csv_file_path: str) -> None:
    """Generate and print the coverage summary table from a JaCoCo CSV file."""

    (
        metrics,
        package_coverage,
        package_classes,
        package_class_coverage,
        total_coverage,
        total_classes,
    ) = _parse_csv(csv_file_path)

    header, rule = _build_header()
    _print_header(header, rule)

    first_pkg = True
    for package in sorted(package_coverage.keys()):
        if not first_pkg:
            print(rule)
        _print_package_block(
            package,
            metrics,
            package_coverage,
            package_classes,
            package_class_coverage,
        )
        first_pkg = False

    print(rule)
    _print_totals(metrics, total_coverage, total_classes)
    print(rule)


if __name__ == "__main__":
    generate_coverage_summary("target/site/jacoco/jacoco.csv")
