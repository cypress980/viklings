# Answer to q001: Maven build failing with "package does not exist" error

## Root Cause
Maven dependencies didn't download properly. This commonly happens on first setup or with network issues.

## Solution
Try these steps in order:

### Step 1: Force dependency download
```bash
mvn clean install -U
```
The `-U` flag forces Maven to check for updated dependencies.

### Step 2: If that fails, clear Maven cache
```bash
rm -rf ~/.m2/repository/org/lwjgl
mvn clean compile
```

### Step 3: Verify dependencies downloaded
Check that LWJGL jars exist:
```bash
ls ~/.m2/repository/org/lwjgl/
```
You should see directories for `lwjgl`, `lwjgl-glfw`, `lwjgl-opengl`, etc.

## Why This Happens
- Corporate firewalls can block Maven Central
- Incomplete downloads leave corrupted files
- Network timeouts during first setup

## Prevention
Always run `mvn clean install` (not just `compile`) on first setup to ensure all dependencies download completely.

## Follow-up
If this doesn't resolve it, create a new question with:
- Output of `mvn clean install -U -X` (debug mode)
- Contents of your `~/.m2/settings.xml` if it exists
- Whether you're behind a corporate firewall