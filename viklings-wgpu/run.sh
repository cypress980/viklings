#!/bin/bash

# Simple script to run the game with different log levels

case "$1" in
    "quiet"|"error")
        echo "Running with minimal logging (errors only)..."
        RUST_LOG=error cargo run
        ;;
    "info"|"")
        echo "Running with standard logging (info + errors)..."
        RUST_LOG=info cargo run
        ;;
    "debug"|"verbose")
        echo "Running with verbose logging (debug + info + errors)..."
        RUST_LOG=debug cargo run
        ;;
    "trace"|"all")
        echo "Running with maximum logging (all levels)..."
        RUST_LOG=trace cargo run
        ;;
    *)
        echo "Usage: $0 [quiet|info|debug|trace]"
        echo "  quiet: Errors only"
        echo "  info:  Standard logging (default)"
        echo "  debug: Verbose logging"
        echo "  trace: Maximum logging"
        exit 1
        ;;
esac