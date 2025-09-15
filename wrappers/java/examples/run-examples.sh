#!/bin/bash

# Askar Java Examples Runner Script
# This script builds and runs the Askar Java examples

set -e  # Exit on any error

cd "$(dirname "$0")"

echo "=== Askar Java Examples Runner ==="
echo "Current directory: $(pwd)"

# Check if Java is available
if ! command -v java &> /dev/null; then
    echo "Error: Java not found. Please install Java 8 or higher."
    exit 1
fi

echo "Java version: $(java -version 2>&1 | head -n 1)"

# Check if Maven is available
if ! command -v mvn &> /dev/null; then
    echo "Error: Maven not found. Please install Maven 3.6 or higher."
    exit 1
fi

echo "Maven version: $(mvn -version | head -n 1)"

# Set library path for native library
ASKAR_LIB_PATH="$(pwd)/../../target/release"
export LD_LIBRARY_PATH="$ASKAR_LIB_PATH:$LD_LIBRARY_PATH"
export DYLD_LIBRARY_PATH="$ASKAR_LIB_PATH:$DYLD_LIBRARY_PATH"  # macOS

echo "Native library path: $ASKAR_LIB_PATH"

# Check if native library exists
NATIVE_LIB=""
if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    NATIVE_LIB="$ASKAR_LIB_PATH/libaries_askar.so"
elif [[ "$OSTYPE" == "darwin"* ]]; then
    NATIVE_LIB="$ASKAR_LIB_PATH/libaries_askar.dylib"
elif [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" ]]; then
    NATIVE_LIB="$ASKAR_LIB_PATH/aries_askar.dll"
fi

if [ ! -f "$NATIVE_LIB" ]; then
    echo "Warning: Native library not found at $NATIVE_LIB"
    echo "Building native library..."

    # Build the native library
    CURRENT_DIR=$(pwd)
    cd ../../
    if command -v cargo &> /dev/null; then
        cargo build --release
        echo "Native library built successfully"
    else
        echo "Error: Cargo not found. Please install Rust toolchain to build the native library."
        exit 1
    fi
    cd "$CURRENT_DIR"
else
    echo "Native library found: $NATIVE_LIB"
fi

# Check if parent wrapper JAR exists
WRAPPER_JAR="../target/aries-askar-0.4.5.jar"
if [ ! -f "$WRAPPER_JAR" ]; then
    echo "Wrapper JAR not found. Building parent project..."
    CURRENT_DIR=$(pwd)
    cd ../
    mvn clean package -DskipTests
    cd "$CURRENT_DIR"
    echo "Wrapper JAR built successfully"
else
    echo "Wrapper JAR found: $WRAPPER_JAR"
fi

echo ""
echo "=== Building Examples ==="
mvn clean compile

echo ""
echo "=== Running Examples ==="

# Function to run an example
run_example() {
    local example_name=$1
    local main_class=$2

    echo ""
    echo "--- Running $example_name ---"
    echo "Main class: $main_class"

    mvn exec:java \
        -Dexec.mainClass="$main_class" \
        -Djava.library.path="$ASKAR_LIB_PATH" \
        -Dexec.cleanupDaemonThreads=false

    echo "--- $example_name completed ---"
}

# Run examples
run_example "BasicStoreExample" "org.hyperledger.aries.askar.examples.BasicStoreExample"
run_example "CryptographyExample" "org.hyperledger.aries.askar.examples.CryptographyExample"

echo ""
echo "=== All Examples Completed Successfully! ==="