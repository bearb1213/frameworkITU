#!/bin/bash

# Simple build script
SRC_DIR="src/main/java"
LIB_DIR="lib"
BUILD_DIR="build"

echo "Building project..."

# Clean
rm -rf $BUILD_DIR
mkdir -p $BUILD_DIR

# Find libraries
if [ -d "$LIB_DIR" ]; then
    CLASSPATH=$(find $LIB_DIR -name "*.jar" | tr '\n' ':')
else
    CLASSPATH=""
fi

# Compile
find $SRC_DIR -name "*.java" | xargs javac -d $BUILD_DIR -cp $CLASSPATH

if [ $? -eq 0 ]; then
    echo "Build successful!"
    echo "Classes compiled to: $BUILD_DIR"
else
    echo "Build failed!"
    exit 1
fi