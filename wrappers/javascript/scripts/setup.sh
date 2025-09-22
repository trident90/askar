#!/bin/bash

# Setup script for Aries Askar Node.js wrapper
echo "Setting up Aries Askar Node.js wrapper..."

# Navigate to the root directory
cd "$(dirname "$0")/../../.."

# Build the native library with SQLite support
echo "Building native library with SQLite support..."
cargo build --release --features sqlite

if [ $? -eq 0 ]; then
    echo "✅ Native library built successfully"
else
    echo "❌ Failed to build native library"
    exit 1
fi

# Navigate back to the JavaScript wrapper directory
cd wrappers/javascript

# Install Node.js dependencies
echo "Installing Node.js dependencies..."
npm install

if [ $? -eq 0 ]; then
    echo "✅ Node.js dependencies installed successfully"
else
    echo "❌ Failed to install Node.js dependencies"
    exit 1
fi

# Build TypeScript
echo "Building TypeScript..."
npm run build

if [ $? -eq 0 ]; then
    echo "✅ TypeScript built successfully"
else
    echo "❌ Failed to build TypeScript"
    exit 1
fi

echo "🎉 Setup completed successfully!"
echo ""
echo "You can now run:"
echo "  npm test          - Run tests"
echo "  npm run example   - Run example"