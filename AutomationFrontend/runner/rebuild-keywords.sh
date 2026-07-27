#!/usr/bin/env bash
# rebuild-keywords.sh
# Regenera bin/keyword/, bin/listener/ y bin/groovy/ usando el compilador
# groovyc embebido en runner-all.jar. Recovery cuando bin/keyword/ quedó
# corrupto o ausente y Katalon Studio reporta cascada de "unable to resolve class".
#
# Uso:
#   bash runner/rebuild-keywords.sh
#
# Tras correrlo, hacer Project → Refresh (F5) en Katalon Studio.

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_ROOT"

KATALON_PLUGINS="/Applications/Katalon Studio.app/Contents/Eclipse/plugins"
RUNNER_JAR="$PROJECT_ROOT/runner/build/libs/runner-all.jar"

if [ ! -f "$RUNNER_JAR" ]; then
  echo "ERROR: $RUNNER_JAR no existe. Compila el runner primero (cd runner && gradle shadowJar)."
  exit 1
fi
if [ ! -d "$KATALON_PLUGINS" ]; then
  echo "ERROR: $KATALON_PLUGINS no existe. ¿Katalon Studio instalado?"
  exit 1
fi

# Classpath: runner-all (incluye groovyc + groovy + appium) + plugins Katalon + Libs/Plugins del proyecto
CP="$RUNNER_JAR"
[ -d Libs ] && CP="$CP:$(find Libs -name '*.jar' 2>/dev/null | tr '\n' ':')"
[ -d Plugins ] && CP="$CP:$(find Plugins -name '*.jar' 2>/dev/null | tr '\n' ':')"
CP="$CP:$(find "$KATALON_PLUGINS" -name '*.jar' 2>/dev/null | tr '\n' ':')"
CP="$CP:Keywords:Include/scripts/groovy:Test Listeners:Libs"

mkdir -p bin/keyword bin/listener bin/groovy bin/lib

echo "→ Compiling Keywords/ ..."
KEYWORDS=$(find Keywords -name '*.groovy' 2>/dev/null)
if [ -n "$KEYWORDS" ]; then
  java -cp "$RUNNER_JAR" org.codehaus.groovy.tools.FileSystemCompiler \
    -cp "$CP" -d bin/keyword $KEYWORDS
fi

echo "→ Compiling Test Listeners/ ..."
LISTENERS=$(find "Test Listeners" -name '*.groovy' 2>/dev/null || true)
if [ -n "$LISTENERS" ]; then
  java -cp "$RUNNER_JAR" org.codehaus.groovy.tools.FileSystemCompiler \
    -cp "$CP:bin/keyword" -d bin/listener $LISTENERS
fi

echo "→ Compiling Include/scripts/groovy/ ..."
INCLUDES=$(find Include/scripts/groovy -name '*.groovy' 2>/dev/null || true)
if [ -n "$INCLUDES" ]; then
  java -cp "$RUNNER_JAR" org.codehaus.groovy.tools.FileSystemCompiler \
    -cp "$CP:bin/keyword" -d bin/groovy $INCLUDES
fi

echo "→ Compiling Libs/ (incluye internal/GlobalVariable, CustomKeywords) ..."
LIBS=$(find Libs -name '*.groovy' 2>/dev/null || true)
if [ -n "$LIBS" ]; then
  java -cp "$RUNNER_JAR" org.codehaus.groovy.tools.FileSystemCompiler \
    -cp "$CP:bin/keyword" -d bin/lib $LIBS
fi

KW_COUNT=$(find bin/keyword -name '*.class' 2>/dev/null | wc -l | tr -d ' ')
echo "✓ Done. $KW_COUNT .class files en bin/keyword/"
echo "→ Ahora hacé Project → Refresh (F5) en Katalon Studio."
