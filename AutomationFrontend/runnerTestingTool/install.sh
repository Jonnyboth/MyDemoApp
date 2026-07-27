#!/usr/bin/env bash
# =============================================================================
# Katalon Headless Runner — Script de instalación automática
#
# Uso:
#   bash install.sh --source /ruta/proyecto-fuente --target /ruta/proyecto-destino
#
# Opciones:
#   --source  Ruta al proyecto Katalon que YA tiene el runner instalado
#   --target  Ruta al proyecto Katalon destino (donde instalar)
#   --udid    Serial del dispositivo Android (adb devices)
#   --package Package de la app (ej: com.tu.empresa.app)
#   --activity Activity principal (ej: com.tu.empresa.app.MainActivity)
#   --android-version Versión Android del dispositivo (default: 14)
#   --dry-run Solo muestra qué haría, sin copiar nada
#
# Ejemplo:
#   bash install.sh \
#     --source "/Users/yo/testAndroid" \
#     --target "/Users/yo/nuevoProyecto" \
#     --udid ABC123XYZ \
#     --package com.miempresa.app \
#     --activity com.miempresa.app.MainActivity
# =============================================================================

set -euo pipefail

# ── Colores ───────────────────────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# ── Argumentos por defecto ────────────────────────────────────────────────────
SOURCE_DIR=""
TARGET_DIR=""
DEVICE_UDID=""
APP_PACKAGE=""
APP_ACTIVITY=""
ANDROID_VERSION="14"
DRY_RUN=false

# ── Parsear argumentos ────────────────────────────────────────────────────────
while [[ $# -gt 0 ]]; do
    case $1 in
        --source)       SOURCE_DIR="$2";       shift 2 ;;
        --target)       TARGET_DIR="$2";       shift 2 ;;
        --udid)         DEVICE_UDID="$2";      shift 2 ;;
        --package)      APP_PACKAGE="$2";      shift 2 ;;
        --activity)     APP_ACTIVITY="$2";     shift 2 ;;
        --android-version) ANDROID_VERSION="$2"; shift 2 ;;
        --dry-run)      DRY_RUN=true;          shift ;;
        *)
            echo -e "${RED}Argumento desconocido: $1${NC}"
            echo "Uso: bash install.sh --source /ruta/fuente --target /ruta/destino [opciones]"
            exit 1 ;;
    esac
done

# ── Banner ────────────────────────────────────────────────────────────────────
echo ""
echo -e "${BLUE}╔══════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║   Katalon Headless Runner — Instalador v1.0.0        ║${NC}"
echo -e "${BLUE}╚══════════════════════════════════════════════════════╝${NC}"
echo ""

# ── Validar argumentos obligatorios ──────────────────────────────────────────
if [[ -z "$SOURCE_DIR" ]]; then
    echo -e "${RED}❌ Error: --source es obligatorio${NC}"
    exit 1
fi
if [[ -z "$TARGET_DIR" ]]; then
    echo -e "${RED}❌ Error: --target es obligatorio${NC}"
    exit 1
fi
if [[ ! -d "$SOURCE_DIR/runner" ]]; then
    echo -e "${RED}❌ Error: no se encontró runner/ en: $SOURCE_DIR${NC}"
    exit 1
fi
if [[ ! -d "$TARGET_DIR" ]]; then
    echo -e "${RED}❌ Error: el directorio destino no existe: $TARGET_DIR${NC}"
    exit 1
fi

echo -e "${BLUE}Fuente:  ${NC}$SOURCE_DIR"
echo -e "${BLUE}Destino: ${NC}$TARGET_DIR"
echo ""

$DRY_RUN && echo -e "${YELLOW}⚠️  DRY RUN — solo mostrando acciones, sin ejecutar${NC}" && echo ""

# ── Función helper ────────────────────────────────────────────────────────────
run_cmd() {
    if $DRY_RUN; then
        echo -e "${YELLOW}[DRY] $*${NC}"
    else
        eval "$@"
    fi
}

# ── PASO 1: Copiar runner/ ────────────────────────────────────────────────────
echo -e "${BLUE}▶ PASO 1: Copiando runner/...${NC}"

if [[ -d "$TARGET_DIR/runner" ]]; then
    echo -e "${YELLOW}⚠️  runner/ ya existe en destino — haciendo backup en runner.bak/${NC}"
    run_cmd "cp -r \"$TARGET_DIR/runner\" \"$TARGET_DIR/runner.bak\""
fi

run_cmd "cp -r \"$SOURCE_DIR/runner\" \"$TARGET_DIR/runner\""
echo -e "${GREEN}  ✅ runner/ copiado${NC}"

# ── PASO 2: Configurar runner.yml ─────────────────────────────────────────────
echo ""
echo -e "${BLUE}▶ PASO 2: Configurando runner/config/runner.yml...${NC}"

RUNNER_YML="$TARGET_DIR/runner/config/runner.yml"

if [[ -n "$DEVICE_UDID" ]] || [[ -n "$APP_PACKAGE" ]] || [[ -n "$APP_ACTIVITY" ]]; then

    # Leer template y sustituir placeholders
    UDID_VALUE="${DEVICE_UDID:-TU_SERIAL_AQUI}"
    PKG_VALUE="${APP_PACKAGE:-com.tu.app}"
    ACT_VALUE="${APP_ACTIVITY:-com.tu.app.MainActivity}"

    run_cmd "cat > \"$RUNNER_YML\" << 'YAML_EOF'
