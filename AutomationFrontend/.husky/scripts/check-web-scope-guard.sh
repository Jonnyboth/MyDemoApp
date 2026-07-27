#!/usr/bin/env bash
# Este proyecto automatiza la app Android (Katalon Studio + Appium). WebKeywords/ es un
# scaffold web inactivo que NO debe crecer solo (ver AutomationFrontend/CLAUDE.md).
# Este hook bloquea el commit si package.json agrega una dependencia de automatización
# WEB que no estaba ya en el último commit — red de seguridad mecánica contra scope
# creep silencioso, no un reemplazo de leer CLAUDE.md antes de tocar WebKeywords/.
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root"

web_packages_regex='^(playwright|@playwright/test|cypress|puppeteer|puppeteer-core|webdriverio|selenium-webdriver|testcafe|nightwatch|taiko)$'

list_deps() {
  node -e '
    let raw = "";
    process.stdin.on("data", (d) => (raw += d));
    process.stdin.on("end", () => {
      try {
        const pkg = JSON.parse(raw);
        const names = [
          ...Object.keys(pkg.dependencies || {}),
          ...Object.keys(pkg.devDependencies || {}),
        ];
        console.log(names.join("\n"));
      } catch (e) {
        // package.json invalido o vacio: nada que listar.
      }
    });
  '
}

fail=0
staged_pkg_files="$(git diff --cached --name-only --diff-filter=ACMR -- '*package.json' || true)"

for f in $staged_pkg_files; do
  staged_deps="$(git show ":$f" 2>/dev/null | list_deps || true)"

  head_deps=""
  if git rev-parse --verify -q HEAD >/dev/null 2>&1 && git cat-file -e "HEAD:$f" 2>/dev/null; then
    head_deps="$(git show "HEAD:$f" | list_deps || true)"
  fi

  new_deps="$(comm -13 <(echo "$head_deps" | sed '/^$/d' | sort -u) <(echo "$staged_deps" | sed '/^$/d' | sort -u))"

  while IFS= read -r dep; do
    [ -z "$dep" ] && continue
    if [[ "$dep" =~ $web_packages_regex ]]; then
      echo "\"$dep\" es una dependencia de automatización WEB nueva en $f."
      echo "Este proyecto automatiza la app Android (Katalon + Appium). Si el usuario"
      echo "pidió automatización web de forma EXPLÍCITA en esta conversación, repite el"
      echo "commit con:"
      echo "  ALLOW_WEB_AUTOMATION_DEPS=1 git commit ..."
      fail=1
    fi
  done <<< "$new_deps"
done

if [ "$fail" -eq 1 ] && [ "${ALLOW_WEB_AUTOMATION_DEPS:-0}" != "1" ]; then
  echo ""
  echo "Commit abortado: dependencia de automatización web nueva sin confirmar."
  exit 1
fi

exit 0
