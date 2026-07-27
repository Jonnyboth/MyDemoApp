# Casos de Prueba — proyecto SIM (AIO Tests)

> **Estado: ✅ subidos.** Los 11 Casos de Prueba de `SIM_test_cases.json` ya fueron creados en
> AIO Tests el 2026-07-25 (`SIM-TC-1` a `SIM-TC-11`) tras recibir un `AIO_API_TOKEN` válido.
> Este archivo se conserva como fuente/histórico — el script `create_all.py` es idempotente en
> el sentido de que puede reutilizarse para volver a crear casos similares, pero **no** hace
> update ni evita duplicados si se vuelve a correr sobre los mismos títulos.

Este archivo (`SIM_test_cases.json`) contiene los 11 Casos de Prueba redactados siguiendo el
estándar de [`formatting-rules.md`](../../../.prompts/skill_qa_engineer/references/formatting-rules.md),
para las 4 Historias de Usuario creadas en Jira (`SIM-5` a `SIM-8`).

## Contexto del bloqueo original (ya resuelto)

`AIO_API_TOKEN` en el `.env` global (`~/.config/autosquad-ai/global.env`) había sido **purgado
en la auditoría de seguridad del 2026-07-18** y no se había regenerado — contenía el
placeholder literal `"ingresesuToken de AIO"`, confirmado con `HTTP 401` en vivo. El usuario
proveyó un token nuevo y la carga se ejecutó exitosamente.

## Cómo volver a ejecutar el script (si hace falta)

1. Genera un token nuevo en https://tcms.aiojiraapps.com/aio-tcms y reemplaza el placeholder
   en `~/.config/autosquad-ai/global.env` (`AIO_API_TOKEN=...`).
2. Ejecuta el script de carga masiva desde la raíz del repo:

   ```bash
   cd FuncionalQaPm
   .prompts/skill_qa_engineer/.venv/bin/python3 docs/QaExplorer/pending_aio_test_cases/create_all.py
   ```

   Esto crea los 11 TCs uno por uno contra el proyecto `SIM`, imprimiendo el ID/key real
   devuelto por AIO Tests para cada uno (o el error tal cual, sin asumir éxito).

3. Alternativa manual (uno por uno, útil para revisar antes de subir):
   ```bash
   .prompts/skill_qa_engineer/.venv/bin/python3 .prompts/skill_qa_engineer/aio_tests_client.py \
     --project-key SIM create --json-file <(python3 -c "import json,sys; json.dump(json.load(open('docs/QaExplorer/pending_aio_test_cases/SIM_test_cases.json'))[0], sys.stdout)")
   ```

## Cobertura

| # | HU relacionada | Título | Tipo |
|---|---|---|---|
| 1 | SIM-5 (Checkout) | Envío exitoso con todos los campos completos | Positivo |
| 2 | SIM-5 (Checkout) | Validación de campos obligatorios vacíos | Negativo |
| 3 | SIM-5 (Checkout) | Zip Code debe recibir foco y ser editable | Regresión (bug SIM-10) |
| 4 | SIM-6 (Login) | Login exitoso con credenciales válidas | Positivo |
| 5 | SIM-6 (Login) | Bloqueo de cuenta locked out | Negativo |
| 6 | SIM-6 (Login) | Validación de Username obligatorio | Borde |
| 7 | SIM-7 (Catálogo) | Ordenar por precio descendente | Regresión (bug SIM-11) |
| 8 | SIM-7 (Catálogo) | Navegar a detalle tras reordenar/scroll | Regresión (bug SIM-9, crítico) |
| 9 | SIM-8 (Carrito) | Imagen refleja el color seleccionado | Regresión (bug SIM-12) |
| 10 | SIM-8 (Carrito) | Color correcto al agregar al carrito | Positivo |
| 11 | SIM-8 (Carrito) | Gramática singular "1 Item" | Regresión (bug SIM-13) |

Todos con `customFields.Testing Layers = "Android"` (según lo indicado por el usuario) y
`labels` incluyendo `created_by_ai` + el key de la HU relacionada, conforme a la Regla 8 de
formatting-rules.md.

**No se ejecutó el triage anti-duplicados** ([`triage.md`](../../../.prompts/skill_qa_engineer/triage.md))
contra `SIM` porque el mismo bloqueo de token impide consultar `search_test_cases` — ejecutarlo
antes de subir estos TCs si el proyecto `SIM` ya tuviera Casos de Prueba previos.
