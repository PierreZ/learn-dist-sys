# learn-dist-sys
Learn distributed systems using Maelstrom from Jepsen

## Resources

- https://github.com/jepsen-io/maelstrom/tree/main
- https://fly.io/dist-sys/

## Requirements

To work through this lab, you'll need:

- JDK 23 or higher
- JBang
- Graphviz (required by Maelstrom for visualizations)
- Gnuplot (required by Maelstrom for plots)

## Setup

Run the setup script to download and install Maelstrom and verify required dependencies:

```bash
./setup.sh
```

This script will:
1. Download and extract Maelstrom to the `bin` directory
2. Make the Maelstrom executable
3. Check if required dependencies are installed

## Lab Structure

This lab is organized into steps of increasing complexity:

- **Step 0**: Introduction to Maelstrom and JBang
- **Step 1-3**: Implementing distributed protocols with increasing complexity

Each step has its own directory with instructions and code.

## Getting Started

Begin with Step 0 to get familiar with the tools:

```bash
cd step-0
```

You can verify your installation by running these commands:

```bash
# Check Java version
java -version
# Should show Java 23 or higher

# Check JBang is installed
jbang --version

# Check Graphviz (required by Maelstrom)
dot -V

# Check Gnuplot (required by Maelstrom)
gnuplot --version

# Check Maelstrom is installed and working
maelstrom help
```

Then follow the instructions in the README.md file in each step directory.

## Resources

- https://github.com/jepsen-io/maelstrom/tree/main
- https://fly.io/dist-sys/
