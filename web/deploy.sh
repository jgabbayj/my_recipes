#!/bin/bash

# Exit on error
set -e

# Clear screen for readability
clear

echo "============================================="
echo "  Deploying Recipe App for Local Network"
echo "============================================="
echo

# Get the directory where this script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
cd "$SCRIPT_DIR"

# Ensure dependencies are installed
if [ ! -d "node_modules" ]; then
  echo "📥 node_modules not found. Installing dependencies..."
  npm install
else
  echo "✔ Dependencies already installed."
fi

# Build and start server
echo "🚀 Building application and starting server..."
npm run deploy
