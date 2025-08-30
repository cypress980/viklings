#!/bin/bash

# macOS LWJGL Launch Script
# This script includes the necessary JVM arguments for running LWJGL applications on macOS

java -XstartOnFirstThread \
     -Djava.awt.headless=false \
     -Dorg.lwjgl.util.DebugLoader=true \
     -Dorg.lwjgl.util.Debug=true \
     -cp target/classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout) \
     viklings.prototype.ViklingsPrototype

echo "Game exited with code: $?"