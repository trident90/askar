#!/bin/bash

# Askar JNI Wrapper Build Script
# This script compiles the JNI wrapper without CMake

echo "=== Building Askar JNI Wrapper ==="

# Check if Java is available
if ! command -v javac &> /dev/null; then
    echo "❌ Java not found. Please install Java 8 or higher."
    exit 1
fi

# Check if GCC is available
if ! command -v gcc &> /dev/null; then
    echo "❌ GCC not found. Please install GCC compiler."
    exit 1
fi

# Detect Java include paths
if [ -z "$JAVA_HOME" ]; then
    echo "⚠️  JAVA_HOME not set, trying to detect Java paths..."
    
    # Common Java paths
    JAVA_PATHS=(
        "/usr/lib/jvm/java-11-openjdk-amd64"
        "/usr/lib/jvm/java-8-openjdk-amd64"
        "/usr/lib/jvm/default-java"
        "/usr/lib/jvm/java-17-openjdk-amd64"
    )
    
    for path in "${JAVA_PATHS[@]}"; do
        if [ -d "$path/include" ]; then
            JAVA_HOME="$path"
            echo "✅ Found Java at: $JAVA_HOME"
            break
        fi
    done
    
    if [ -z "$JAVA_HOME" ]; then
        echo "❌ Could not find Java include directory. Please set JAVA_HOME."
        exit 1
    fi
fi

# Set include paths
JAVA_INCLUDE="$JAVA_HOME/include"
JAVA_INCLUDE_LINUX="$JAVA_HOME/include/linux"

# Check if include directories exist
if [ ! -d "$JAVA_INCLUDE" ]; then
    echo "❌ Java include directory not found: $JAVA_INCLUDE"
    exit 1
fi

if [ ! -d "$JAVA_INCLUDE_LINUX" ]; then
    echo "❌ Java Linux include directory not found: $JAVA_INCLUDE_LINUX"
    exit 1
fi

# Set source and output files
SOURCE_FILE="askar_jni_wrapper.c"
OUTPUT_FILE="libaskar_jni_wrapper.so"

echo "📁 Source file: $SOURCE_FILE"
echo "📁 Output file: $OUTPUT_FILE"
echo "📁 Java include: $JAVA_INCLUDE"
echo "📁 Java Linux include: $JAVA_INCLUDE_LINUX"

# Check if source file exists
if [ ! -f "$SOURCE_FILE" ]; then
    echo "❌ Source file not found: $SOURCE_FILE"
    exit 1
fi

echo ""
echo "🔨 Compiling..."

# Find Askar core library
ASKAR_LIB_DIR="/home/ubuntu/Projects/DID/askar/target/release"
ASKAR_LIB="$ASKAR_LIB_DIR/libaries_askar.so"

echo "📁 Askar library: $ASKAR_LIB"

# Check if Askar library exists
if [ ! -f "$ASKAR_LIB" ]; then
    echo "❌ Askar core library not found: $ASKAR_LIB"
    echo "Building Askar core library..."
    cd /home/ubuntu/Projects/DID/askar
    cargo build --release
    cd - > /dev/null
    
    if [ ! -f "$ASKAR_LIB" ]; then
        echo "❌ Failed to build Askar core library"
        exit 1
    fi
fi

# Compile with GCC and link Askar core
gcc -shared -fPIC -o "$OUTPUT_FILE" "$SOURCE_FILE" \
    -I"$JAVA_INCLUDE" \
    -I"$JAVA_INCLUDE_LINUX" \
    -L"$ASKAR_LIB_DIR" \
    -laries_askar \
    -lpthread \
    -Wl,-rpath,"$ASKAR_LIB_DIR"

# Check compilation result
if [ $? -eq 0 ]; then
    echo "✅ Compilation successful!"
    echo "📦 Generated: $OUTPUT_FILE"
    
    # Show file info
    ls -lh "$OUTPUT_FILE"
    
    # Copy to resources if directory exists
    if [ -d "../resources" ]; then
        cp "$OUTPUT_FILE" "../resources/"
        echo "📋 Copied to resources directory"
    fi
    
    echo ""
    echo "🎉 Build completed successfully!"
    echo "💡 To use: java -Djava.library.path=/path/to/this/directory YourClass"
    
else
    echo "❌ Compilation failed!"
    exit 1
fi
