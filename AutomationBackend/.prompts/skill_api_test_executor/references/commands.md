# Catálogo de Comandos de Ejecución

Todos los comandos se corren desde `AutomationBackend/tests/` (con el `venv` activado).

## Setup inicial (una sola vez por máquina)

```bash
cd AutomationBackend/tests
python -m venv venv
source venv/bin/activate        # Windows: venv\Scripts\activate
pip install -r requirements.txt
cp .env.example .env
# Editar .env con BASE_URL y credenciales reales del ambiente a probar
```

## Ejecución por alcance

4 carpetas físicas, pero solo `component/` y `e2e/` tienen lógica propia.
`smoke/` y `regression/` son **reexportaciones por import** de tests que ya
viven en `component/` — por eso `pytest.ini` las excluye de `testpaths`: un
`pytest` a secas ya corre todo exactamente una vez (vía `component/` + `e2e/`),
sin duplicar llamadas reales a la API.

| Objetivo | Comando |
|---|---|
| Suite exhaustiva completa (default, sin duplicados) | `pytest` |
| Gate rápido de deploy (solo camino feliz crítico) | `pytest tests/smoke/` |
| Regresión curada (feliz + alternos de negocio) | `pytest tests/regression/` |
| Un archivo puntual de `component/` (todos los escenarios de 1 endpoint) | `pytest tests/component/test_SIM_TC_12_create_user.py -v` |
| Un test puntual | `pytest tests/component/test_SIM_TC_12_create_user.py::test_SIM_TC_12_create_user` |
| Carpeta `e2e` completa | `pytest tests/e2e/` |
| En paralelo (pytest-xdist) | `pytest -n auto` |
| Sin reporte HTML | `pytest --no-html-report` |

## Ejecución por ambiente

`ENV` selecciona el tier (`dev`/`qa`/`prod`, default `qa`). Nunca se pasa `prod` sin confirmación explícita del usuario.

```bash
ENV=dev pytest tests/component/test_SIM_TC_12_create_user.py -v
ENV=qa pytest tests/smoke/
ENV=prod pytest tests/smoke/   # SOLO con confirmación explícita del usuario
```

## Dónde queda cada evidencia

| Artefacto | Ruta |
|---|---|
| Log completo (DEBUG + cURL) | `tests/reports/test_execution.log` |
| Reporte HTML — `component/` (fuente exhaustiva) | `tests/tests/component/reports/<archivo>_<fecha>_<hora>.html` |
| Reporte HTML — `smoke/` (gate de deploy) | `tests/tests/smoke/reports/<fecha>_<hora>.html` |
| Reporte HTML — `regression/` | `tests/tests/regression/reports/<fecha>_<hora>.html` |
| Reporte HTML — `e2e/` | `tests/tests/e2e/reports/<archivo>_<fecha>_<hora>.html` |
| Salida en vivo request/response/aserción | stdout de la consola (gracias a `-s` en `pytest.ini`) |

Nota: un test promovido a `smoke`/`regression` genera evidencia en 2 reportes
distintos si corriste ambas carpetas por separado (una vez en `component/`,
otra en `smoke/`/`regression/`) — es la misma llamada real a la API, reportada
desde 2 vistas distintas. Correr `pytest` a secas no duplica esto, porque
`testpaths` no incluye `smoke/`/`regression/`.

## Lectura rápida de la consola

```
▶ REQUEST  POST https://dummyjson.com/auth/login
  body:
    { "username": "emilys", "password": "emilyspass" }
◀ RESPONSE status=200
  body:
    { "accessToken": "...", "refreshToken": "..." }
  ✔ SUCCESS ASSERTION — status_code (esperado=200, obtenido=200)
```

Un fallo se ve igual pero con `✘ FAILED ASSERTION` y los valores esperado/obtenido explícitos — no hace falta abrir el reporte HTML para un diagnóstico rápido, pero sí para compartir evidencia.
