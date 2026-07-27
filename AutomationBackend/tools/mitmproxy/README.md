# mitmproxy — Captura de tráfico para extraer endpoints de apps móviles

Herramienta de apoyo para `AutomationBackend`: usa [mitmproxy](https://www.mitmproxy.org/) para
interceptar el tráfico HTTP(S) de una app Android corriendo en un emulador/dispositivo y así
descubrir los endpoints reales que consume, como insumo para [`skill_api_test_designer`](../../.prompts/skill_api_test_designer/system.md).

## Estado de la instalación en esta máquina

mitmproxy **ya está instalado globalmente** en un venv dedicado:

```
~/.venvs/mitmproxy/          # venv con mitmproxy 12.2.3
~/.local/bin/mitmproxy       # symlinks al venv
~/.local/bin/mitmweb
~/.local/bin/mitmdump
```

No hace falta reinstalarlo. Los certificados CA se generan la primera vez que corre (`~/.mitmproxy/`).

## Flujo de trabajo (Android, emulador AVD)

### 1. Levantar el proxy

```bash
mitmweb --listen-host 0.0.0.0 --listen-port 8080 \
        --web-host 127.0.0.1 --web-port 8081 --no-web-open-browser \
        -w tools/mitmproxy/captures/session_$(date +%Y%m%d_%H%M%S).flow &
```

- Proxy en `0.0.0.0:8080` (debe aceptar conexiones desde fuera de `127.0.0.1` para que el emulador llegue).
- UI web en `http://127.0.0.1:8081` — la primera carga pide un `?token=...`; ese token se imprime en el log de arranque de `mitmweb` (`Web server listening at http://127.0.0.1:8081/?token=...`), ábrelo con ese link exacto desde un navegador en el host.
- `-w` guarda cada flujo en disco de forma incremental — es lo que después parsea `scripts/extract_endpoints.py`.

### 2. Apuntar el emulador al proxy

Desde un **AVD** (Android Emulator estándar, no Genymotion), el host es siempre `10.0.2.2`:

```bash
adb -s <serial> shell settings put global http_proxy 10.0.2.2:8080
```

No requiere root. Para revertir:

```bash
adb -s <serial> shell settings put global http_proxy :0
```

Scripts listos: [`scripts/setup_android_proxy.sh`](scripts/setup_android_proxy.sh) / [`scripts/teardown_android_proxy.sh`](scripts/teardown_android_proxy.sh).

### 3. Confiar el certificado CA de mitmproxy (necesario para HTTPS)

Sin este paso, el tráfico **HTTP plano se ve igual** (confirmado, ver más abajo), pero todo **HTTPS falla el handshake TLS** (`Client TLS handshake failed... does not trust the proxy's certificate`) y no se puede leer el contenido.

Dos caminos, según si el emulador fue lanzado con `-writable-system`:

**a) CA de sistema (recomendado si es posible — funciona con más apps, incluidas las que restringen su Network Security Config a CAs de sistema):**

```bash
adb -s <serial> root
adb -s <serial> remount   # falla con "Device must be bootloader unlocked" si el AVD
                          # no se lanzó con -writable-system; en ese caso hay que
                          # relanzarlo: emulator @<avd> -writable-system
HASH=$(openssl x509 -inform PEM -subject_hash_old -in ~/.mitmproxy/mitmproxy-ca-cert.pem | head -1)
adb -s <serial> push ~/.mitmproxy/mitmproxy-ca-cert.pem "/system/etc/security/cacerts/${HASH}.0"
adb -s <serial> shell chmod 644 "/system/etc/security/cacerts/${HASH}.0"
adb -s <serial> reboot
```

**b) CA de usuario vía Settings (no requiere reiniciar el emulador, pero exige tener PIN/patrón configurado en el dispositivo y solo cubre apps que confían en CAs de usuario):**

```bash
adb -s <serial> push ~/.mitmproxy/mitmproxy-ca-cert.cer /sdcard/Download/mitmproxy-ca-cert.cer
# En el dispositivo: Ajustes > Seguridad > Cifrado y credenciales > Instalar un certificado > CA certificate
# → elegir el archivo en Download → aceptar la advertencia.
```

### 4. Interactuar con la app y revisar la captura

Navega la app (login, catálogo, acciones que quieras capturar). Para inspeccionar lo capturado sin pelear con el token HTTP de la web UI, es más simple leer el archivo `.flow` directo con la librería de mitmproxy:

```bash
~/.venvs/mitmproxy/bin/python3 tools/mitmproxy/scripts/extract_endpoints.py tools/mitmproxy/captures/<archivo>.flow
```

Esto imprime cada endpoint único (`METODO host/path`) y, si el flujo tiene body, un `curl` reconstruido — listo para pasarle a `skill_api_test_designer`.

### 5. Apagar

```bash
tools/mitmproxy/scripts/teardown_android_proxy.sh <serial>   # quita el proxy del dispositivo
pkill -f mitmweb                                              # si quieres parar el proxy en el host
```

## Verificación hecha en esta sesión (2026-07-26, emulator-5554, `qa android`, Android 14)

- `adb remount` falló con `Device must be bootloader unlocked` → este AVD no se lanzó con `-writable-system`, así que la instalación de CA de sistema no fue posible sin relanzarlo (se optó por no relanzar para no perder el estado de la app).
- Proxy global configurado (`10.0.2.2:8080`) y **validado end-to-end con Chrome**: `GET http://example.com/` se capturó completo (body incluido); las conexiones HTTPS de Chrome (`www.google.com`, `accounts.google.com`, etc.) llegaron al proxy pero fallaron el handshake TLS por certificado no confiable — es decir, el proxy funciona, falta solo el paso de confiar el CA (paso 3) para decodificar HTTPS.
- Se navegó **login completo + catálogo completo** de `com.saucelabs.mydemoapp.android` (la app "MyDemoApp" instalada en el emulador) con el proxy activo: **0 flujos capturados**. Esta app es la conocida app de demo open-source de Sauce Labs — el catálogo y el login son enteramente locales/offline (de hecho la pantalla de login muestra las credenciales de prueba hardcodeadas en pantalla). No expone ningún backend real, por lo que no existen endpoints de login/productos/PUT/DELETE que extraer de esta app en particular.
- Conclusión práctica: si se necesita practicar extracción de endpoints reales, hace falta apuntar esta herramienta a una app que sí tenga backend (una build propia, u otra app instalada), no a `com.saucelabs.mydemoapp.android`.
