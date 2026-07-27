# Ejecución de tests

Todos los comandos se corren desde la raíz del proyecto (`AutomationBackend/tests/`), con el venv activo.

4 carpetas físicas, pero solo `component/` y `e2e/` tienen lógica propia.
`smoke/` y `regression/` **reexportan por import** los tests que ya viven en
`component/` (nunca los reescriben) — por eso `pytest.ini` las excluye de
`testpaths`: correr `pytest` a secas ya es la suite exhaustiva completa, sin
duplicar llamadas reales a la API.

| Carpeta | Contenido | Cómo se ejecuta |
|---|---|---|
| `component/` | TODOS los casos de cada endpoint — 1 archivo por endpoint/TC | Archivo por archivo, o carpeta completa (`pytest` la incluye por defecto) |
| `smoke/` | Import del camino feliz crítico, 1 archivo por microservicio | `pytest tests/smoke/` |
| `regression/` | Import del camino feliz + alternos de negocio, 1 archivo por microservicio | `pytest tests/regression/` |
| `e2e/` | Flujos de negocio completos, código propio | Archivo por archivo (`pytest` la incluye por defecto) |

## Smoke (carpeta completa — gate rápido de deploy)

```bash
pytest tests/smoke/
```

## Regression (carpeta completa — regresión curada)

```bash
pytest tests/regression/
```

## Component (archivo por archivo — 1 endpoint, todos sus escenarios)

```bash
pytest tests/component/test_<KEY_SIN_GUIONES>_<funcion>.py
```

## E2E (archivo por archivo)

```bash
pytest tests/e2e/test_<flujo>_e2e.py
```

## Suite completa (component + e2e, sin duplicados)

```bash
pytest
```

## Reporte HTML (con historial, por carpeta física)

Cualquiera de los comandos anteriores genera un reporte HTML por defecto,
agrupado por la carpeta física que ejecutaste. Cada corrida crea un archivo
nuevo (no sobrescribe), con fecha y hora en el nombre:

```
tests/component/reports/test_SIM_TC_12_create_user_20260727_072525.html
tests/smoke/reports/test_smoke_users_manager_20260727_072527.html
tests/regression/reports/test_reg_users_manager_20260727_072536.html
```

Un test promovido a `smoke`/`regression` genera evidencia en 2 reportes
distintos si corriste ambas carpetas por separado — es la misma llamada real,
reportada desde 2 vistas. El nombre lleva `{archivo}_{fecha}_{hora}.html`
cuando el comando corrió un solo archivo; si corrió varios de la misma
carpeta, el nombre es solo `{fecha}_{hora}.html`.

Para desactivarlo, agrega `--no-html-report`:

```bash
pytest tests/smoke/ --no-html-report
```

## Salida en consola

Por defecto (`-s` en `pytest.ini`) se imprime en vivo, por cada test: el
request (body incluido), el response (status + body) y cada aserción con
`✔ SUCCESS ASSERTION` o `✘ FAILED ASSERTION`. Al iniciar cada archivo se
muestra además un arte de bienvenida.
