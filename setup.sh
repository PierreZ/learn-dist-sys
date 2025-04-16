#!/usr/bin/env bash
set -eo pipefail

# Create bin directory if it doesn't exist
mkdir -p bin

# Download Maelstrom if not already present
if [ ! -f "bin/maelstrom" ]; then
    echo "Downloading Maelstrom..."
    curl -L https://github.com/jepsen-io/maelstrom/releases/download/v0.2.4/maelstrom.tar.bz2 -o /tmp/maelstrom.tar.bz2
    
    echo "Extracting Maelstrom..."
    tar xjf /tmp/maelstrom.tar.bz2 -C /tmp
    mv /tmp/maelstrom/* bin/
    rm -rf /tmp/maelstrom.tar.bz2 /tmp/maelstrom
    
    echo "Making Maelstrom executable..."
    chmod +x bin/maelstrom
    
    echo "Maelstrom has been installed to bin/maelstrom"
else
    echo "Maelstrom is already installed in bin/maelstrom"
fi


# Check for jbang and install if needed
echo "Checking jbang..."
if command -v jbang >/dev/null 2>&1; then
    echo "jbang is already installed"
else
    echo "Installing jbang..."
    sdk install jbang
fi


# Check for required dependencies
echo "Checking dependencies..."
missing_deps=()

if ! command -v java >/dev/null 2>&1; then
    missing_deps+=("Java")
fi

if ! command -v dot >/dev/null 2>&1; then
    missing_deps+=("Graphviz")
fi

if ! command -v gnuplot >/dev/null 2>&1; then
    missing_deps+=("Gnuplot")
fi

if [ ${#missing_deps[@]} -ne 0 ]; then
    echo "Warning: The following dependencies are missing:"
    printf '%s\n' "${missing_deps[@]}"
    echo "Please install them using your system's package manager"
fi