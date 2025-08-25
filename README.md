# Petri Net Simulator

## Introduction
The Petri Net Simulator is a concurrent, configurable simulation framework written in Java. It provides a robust environment for modeling, simulating, and analyzing systems with parallel processes using Petri Nets.

The project emphasizes a clean separation of concerns, with a core Petri Net engine, a monitor-based synchronization layer for handling concurrency, and a pluggable policy system for defining transition firing logic. The entire simulation, including the net's structure, timing, and threading model, is defined through external JSON configuration files.

## Features
- **JSON Configuration**: Dynamically configure the entire Petri Net, including its structure (places, transitions, incidence matrix), initial state, transition timings, and threading model from a single JSON file.
- **Multi-threaded Simulation**: Leverage a sophisticated threading model using "Segments" to assign groups of transitions to dedicated worker threads, enabling true parallel simulation.
- **Monitor-Based Synchronization**: Guarantees thread-safe state transitions using a classic monitor pattern, which manages concurrent access to the Petri Net and coordinates worker threads via condition queues.
- **Pluggable Firing Policies**: Easily switch between different strategies for selecting which transition to fire when multiple are enabled. The project includes a `RandomPolicy` and a `PriorityPolicy`, and the `PolicyInterface` allows for custom implementations.
- **Time-Aware Transitions**: Model timed Petri Nets by specifying minimum and maximum firing delays for each transition, allowing for more realistic system simulations.
- **Comprehensive Logging**: Generates a detailed, formatted log file that records the entire simulation sequence, including which thread fired each transition and other significant monitor events.
- **Maven Build System**: The project is built and managed with Maven, providing easy dependency management, testing, and execution.

## Installation

### Prerequisites
- **Java Development Kit (JDK)**: Version 8 or higher.
- **Apache Maven**: The project uses the Maven Wrapper, so a local installation is not required.

### Steps
1.  **Clone the repository:**
    ```bash
    git clone <repository URL>
    ```
2.  **Navigate to the project directory:**
    ```bash
    cd TP_CONCURRENTE_2025_DL
    ```
3.  **Build the project using the Maven Wrapper:**
    This command will compile the source code, download dependencies, and package the application.

    *   On **Linux** or **macOS**:
        ```bash
        ./mvnw clean install
        ```
    *   On **Windows** (Command Prompt or PowerShell):
        ```bash
        mvnw.cmd clean install
        ```
4.  **Run tests to verify the installation:**

    *   On **Linux** or **macOS**:
        ```bash
        ./mvnw test
        ```
    *   On **Windows**:
        ```bash
        mvnw.cmd test
        ```

## Development Workflow and Usage

This project is managed with the Maven Wrapper. Use `./mvnw` on Linux/macOS and `mvnw.cmd` on Windows. The workflow for building, testing, and running is the same across platforms.

#### Step 1: Format Code (Recommended)
Before building, it's good practice to format your code according to the project's style guide (Google Java Style).

*   On **Linux** or **macOS**:
    ```bash
    ./mvnw spotless:apply
    ```
*   On **Windows**:
    ```bash
    mvnw.cmd spotless:apply
    ```

#### Step 2: Build and Verify the Project
The `verify` command compiles the code, runs all unit tests, and packages the application into a runnable JAR file in the `target` directory.

*   On **Linux** or **macOS**:
    ```bash
    ./mvnw verify
    ```
*   On **Windows**:
    ```bash
    mvnw.cmd verify
    ```
    A `BUILD SUCCESS` message means the simulator is ready to run.

#### Step 3: Run the Simulator
Once packaged, you can execute the application. It expects the path to a JSON configuration file as an argument; without one, it defaults to `config_default.json`.

*   On **Linux** or **macOS**:
    ```bash
    java -jar target/petri-sim-0.1.0-SNAPSHOT.jar /path/to/your_config.json
    ```
*   On **Windows**:
    ```bash
    java -jar target\petri-sim-0.1.0-SNAPSHOT.jar \path\to\your_config.json
    ```

#### All-in-One Command
You can chain these commands to format, build, and run in a single line.

*   On **Linux** or **macOS**:
    ```bash
    ./mvnw spotless:apply && ./mvnw verify && java -jar target/petri-sim-0.1.0-SNAPSHOT.jar /path/to/your_config.json
    ```
*   On **Windows**:
    ```bash
    mvnw.cmd spotless:apply && mvnw.cmd verify && java -jar target\petri-sim-0.1.0-SNAPSHOT.jar \path\to\your_config.json
    ```

### Configuration File Explained
The behavior of the simulation is entirely controlled by a JSON file. Here is a breakdown of the structure using `config_default.json` as an example:

```json
{
  // Path for the output log file.
  "logPath": "log_default.txt",

  // An array representing the initial number of tokens in each place. The index corresponds to the place number (P0, P1, ...).
  "initialMarking":,

  // The incidence matrix (I). Rows represent places and columns represent transitions.
  // -1: Token is consumed from the place.
  //  1: Token is produced into the place.
  //  0: No connection.
  "incidence": [
    [-1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
    [1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
    [0, 0, -1, 1, 0, 0, 0, 0, 0, 0, 0, 0],
    [0, 0, 1, -1, 0, 0, 0, 0, 0, 0, 0, 0],
    [0, 0, 0, 0, -1, 1, 0, 0, 0, 0, 0, 0],
    [0, 0, 0, 0, 1, -1, 0, 0, 0, 0, 0, 0]
  ],

  // Time ranges [min, max] in milliseconds for each transition. A transition can only fire if the time since it was enabled falls within this range.
  // represents an immediate transition.
  "timeRanges": [
   ,,,,,
  ],

  // Defines the threading model. Each object in this array is a "Segment".
  "segments": [
    {
      "name": "Default-Segment",       // A logical name for the group of threads.
      "threadQuantity": 1,             // The number of worker threads to create for this segment.
      "transitions":            // The transitions that the threads in this segment will attempt to fire.
    }
  ],

  // A map of transition weights used by the PriorityPolicy. Higher numbers mean higher priority.
  "transitionWeights": {
    "0": 1, "1": 1, "2": 1, "3": 1, "4": 1, "5": 1
  }
}
```

## Contributing

See the [`CONTRIBUTING.md`](./CONTRIBUTING.md) file for detailed guidelines.

### Code Style
Please adhere to the Google Java Style Guide. The project uses the `spotless-maven-plugin` to automatically check and format the code.

## License
This project is licensed under the MIT License. See the `LICENSE` file for more details.

## Contact
For any inquiries or support, please open an issue on the GitHub repository.

