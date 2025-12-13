"""Run the invariant checker as a module: `python -m invariant_checker`."""

from __future__ import annotations

import sys

from .invariant_checker import main

if __name__ == "__main__":
    sys.exit(main())
