#!/usr/bin/env bash
# Quita el proxy global configurado por setup_android_proxy.sh.
# Uso: teardown_android_proxy.sh [serial]
set -euo pipefail

SERIAL="${1:-}"
ADB="adb"
[[ -n "$SERIAL" ]] && ADB="adb -s $SERIAL"

echo "==> Quitando proxy global del dispositivo..."
$ADB shell settings put global http_proxy :0
echo "==> Proxy actual:"
$ADB shell settings get global http_proxy

echo
echo "Nota: si se instaló el CA de mitmproxy como certificado de sistema"
echo "(/system/etc/security/cacerts), este script NO lo elimina. Bórralo manualmente"
echo "(adb shell rm /system/etc/security/cacerts/<hash>.0 + reboot) si quieres revertirlo del todo."
