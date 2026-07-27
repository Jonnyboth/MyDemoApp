#!/usr/bin/env bash
# Analiza el árbol de directorios del repo (existente + lo que queda staged en este commit)
# y aborta el commit si detecta:
#   1) Carpetas hermanas que solo difieren en mayúsculas/minúsculas (case-insensitive).
#      En Linux/CI ambas coexisten sin problema, pero al clonar en Windows/macOS
#      (filesystem case-insensitive por defecto) colisionan y se pierden/corrompen archivos.
#   2) Carpetas NUEVAS que violan la convención de nombres del proyecto.
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root"

# Carpetas de nivel superior que Katalon Studio genera con nombre fijo (algunas con espacios)
# y que no se pueden renombrar sin romper el IDE. Quedan exentas de la convención estricta.
allowlist_regex='^(Test Cases|Test Suites|Test Listeners|Object Repository|Data Files|Include|Keywords|Scripts|Drivers|Plugins|Profiles|Reports|Checkpoints|Libs)$'

# Convención global para carpetas nuevas en cualquier parte del repo: sin espacios ni
# caracteres especiales (deja pasar letras, números, punto, guion y guion bajo).
global_naming_regex='^[A-Za-z0-9._-]+$'

# Convención estricta para carpetas nuevas dentro del proyecto Node.js web (kebab-case).
web_keywords_naming_regex='^[a-z0-9]+(-[a-z0-9]+)*$'

fail=0

# --- Árbol conocido: directorios ya trackeados en HEAD ---
existing_dirs=""
if git rev-parse --verify -q HEAD >/dev/null; then
  existing_dirs="$(git ls-tree -r -d --name-only HEAD || true)"
fi

# --- Directorios que implica el commit actual (archivos en el índice) ---
staged_files="$(git diff --cached --name-only --diff-filter=ACMR || true)"
staged_dirs="$(
  echo "$staged_files" | awk -F/ '
    NF > 1 {
      out = ""
      for (i = 1; i < NF; i++) {
        out = (out == "" ? $i : out "/" $i)
        print out
      }
    }
  '
)"

all_dirs="$(printf '%s\n%s\n' "$existing_dirs" "$staged_dirs" | sed '/^$/d' | sort -u)"

if [ -z "$all_dirs" ]; then
  exit 0
fi

# --- 1) Colisiones case-insensitive entre carpetas hermanas ---
collisions="$(
  echo "$all_dirs" | awk -F/ '
    {
      n = NF
      base = $n
      parent = ""
      for (i = 1; i < n; i++) parent = (parent == "" ? $i : parent "/" $i)
      key = parent "\t" tolower(base)
      full = ($0)
      if (key in seen && seen[key] != full) {
        print "   " seen[key] "  <->  " full
      }
      seen[key] = full
    }
  '
)"

if [ -n "$collisions" ]; then
  echo "Conflicto de nombres de carpeta (case-insensitive):"
  echo "$collisions"
  echo "Esto rompe checkouts en Windows/macOS y puede corromper el árbol en CI."
  fail=1
fi

# --- 2) Convención de nombres para carpetas NUEVAS (no existían en HEAD) ---
new_dirs="$(comm -13 <(echo "$existing_dirs" | sed '/^$/d' | sort -u) <(echo "$staged_dirs" | sed '/^$/d' | sort -u))"

while IFS= read -r dir; do
  [ -z "$dir" ] && continue
  base="$(basename "$dir")"

  if [[ "$base" =~ $allowlist_regex ]]; then
    continue
  fi

  case "$dir" in
    WebKeywords/*)
      if ! [[ "$base" =~ $web_keywords_naming_regex ]]; then
        echo "La carpeta \"$dir\" no cumple la convención kebab-case del proyecto Node.js web (minúsculas, números y '-')."
        fail=1
      fi
      ;;
    *)
      if ! [[ "$base" =~ $global_naming_regex ]]; then
        echo "La carpeta \"$dir\" no cumple la convención de nombres (sin espacios ni caracteres especiales)."
        fail=1
      fi
      ;;
  esac
done <<< "$new_dirs"

if [ "$fail" -eq 1 ]; then
  echo ""
  echo "Commit abortado por conflictos en la estructura de directorios."
  exit 1
fi

exit 0
