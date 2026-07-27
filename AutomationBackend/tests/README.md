# API Test Framework

Framework de automatización de pruebas para APIs REST basado en `pytest` + `requests`.
No depende de herramientas de UI (Selenium/Appium): está diseñado exclusivamente
para validar contratos, lógica de negocio y regresión de microservicios HTTP.

> **Nota de ubicación:** por indicación del proyecto, este framework vive completo
> dentro de `AutomationBackend/tests/`. Internamente conserva su propia carpeta
> `tests/` (`AutomationBackend/tests/tests/`) donde residen los casos de prueba,
> siguiendo la convención estándar de `api_test_framework/tests/`. No es un error
> de anidación: es la estructura interna del framework.

Documentación detallada:
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — capas, patrones de diseño y flujo de una petición.
- [`docs/MODULES.md`](docs/MODULES.md) — resumen de cada módulo/archivo.

## Instalación

```bash
cd AutomationBackend/tests
python -m venv venv
source venv/bin/activate        # Windows: venv\Scripts\activate
pip install -r requirements.txt

cp .env.example .env
# Editar .env con BASE_URL y credenciales reales del ambiente a probar
```

El `.env.example` viene apuntando a `https://dummyjson.com` (API pública de
prueba, sin necesidad de API key ni registro) solo como placeholder funcional
para que la suite corra de inmediato. **Reemplázalo por el `BASE_URL` real del
backend a validar.**

## Ejecución

4 carpetas físicas: `tests/tests/component/` (todos los escenarios, 1 archivo
por endpoint/TC) y `tests/tests/e2e/` (flujos encadenados) tienen lógica
propia. `tests/tests/smoke/` y `tests/tests/regression/` reexportan por
**import** los tests que ya viven en `component/` — nunca los reescriben —
así que `pytest` a secas ya es la suite exhaustiva completa, sin duplicar
llamadas reales a la API. Ver [`tests/README.md`](tests/README.md) para el
detalle completo.

```bash
# Suite exhaustiva completa (component + e2e, sin duplicados)
pytest

# Solo smoke / solo regression (carpeta, no marcador)
pytest tests/smoke/
pytest tests/regression/

# Un archivo puntual, contra el ambiente dev
ENV=dev pytest tests/component/test_SIM_TC_12_create_user.py -v

# Sin generar el reporte HTML
pytest --no-html-report
```

El reporte HTML (activado por defecto) se guarda con historial — ver
[Reporte HTML](#reporte-html) más abajo. El log detallado (incluyendo el cURL
de cada request) va siempre a `reports/test_execution.log`. La consola,
además, imprime en vivo el request, el response y cada aserción (ver sección
siguiente).

## Reporte en consola

Cada request se imprime con su body, status code y response body, y cada
aserción muestra explícitamente si pasó o falló:

```
▶ REQUEST  POST https://dummyjson.com/auth/login
  body:
    { "username": "emilys", "password": "emilyspass" }
◀ RESPONSE status=200
  body:
    { "accessToken": "...", "refreshToken": "..." }
  ✔ SUCCESS ASSERTION — status_code (esperado=200, obtenido=200)
```

Si una aserción falla, la misma línea se imprime como `✘ FAILED ASSERTION` con
los valores esperado/obtenido, antes de que pytest marque el test como fallido.

Al iniciar cada **archivo** de test (no cada test individual) se imprime un
arte de bienvenida (gatito robot + "QA AUTOMATION"). Esto requiere correr
pytest sin capturar stdout — ya viene configurado vía `-s` en `pytest.ini`.

### Activar / desactivar el reporte HTML

El reporte HTML se genera por defecto. Para desactivarlo en una corrida puntual:

```bash
pytest --no-html-report
```

## Reporte HTML

Cada reporte es un dashboard propio (HTML + CSS + JS vanilla, autocontenido en
un solo archivo, sin dependencias externas ni build): generado por
`core/html_report.py`, no por un plugin de terceros.

- Header con fecha, tabla de "Entorno" (Python, plataforma, paquetes/plugins).
- Tarjetas de resumen (Total, Passed, Failed, Duración total) + gráfica de
  dona (SVG) con el % por status; la leyenda es **clickeable** y filtra la
  lista de resultados.
- Cada test es una fila coloreada en **verde** (passed) o **rojo**
  (failed/error), con badge de resultado, conteo de aserciones (`X/Y asserts`)
  y duración. Al hacer clic se expande y muestra el detalle completo:
  bloques de **REQUEST** (método + URL + body), **RESPONSE** (status + body)
  y cada **aserción** con su resultado (`✔ SUCCESS` / `✘ FAILED`, esperado/obtenido).
- Botones "Expandir todo" / "Colapsar todo".

### Historial por carpeta física

Cada corrida genera un archivo **nuevo** (no sobrescribe el anterior), guardado
dentro de la carpeta física que ejecutaste — `pytest tests/smoke/` reporta en
`smoke/reports/`, aunque el mismo test también corra (y reporte) en
`component/reports/` cuando corres la suite exhaustiva por defecto:

```
tests/tests/component/reports/test_SIM_TC_12_create_user_20260727_072525.html
tests/tests/smoke/reports/test_smoke_users_manager_20260727_072527.html
tests/tests/regression/reports/test_reg_users_manager_20260727_072536.html
tests/tests/e2e/reports/test_user_lifecycle_e2e_20260709_001112.html
```

Regla de nombre: si la corrida tocó **un solo archivo** de esa carpeta, el
nombre lleva `{archivo}_{fecha}_{hora}.html`. Si tocó varios archivos de la
misma carpeta, el nombre es solo `{fecha}_{hora}.html`. Si corres la suite
completa (`pytest` desde la raíz), se genera un reporte separado por cada
carpeta que tuvo tests.

La consola y el HTML se alimentan de la misma fuente (`core/console_reporter.py`
acumula, por test, los eventos de request/response/aserción); `conftest.py`
los recolecta al final de cada test (`pytest_runtest_logreport`) y arma los
reportes al terminar la corrida (`pytest_sessionfinish`).

## Convenciones

- Un test nunca llama `requests` ni arma payloads directamente: pasa por un
  `service` (`services/`) que expone métodos de dominio (`login`, `create_user`, ...).
- Los payloads dinámicos se construyen con `builders/` (Faker por debajo).
- Toda aserción vive en `utils/assertions.py`; no se escriben `assert` sueltos
  contra `response.json()` dentro del test.
- Un escenario se escribe **una sola vez**, en `component/` (marcado
  `@pytest.mark.component`). Si es camino feliz crítico o un alterno de
  negocio relevante, se promueve a `smoke/`/`regression/` por **import**, no
  por copia — ver `tests/docs/ARCHITECTURE.md` §2.1.