# ─────────────────────────────────────────────────────────────────────────────
# Katalon Headless Runner — Configuration
# ─────────────────────────────────────────────────────────────────────────────

appium:
  url: http://localhost:4723
  newCommandTimeout: 300

device:
  udid: $UDID_VALUE
  platformName: Android
  platformVersion: \"$ANDROID_VERSION\"
  automationName: UiAutomator2
  appPackage: $PKG_VALUE
  appActivity: $ACT_VALUE
  noReset: true
  fullReset: false
  autoGrantPermissions: true

runner:
  reportDir: runner/reports
  screenshotOnFailure: true
  retryOnFailure: 0
  defaultTimeout: 15
YAML_EOF"

    echo -e "${GREEN}  ✅ runner.yml configurado con datos del dispositivo${NC}"
    [[ -n "$DEVICE_UDID" ]]   && echo -e "     udid:        ${DEVICE_UDID}"
    [[ -n "$APP_PACKAGE" ]]   && echo -e "     appPackage:  ${APP_PACKAGE}"
    [[ -n "$APP_ACTIVITY" ]]  && echo -e "     appActivity: ${APP_ACTIVITY}"
else
    echo -e "${YELLOW}  ⚠️  No se pasaron --udid/--package/--activity — editar runner/config/runner.yml manualmente${NC}"
fi

# ── PASO 3: Permisos de ejecución ─────────────────────────────────────────────
echo ""
echo -e "${BLUE}▶ PASO 3: Ajustando permisos...${NC}"
run_cmd "chmod +x \"$TARGET_DIR/runner/run.sh\""
echo -e "${GREEN}  ✅ run.sh ejecutable${NC}"

# ── PASO 4: Verificar Profiles/default.glbl ──────────────────────────────────
echo ""
echo -e "${BLUE}▶ PASO 4: Verificando Profiles/default.glbl...${NC}"
GLBL="$TARGET_DIR/Profiles/default.glbl"

if [[ ! -f "$GLBL" ]]; then
    echo -e "${YELLOW}  ⚠️  Profiles/default.glbl no encontrado — creando archivo mínimo...${NC}"
    UDID_GLBL="${DEVICE_UDID:-TU_SERIAL_AQUI}"
    PKG_GLBL="${APP_PACKAGE:-com.tu.app}"

    run_cmd "mkdir -p \"$TARGET_DIR/Profiles\""
    run_cmd "cat > \"$GLBL\" << 'XML_EOF'
<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<GlobalVariableEntities>
   <description></description>
   <name>default</name>
   <defaultProfile>true</defaultProfile>
   <globalVariableEntities>
      <GlobalVariableEntity>
         <description></description>
         <initValue>'android'</initValue>
         <name>G_Platform</name>
      </GlobalVariableEntity>
      <GlobalVariableEntity>
         <description></description>
         <initValue>'$UDID_GLBL'</initValue>
         <name>G_DevicesName</name>
      </GlobalVariableEntity>
      <GlobalVariableEntity>
         <description></description>
         <initValue>'$PKG_GLBL'</initValue>
         <name>G_AppBundleID</name>
      </GlobalVariableEntity>
   </globalVariableEntities>
</GlobalVariableEntities>
XML_EOF"
    echo -e "${GREEN}  ✅ Profiles/default.glbl creado${NC}"
else
    echo -e "${GREEN}  ✅ Profiles/default.glbl encontrado${NC}"
fi

# ── PASO 5: Verificar prerequisitos del sistema ───────────────────────────────
echo ""
echo -e "${BLUE}▶ PASO 5: Verificando prerequisitos del sistema...${NC}"

check_cmd() {
    local cmd=$1
    local label=$2
    if command -v "$cmd" &>/dev/null; then
        echo -e "${GREEN}  ✅ $label encontrado: $(command -v "$cmd")${NC}"
    else
        echo -e "${YELLOW}  ⚠️  $label NO encontrado — instalar antes de ejecutar tests${NC}"
    fi
}

check_cmd java   "Java"
check_cmd gradle "Gradle"
check_cmd adb    "adb (Android SDK)"
check_cmd appium "Appium"

# Verificar dispositivo si se pasó udid
if [[ -n "$DEVICE_UDID" ]]; then
    if adb devices 2>/dev/null | grep -q "$DEVICE_UDID"; then
        echo -e "${GREEN}  ✅ Dispositivo $DEVICE_UDID conectado${NC}"
    else
        echo -e "${YELLOW}  ⚠️  Dispositivo $DEVICE_UDID NO detectado por adb — conectar antes de ejecutar${NC}"
    fi
fi

# ── RESUMEN FINAL ─────────────────────────────────────────────────────────────
echo ""
echo -e "${BLUE}════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}  ✅ Instalación completada${NC}"
echo -e "${BLUE}════════════════════════════════════════════════════════${NC}"
echo ""
echo "Próximos pasos:"
echo ""
echo -e "  1. ${YELLOW}Editar runner/config/runner.yml${NC} si no pasaste --udid/--package"
echo -e "  2. ${YELLOW}Iniciar Appium:${NC} appium server --port 4723 &"
echo -e "  3. ${YELLOW}Listar tests disponibles:${NC}"
echo -e "     cd \"$TARGET_DIR\""
echo -e "     bash runner/run.sh list"
echo ""
echo -e "  4. ${YELLOW}Ejecutar un test:${NC}"
echo -e "     bash runner/run.sh TC_NombreDelTest"
echo ""
echo -e "  Reportes en: ${YELLOW}runner/reports/${NC}"
echo ""
