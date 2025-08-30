# q001-example-question.md

## Question: Maven build failing with "package does not exist" error

### What I'm trying to do:
Following DEV-001 ticket, trying to compile the project with `mvn clean compile`

### What went wrong:
Build fails with compilation errors saying LWJGL packages don't exist

### Error message (if any):
```
[ERROR] /Users/agent/projects/viklings/src/main/java/engine/GameWindow.java:[10,8] package org.lwjgl.glfw does not exist
[ERROR] /Users/agent/projects/viklings/src/main/java/engine/GameWindow.java:[11,8] package org.lwjgl.opengl does not exist
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.8.1:compile
```

### What I've already tried:
- Verified Java 8 is active with `java -version` (shows 1.8.0_392)
- Checked internet connection is working
- Ran `mvn clean` first
- Verified I'm in the correct directory (/Users/agent/projects/viklings)

### My environment:
- **Operating System**: macOS 14.1
- **Java version**: openjdk version "1.8.0_392"
- **Maven version**: Apache Maven 3.9.5 (Java version: 1.8.0_392)
- **IDE**: IntelliJ IDEA 2023.2
- **Working directory**: /Users/agent/projects/viklings

### Related files/context:
Working on DEV-001 development environment setup ticket, specifically Step 1: Clone and Build

### Additional context:
This is my first time setting up LWJGL. I can see the pom.xml file exists and looks correct.