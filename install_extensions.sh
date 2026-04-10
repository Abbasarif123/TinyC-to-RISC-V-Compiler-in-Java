#!/usr/bin/env bash

DEVCONTAINER_FILE=".devcontainer/devcontainer.json"

if [ ! -f "$DEVCONTAINER_FILE" ]; then
    echo "Error: $DEVCONTAINER_FILE not found!"
    exit 1
fi

if ! command -v jq &> /dev/null; then
    echo "Error: 'jq' is required but not installed. Please install it and try again."
    exit 1
fi

echo "Extracting extensions from $DEVCONTAINER_FILE..."

# Parse the JSON file, filter out comments, and extract the extensions array
extensions=$(jq -r '
  # Strip C-style comments if present by using jq filter logic or relying on clean input
  .customizations.vscode.extensions[]
' "$DEVCONTAINER_FILE" 2>/dev/null)

if [ -z "$extensions" ]; then
    echo "No extensions found in the configuration."
    exit 0
fi

echo "Starting installation..."
echo "----------------------------------------"
while read -r extension_id; do
    if [ -n "$extension_id" ]; then
        echo "Installing: $extension_id"
        code --force --install-extension "$extension_id"
        echo "----------------------------------------"
    fi
done <<< "$extensions"

echo "All extensions processed!"
