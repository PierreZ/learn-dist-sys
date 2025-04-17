#!/usr/bin/env bash
set -eo pipefail

# Script to start a devcontainer for learn-dist-sys project
# Created: $(date)

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
echo "Starting devcontainer for project at: $PROJECT_DIR"

# Check if devcontainer CLI is installed
if ! command -v devcontainer &> /dev/null; then
    echo "Error: devcontainer CLI is not installed"
    echo "Please install it with: npm install -g @devcontainers/cli"
    exit 1
fi

# Start the devcontainer
echo "Starting devcontainer..."
CONTAINER_INFO=$(devcontainer up --workspace-folder "$PROJECT_DIR")
CONTAINER_ID=$(echo "$CONTAINER_INFO" | grep -o '"containerId":"[^"]*"' | cut -d'"' -f4)

if [ -z "$CONTAINER_ID" ]; then
    echo "Failed to get container ID. Raw output:"
    echo "$CONTAINER_INFO"
    exit 1
fi

echo "Devcontainer started successfully!"
echo "Container ID: $CONTAINER_ID"
echo ""
echo "To execute commands in the container:"
echo "  devcontainer exec --workspace-folder \"$PROJECT_DIR\" <command>"
echo ""
echo "To open a shell in the container:"
echo "  devcontainer exec --workspace-folder \"$PROJECT_DIR\" bash"
echo ""
echo "Your local files are mounted in the container at /workspaces/learn-dist-sys"
echo "Any changes you make locally will be reflected in the container and vice versa"

devcontainer exec --workspace-folder "$PROJECT_DIR" bash