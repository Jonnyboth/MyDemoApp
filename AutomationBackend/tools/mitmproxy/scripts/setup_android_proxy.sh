#!/usr/bin/env bash
# Configura un emulador/dispositivo Android para enrutar su tráfico a mitmproxy.
# Uso: setup_android_proxy.sh [serial] [proxy_host:proxy_port]
#   serial          por defecto: el único dispositivo conectado (adb devices)
#   proxy_host:port por defecto: 10.0.2.2:8080 (10.0.2.2 = alias al host desde un AVD estándar;
#                    para un dispositivo físico en la misma red usa la IP LAN del host)
set -euo pipefail

SERIAL="${1:-}"
PROXY="${2:-10.0.2.2:8080}"
ADB="adb"
[[ -n "$SERIAL" ]] && ADB="adb -s $SERIAL"

echo "==> Configurando proxy global -> ${PROXY}"
$ADB shell settings put global http_proxy "$PROXY"
echo "==> Proxy actual en el dispositivo:"
$ADB shell settings get global http_proxy

CERT_PEM="$HOME/.mitmproxy/mitmproxy-ca-cert.pem"
CERT_CER="$HOME/.mitmproxy/mitmproxy-ca-cert.cer"

if [[ ! -f "$CERT_PEM" ]]; then
  echo
  echo "AVISO: no existe ${CERT_PEM}. Arranca mitmproxy/mitmweb al menos una vez para generarlo"
  echo "       antes de intentar instalar el certificado CA."
  exit 0
fi

echo
echo "==> Intentando instalar el CA como certificado de SISTEMA (requiere -writable-system)..."
if $ADB root >/dev/null 2>&1 && $ADB remount >/dev/null 2>&1; then
  HASH=$(openssl x509 -inform PEM -subject_hash_old -in "$CERT_PEM" | head -1)
  DEST="/system/etc/security/cacerts/${HASH}.0"
  $ADB push "$CERT_PEM" "$DEST"
  $ADB shell chmod 644 "$DEST"
  echo "==> Certificado instalado como CA de sistema (${DEST}). Reinicia el dispositivo:"
  echo "    ${ADB} reboot"
else
  echo "==> No se pudo remount /system (el AVD no fue lanzado con -writable-system)."
  echo "    Alternativa sin reiniciar: instalar como CA de usuario."
  if [[ -f "$CERT_CER" ]]; then
    $ADB push "$CERT_CER" /sdcard/Download/mitmproxy-ca-cert.cer
    echo "==> Certificado copiado a /sdcard/Download/mitmproxy-ca-cert.cer"
    echo "    En el dispositivo: Ajustes > Seguridad > Cifrado y credenciales >"
    echo "    Instalar un certificado > CA certificate (puede pedir configurar PIN/patrón antes)."
  fi
fi

echo
echo "Listo. Deja mitmweb/mitmdump corriendo en el host y abre la app en el dispositivo."
