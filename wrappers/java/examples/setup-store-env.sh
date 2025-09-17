#!/bin/bash

# Askar Java Wrapper - Store Environment Setup Script
# This script prepares the environment for running store operations

echo "=== Askar Store Environment Setup ==="

# Check if running as script or sourced
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    echo "ℹ️  Run with: source setup-store-env.sh"
    echo "   or: . setup-store-env.sh"
    exit 1
fi

# 1. Check SQLite installation
echo "1. Checking SQLite installation..."
if command -v sqlite3 &> /dev/null; then
    SQLITE_VERSION=$(sqlite3 --version | cut -d' ' -f1)
    echo "✅ SQLite found: $SQLITE_VERSION"
else
    echo "❌ SQLite not found. Installing..."
    if command -v apt &> /dev/null; then
        sudo apt update && sudo apt install -y sqlite3 libsqlite3-dev
    elif command -v yum &> /dev/null; then
        sudo yum install -y sqlite sqlite-devel
    elif command -v dnf &> /dev/null; then
        sudo dnf install -y sqlite sqlite-devel
    elif command -v brew &> /dev/null; then
        brew install sqlite
    else
        echo "❌ Package manager not found. Please install SQLite manually."
        return 1
    fi
fi

# 2. Check write permissions
echo "2. Checking directory permissions..."
if touch test_write_check.tmp 2>/dev/null; then
    rm -f test_write_check.tmp
    echo "✅ Write permissions OK"
else
    echo "❌ No write permission in current directory"
    echo "   Run: chmod 755 $(pwd)"
    return 1
fi

# 3. Test SQLite database creation
echo "3. Testing SQLite database creation..."
if sqlite3 test_sqlite_check.db "CREATE TABLE test(id INTEGER); INSERT INTO test VALUES(1); SELECT * FROM test;" 2>/dev/null | grep -q "1"; then
    rm -f test_sqlite_check.db
    echo "✅ SQLite database creation works"
else
    echo "❌ SQLite database creation failed"
    return 1
fi

# 4. Check Java environment
echo "4. Checking Java environment..."
if [[ -n "$JAVA_HOME" ]]; then
    echo "✅ JAVA_HOME: $JAVA_HOME"
else
    echo "⚠️  JAVA_HOME not set, detecting..."
    if [[ -d "/usr/lib/jvm/java-11-openjdk-amd64" ]]; then
        export JAVA_HOME="/usr/lib/jvm/java-11-openjdk-amd64"
        echo "✅ Set JAVA_HOME: $JAVA_HOME"
    else
        echo "❌ Java not found. Please set JAVA_HOME manually."
        return 1
    fi
fi

# 5. Check native library
echo "5. Checking native library..."
NATIVE_LIB="../src/main/native/libaskar_jni_wrapper.so"
if [[ -f "$NATIVE_LIB" ]]; then
    echo "✅ Native library found: $NATIVE_LIB"
    # Check if it's linked with Askar core
    if ldd "$NATIVE_LIB" 2>/dev/null | grep -q "libaries_askar"; then
        echo "✅ Askar core library linked"
    else
        echo "⚠️  Askar core library may not be linked"
        echo "   Run: cd ../src/main/native && ./build.sh"
    fi
else
    echo "❌ Native library not found: $NATIVE_LIB"
    echo "   Run: cd ../src/main/native && ./build.sh"
    return 1
fi

# 6. Set environment variables for store operations
echo "6. Setting up environment variables..."
export LD_LIBRARY_PATH="../src/main/native:/home/ubuntu/Projects/DID/askar/target/release:$LD_LIBRARY_PATH"
export RUST_LOG="${RUST_LOG:-warn}"  # Set to 'debug' for verbose output

# 7. Create test data directory
echo "7. Creating test data directory..."
mkdir -p test_data
chmod 755 test_data
echo "✅ Test data directory: test_data/"

echo ""
echo "🎉 Store environment setup completed!"
echo ""
echo "📋 Ready to run:"
echo "   java -cp \"target/classes:../target/aries-askar-0.4.5.jar\" \\"
echo "        -Djava.library.path=../src/main/native \\"
echo "        org.hyperledger.aries.askar.examples.BasicStoreExample"
echo ""
echo "🔧 Environment variables set:"
echo "   JAVA_HOME=$JAVA_HOME"
echo "   LD_LIBRARY_PATH=$LD_LIBRARY_PATH"
echo "   RUST_LOG=$RUST_LOG"
echo ""
echo "💡 For debugging store issues, set: export RUST_LOG=debug"