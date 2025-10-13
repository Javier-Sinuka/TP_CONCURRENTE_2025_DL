# Petri Net Simulator

[![es](https://img.shields.io/badge/lang-es-yellow.svg)](./docs/README.es.md)

## Introduction
Petri Net Simulator is a concurrent, configurable simulation framework written in Java. It provides a robust environment for modeling, simulating, and analyzing systems with concurrent and parallel processes using Petri Nets.

The project emphasizes a clean separation of concerns, with a core Petri Net engine, a monitor-based synchronization layer for handling concurrency, and a pluggable policy system for defining transition firing logic in case of conflicts. The entire simulation, including the net's structure, timing, and threading model, is defined through external YAML configuration files. Additionally, it features a static analysis toolkit to compute and report on structural properties of the net.

## Features
- **YAML Configuration**: Dynamically configure the entire Petri Net, including its structure (places, transitions, incidence matrix), initial state, transition timings, and threading model from a single YAML file.
- **Analysis Tools**: A built-in static analyzer computes key structural properties of the Petri net, including:
    - **P-Invariants (Place Invariants)**: Identifies sets of places where the weighted sum of tokens remains constant, crucial for verifying conservation properties.
    - **T-Invariants (Transition Invariants)**: Finds sequences of transition firings that restore the net to a previous state, useful for detecting cycles and verifying liveness.
    - **Structural Conflicts**: Detects which transitions compete for the same input resources (places).
- **Flexible Command-Line Interface**: Run simulations, analysis, or both using simple command-line flags. Supports executing multiple runs and generating aggregated statistical reports.
- **Multi-threaded Simulation**: Leverage a sophisticated threading model using "Segments" to assign groups of transitions to dedicated worker threads, enabling true parallel simulation.
- **Monitor-Based Synchronization**: Guarantees thread-safe state transitions using a classic monitor pattern, which manages concurrent access to the Petri Net and coordinates worker threads via condition queues.
- **Pluggable Firing Policies**: Easily switch between different strategies for selecting which transition to fire when multiple are enabled. The project includes a `RandomPolicy` and a `PriorityPolicy`.
- **Time-Aware Transitions**: Model timed Petri Nets by specifying minimum and maximum firing delays for each transition.
- **Invariant-Based Simulation Goals**: Define a clear end-condition for simulations by setting an `invariantLimit` in the configuration file.
- **Comprehensive Logging**: Generates detailed log files for both transition sequences and debug information.

## Installation and Building

### Prerequisites
- **Java Development Kit (JDK)**: Version 8 or higher.
- **Apache Maven**: The project uses the Maven Wrapper, so a local installation is not required.

### Steps
1.  **Clone the repository:**
    ```bash
    git clone https://github.com/Javier-Sinuka/TP_CONCURRENTE_2025_DL.git
    ```
2.  **Navigate to the project directory:**
    ```bash
    cd TP_CONCURRENTE_2025_DL
    ```
3.  **Build the project using the Maven Wrapper:**
    This command will compile the source, run tests, and package the application into a runnable JAR file located in the `target/` directory.

    *   On **Linux** or **macOS**:
        ```bash
        ./mvnw clean install
        ```
    *   On **Windows** (Command Prompt or PowerShell):
        ```bash
        .\mvnw.cmd clean install
        ```
    A `BUILD SUCCESS` message indicates the simulator is ready.

## Usage
The simulator is run from the command line and controlled with flags. By default, it runs both a static analysis and a single simulation using `config_default.yaml` if no configuration file is specified.

### Basic Syntax
```bash
java -jar target/petri-sim-1.0.jar [options] [config_file_path]
```

### Command-Line Options

| Option            | Description                                                                                                   |
|-------------------|---------------------------------------------------------------------------------------------------------------|
| `--analysis`      | Run only the static Petri net analysis (P/T invariants, conflicts).                                           |
| `--simulation`    | Run only the simulation.                                                                                      |
| `--runs <N>`      | Execute the simulation `N` times. Defaults to 1.                                                              |
| `--statistics`    | When `--runs > 1`, suppresses individual run reports and shows a final statistical report with averages.      |
| `--debug`         | Enable detailed debug logging. The log file is specified by `logPath` in the YAML configuration.              |
| `--regex-checker` | After each simulation run, execute `scripts/invariant_checker.py`.                                            |
| `--help`          | Display the help message and exit.                                                                            |

### Examples
*   **Run both analysis and a single simulation (default behavior):**
    ```bash
    java -jar target/petri-sim-1.0.jar
    ```

*   **Run analysis only using a specific configuration:**
    ```bash
    java -jar target/petri-sim-1.0.jar --analysis simulation_configs/config_tp_2024.yaml
    ```

*   **Run 10 simulations with statistical reporting and debug logging:**
    ```bash
    java -jar target/petri-sim-1.0.jar --simulation --runs 10 --statistics --debug
    ```

## Configuration File Explained
The simulation's behavior is entirely controlled by a YAML file. Below is a breakdown of its structure.

```yaml
logPath: log_default.txt # Path for the debug output log file.

invariantLimit: 10 # The simulation will stop after this many T-invariants have completed.

initialMarking: [1, 0, 0, 0] # Initial tokens per place (P0, P1, ...).

# The incidence matrix (I). Rows are places, columns are transitions.
# -1: Token is consumed.
#  1: Token is produced.
#  0: No connection.
incidence:
  - [-1, 0, 0, 1]
  - [1, -1, 0, 0]
  - [0, 1, -1, 0]
  - [0, 0, 1, -1]

# Time ranges [min, max] in milliseconds for each transition.
# A transition can only fire if the time since it was last enabled falls within this range.
# [0, 0] represents an immediate transition.
timeRanges:
  - [100, 100]
  - [100, 100]
  - [100, 100]
  - [100, 100]

# Defines the threading model. Each object is a "Segment".
segments:
  - name: Default-Segment
    threadQuantity: 1
    transitions: [0, 1, 2, 3]

# Policy for choosing which transition to fire when multiple are enabled.
# Options: random, priority.
policy: random

# Transition weights used by the "priority" policy. Higher numbers mean higher priority.
transitionWeights:
  0: 1
  1: 1
  2: 1
  3: 1
```

## Contributing
Please see the [`CONTRIBUTING.md`](./CONTRIBUTING.md) file for detailed guidelines on the development workflow, coding standards, and branching model.

### Code Style
The project adheres to the **Google Java Style Guide**. The `spotless-maven-plugin` is used to automatically check and format the code. For more information, see the [Contributing Guidelines](./CONTRIBUTING.md).

## Contact
For any inquiries or support, please open an issue on the GitHub repository.
