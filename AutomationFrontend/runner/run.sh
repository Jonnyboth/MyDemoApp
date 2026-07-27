#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# Katalon Headless Runner — CLI entry point
# Usage:
#   ./run.sh run --case android/TC_CompraProductosTurboDev
#   ./run.sh run --tag smoke
#   ./run.sh run --all
#   ./run.sh list
# ─────────────────────────────────────────────────────────────────────────────

set -euo pipefail

RUNNER_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$RUNNER_DIR")"
JAR="$RUNNER_DIR/build/libs/runner-all.jar"

# ── Build if needed ──────────────────────────────────────────────────────────
needs_build() {
    [ ! -f "$JAR" ] && return 0
    # Rebuild if any source file is newer than the jar
    find "$RUNNER_DIR/src" -name '*.groovy' -newer "$JAR" | grep -q . && return 0
    return 1
}

if needs_build; then
    echo "Building runner..."
    cd "$RUNNER_DIR"

    # Buscar gradle en ubicaciones conocidas
    GRADLE_BIN=""
    for candidate in \
        "$(command -v gradle 2>/dev/null)" \
        "$HOME/.gradle/wrapper/dists/gradle-8.9-bin/90cnw93cvbtalezasaz0blq0a/gradle-8.9/bin/gradle" \
        "$HOME/.gradle/wrapper/dists/gradle-8.14-bin/38aieal9i53h9rfe7vjup95b9/gradle-8.14/bin/gradle" \
        "/opt/homebrew/bin/gradle" \
        "/usr/local/bin/gradle"; do
        if [ -x "$candidate" ]; then
            GRADLE_BIN="$candidate"
            break
        fi
    done

    if [ -z "$GRADLE_BIN" ]; then
        echo "Error: 'gradle' no encontrado."
        echo "Instalar con: brew install gradle   (macOS)"
        exit 1
    fi

    "$GRADLE_BIN" shadowJar -q
    cd - > /dev/null
fi

# ── Android SDK — apunta al SDK de Katalon si ANDROID_HOME no está definido ──
if [ -z "${ANDROID_HOME:-}" ] || [ ! -d "${ANDROID_HOME}" ]; then
    KATALON_SDK="$HOME/.katalon/tools/android_sdk"
    if [ -d "$KATALON_SDK" ]; then
        export ANDROID_HOME="$KATALON_SDK"
        export ANDROID_SDK_ROOT="$KATALON_SDK"
    fi
fi

# ── Run ──────────────────────────────────────────────────────────────────────
exec java \
    -Drunner.projectDir="$PROJECT_DIR" \
    -Drunner.configFile="$RUNNER_DIR/config/runner.yml" \
    -jar "$JAR" \
    "$@"
