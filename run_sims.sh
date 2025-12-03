#!/usr/bin/env bash
set -euo pipefail

# Usage:
#   ./run_sims.sh --runs 300 config1.json config2.json ...
#
# For each config:
#   systemd-inhibit --what=sleep java -jar target/petri-sim-1.0.jar <config> --simulation --runs <runs> --statistics
# Output:
#   run_outputs/<runs>_runs_<config_name>.txt
#   where <config_name> is the config filename without "config_" prefix and without extension.

if [[ $# -lt 3 || "$1" != "--runs" ]]; then
    echo "Usage: $0 --runs <N> config1.json [config2.json ...]" >&2
    exit 1
fi

shift
RUNS="$1"
shift

mkdir -p run_outputs

total="$#"
idx=0

for cfg in "$@"; do
    idx=$((idx + 1))

    if [[ ! -f "$cfg" ]]; then
        echo "Config file not found: $cfg" >&2
        exit 1
    fi

    base="$(basename "$cfg")"
    base_no_ext="${base%.*}"             # e.g. config_5_segments_1_thread_segment_A_random
    config_name="${base_no_ext#config_}" # strip leading "config_" if present

    out_file="run_outputs/${RUNS}_runs_${config_name}.txt"

    echo
    echo "============================================================"
    echo "[$(date +'%F %T')] Running simulation (${idx}/${total})"
    echo "  Config : $cfg"
    echo "  Output : $out_file"
    echo "============================================================"

    systemd-inhibit --what=sleep \
        java -jar target/petri-sim-1.0.jar \
        "$cfg" \
        --simulation \
        --runs "$RUNS" \
        --statistics \
        >"$out_file"

    echo "[$(date +'%F %T')] Finished (${idx}/${total}) -> $out_file"
done

echo
echo "All simulations finished."
