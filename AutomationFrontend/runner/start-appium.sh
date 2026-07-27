#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# start-appium.sh — Lanza un servidor Appium standalone para el runner headless
#
# Uso:
#   bash runner/start-appium.sh           # foreground (Ctrl+C para detener)
#   bash runner/start-appium.sh --bg      # background, log en /tmp/appium-4723.log
#   bash runner/start-appium.sh --stop    # mata el Appium que escucha en el puerto
#
# Variables de entorno opcionales:
#   APPIUM_PORT       (default 4723)    — debe coincidir con runner.yml > appium.url
#   APPIUM_BASE_PATH  (default '/')     — el runner usa el path tal cual aparece en runner.yml
#   ANDROID_HOME      (default ~/.katalon/tools/android_sdk si existe, o ~/Library/Android/sdk)
# ─────────────────────────────────────────────────────────────────────────────

set -euo pipefail

PORT="${APPIUM_PORT:-4723}"
BASE_PATH="${APPIUM_BASE_PATH:-/}"
LOG_FILE="/tmp/appium-${PORT}.log"

# Resolver Android SDK
if [ -z "${ANDROID_HOME:-}" ] || [ ! -d "${ANDROID_HOME}" ]; then
    for candidate in \
        "$HOME/.katalon/tools/android_sdk" \
        "$HOME/Library/Android/sdk" \
        "/usr/local/share/android-sdk" \
        "/opt/android-sdk"; do
        if [ -d "$candidate" ]; then
            export ANDROID_HOME="$candidate"
            break
        fi
    done
fi

if [ -z "${ANDROID_HOME:-}" ] || [ ! -d "${ANDROID_HOME}" ]; then
    echo "❌ ANDROID_HOME no resuelto. Exporta la ruta al SDK de Android antes de continuar."
    echo "   Ej: export ANDROID_HOME=\"\$HOME/Library/Android/sdk\""
    exit 1
fi
export ANDROID_SDK_ROOT="$ANDROID_HOME"

# Resolver appium
APPIUM_BIN="$(command -v appium || true)"
if [ -z "$APPIUM_BIN" ]; then
    echo "❌ 'appium' no está en PATH. Instala con: npm install -g appium && appium driver install uiautomator2"
    exit 1
fi

case "${1:-}" in
    --stop)
        PID="$(lsof -tiTCP:"$PORT" -sTCP:LISTEN 2>/dev/null || true)"
        if [ -n "$PID" ]; then
            echo "Deteniendo Appium en puerto $PORT (pid=$PID)..."
            kill -9 $PID 2>/dev/null || true
            echo "✅ Appium detenido"
        else
            echo "ℹ️ No hay Appium escuchando en puerto $PORT"
        fi
        exit 0
        ;;
    --bg)
        if lsof -iTCP:"$PORT" -sTCP:LISTEN >/dev/null 2>&1; then
            echo "ℹ️ Appium ya está corriendo en puerto $PORT. (--stop para detenerlo)"
            exit 0
        fi
        echo "▶️ Lanzando Appium en background"
        echo "   ANDROID_HOME=$ANDROID_HOME"
        echo "   puerto=$PORT  basePath=$BASE_PATH"
        echo "   log=$LOG_FILE"
        nohup "$APPIUM_BIN" -p "$PORT" --base-path "$BASE_PATH" --log-level info >"$LOG_FILE" 2>&1 &
        sleep 5
        if lsof -iTCP:"$PORT" -sTCP:LISTEN >/dev/null 2>&1; then
            echo "✅ Appium escuchando en puerto $PORT (pid=$!)"
        else
            echo "❌ Appium no arrancó. Revisa $LOG_FILE"
            tail -20 "$LOG_FILE"
            exit 1
        fi
        ;;
    ""|--fg)
        if lsof -iTCP:"$PORT" -sTCP:LISTEN >/dev/null 2>&1; then
            echo "ℹ️ Appium ya está corriendo en puerto $PORT. (--stop para detenerlo)"
            exit 0
        fi
        echo "▶️ Lanzando Appium en foreground (Ctrl+C para detener)"
        echo "   ANDROID_HOME=$ANDROID_HOME"
        echo "   puerto=$PORT  basePath=$BASE_PATH"
        exec "$APPIUM_BIN" -p "$PORT" --base-path "$BASE_PATH" --log-level info
        ;;
    -h|--help)
        sed -n '2,15p' "$0"
        exit 0
        ;;
    *)
        echo "Uso: $0 [--bg|--fg|--stop|--help]"
        exit 2
        ;;
esac
