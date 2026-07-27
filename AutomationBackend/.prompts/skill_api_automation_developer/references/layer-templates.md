# Plantillas de Código por Capa

Plantillas basadas literalmente en el estilo del framework existente (`users`/`posts`/`auth`). Sustituye `<Dominio>`/`<dominio>` por el nombre real (ej. `Product`/`product`, `products`).

## 1. `config/endpoints.py` — agregar clase de rutas

```python
class <Dominio>Endpoints:
    """Rutas relativas del dominio de <dominio>."""

    BASE = "/<dominio>s"

    @staticmethod
    def by_id(<dominio>_id: int) -> str:
        return f"/<dominio>s/{<dominio>_id}"
```

## 2. `config/environment.py` — solo si el dominio vive en un host nuevo

```python
_DEFAULT_BASE_URLS: dict[str, str] = {
    "users": "https://dummyjson.com",
    "posts": "https://jsonplaceholder.typicode.com",
    "<dominio>s": "https://dummyjson.com",  # host real del dominio nuevo
}
```

Y en `tests/.env.example`:

```
<DOMINIO>S_DEV_BASE_URL=https://dummyjson.com
<DOMINIO>S_QA_BASE_URL=https://dummyjson.com
<DOMINIO>S_PROD_BASE_URL=https://dummyjson.com
```

## 3. `models/<dominio>_model.py` — DTO de request (solo para la operación con builder)

Un único DTO, el del payload que arma el builder para el camino feliz. **No agregues un `<Dominio>Response`**: el contrato de la respuesta se valida con un dict de JSON Schema en el propio archivo de `component/` (ver sección 7), pasado a `assert_json_schema` — un modelo de response que ningún test importa es código muerto.

```python
from pydantic import BaseModel


class Create<Dominio>Request(BaseModel):
    name: str
    price: float
```

Si el endpoint no tiene un builder (ej. un `PUT` que solo manda el campo a modificar, o un `GET`/`DELETE` por id sin body), no crees un DTO para él — un `dict[str, Any]`/escalar directo en la firma del `service` es correcto ahí (ver `UsersService.update_user` real).

## 4. `builders/<dominio>_builder.py` — builder encadenable (solo si conviene)

```python
from __future__ import annotations

from models.<dominio>_model import Create<Dominio>Request
from utils.data_generator import random_price, random_product_name  # agregar en data_generator.py si no existen


class <Dominio>Builder:
    """Builder encadenable para construir payloads de <dominio> de forma legible en los tests."""

    def __init__(self) -> None:
        self._name: str = random_product_name()
        self._price: float = random_price()

    def with_name(self, name: str) -> "<Dominio>Builder":
        self._name = name
        return self

    def with_price(self, price: float) -> "<Dominio>Builder":
        self._price = price
        return self

    def build(self) -> Create<Dominio>Request:
        return Create<Dominio>Request(name=self._name, price=self._price)
```

## 5. `services/<dominio>s_service.py` — Service Object

```python
from __future__ import annotations

from typing import Any

from requests import Response

from config.endpoints import <Dominio>Endpoints
from core.http_client import HttpClient
from models.<dominio>_model import Create<Dominio>Request


class <Dominio>sService:
    """Encapsula las operaciones CRUD del dominio de <dominio>s sobre HttpClient."""

    def __init__(self, client: HttpClient) -> None:
        self._client = client

    def create_<dominio>(self, payload: Create<Dominio>Request) -> Response:
        return self._client.post(<Dominio>Endpoints.BASE, json_body=payload.model_dump())

    def get_<dominio>(self, <dominio>_id: int) -> Response:
        return self._client.get(<Dominio>Endpoints.by_id(<dominio>_id))

    def update_<dominio>(self, <dominio>_id: int, payload: dict[str, Any]) -> Response:
        return self._client.put(<Dominio>Endpoints.by_id(<dominio>_id), json_body=payload)

    def delete_<dominio>(self, <dominio>_id: int) -> Response:
        return self._client.delete(<Dominio>Endpoints.by_id(<dominio>_id))
```

## 6. Fixture en `tests/tests/conftest.py`

```python
from services.<dominio>s_service import <Dominio>sService

@pytest.fixture
def <dominio>s_service(http_client: HttpClient) -> <Dominio>sService:
    return <Dominio>sService(http_client)
```

Si el dominio vive en un host nuevo, agregar antes en el `conftest.py` raíz (mismo patrón que `posts_environment`/`posts_http_client`):

```python
@pytest.fixture(scope="session")
def <dominio>s_environment() -> EnvironmentConfig:
    return get_environment("<dominio>s")


@pytest.fixture(scope="session")
def <dominio>s_http_client(<dominio>s_environment: EnvironmentConfig) -> HttpClient:
    return HttpClient(base_url=<dominio>s_environment.base_url, timeout=<dominio>s_environment.timeout)
```

## 7. Tests — component (1 archivo por endpoint) + smoke/regression (import, sin duplicar)

### `tests/tests/component/test_<KEY_SIN_GUIONES>_create_<dominio>.py`

Un archivo por endpoint/TC, con TODOS sus escenarios. Único lugar donde se
escribe lógica de test para ese endpoint.

Cada escenario (y cada caso de un `parametrize`) lleva **obligatoriamente**
`@pytest.mark.scenario_name("<KEY-TC-con-guiones> <validación principal>")` —
es el nombre legible que se imprime en consola (📌) y en el reporte HTML, en
formato `"<KEY-TC> <descripción corta de la validación principal>"` (ej.
`"PROD-TC-1 Create product price value negativo"`). Usa un `TC_KEY = "PROD-TC-1"`
al inicio del archivo para no repetir el key a mano en cada escenario. En
`parametrize`, cada caso necesita su propio nombre: se pasa como
`marks=pytest.mark.scenario_name(...)` dentro de un `pytest.param(...)`, nunca
un solo `scenario_name` para todos los casos del parametrize.

**Anatomía obligatoria (orden estricto):** encabezado (docstring 1 línea →
imports → constantes/JSON de soporte) y luego, por cada escenario: separador
de 84 `#` → decoradores (`component` + `scenario_name` obligatorio +
`parametrize`/`xfail` si aplica) → armado de parámetros/payload → llamado al
método del `service` (dispara el request real) → **status code siempre
primera aserción** → resto de aserciones después. Ver regla completa
("Anatomía obligatoria de un archivo `component/`") en el `system.md` de esta
skill.

**Documentación (mínima, obligatoria):** docstring de módulo de **una sola
línea** (qué endpoint/TC cubre, nada de arquitectura — eso vive en
`tests/docs/ARCHITECTURE.md`); docstring por función solo cuando el nombre de
la función no alcanza a explicar el caso. Ver regla completa en el
`system.md` de esta skill.

```python
"""Tests de componente para POST /<dominio>s (creación de <dominio>) — PROD-TC-1."""

import pytest

from builders.<dominio>_builder import <Dominio>Builder
from utils.assertions import assert_json_schema, assert_response_time, assert_status_code

TC_KEY = "PROD-TC-1"

<DOMINIO>_SCHEMA = {
    "type": "object",
    "properties": {
        "id": {"type": "integer"},
        "name": {"type": "string"},
        "price": {"type": "number"},
    },
    "required": ["id", "name", "price"],
}


####################################################################################
@pytest.mark.component
@pytest.mark.scenario_name(f"{TC_KEY} Create <dominio> happy path")
def test_PROD_TC_1_create_<dominio>(<dominio>s_service) -> None:
    payload = <Dominio>Builder().build()
    response = <dominio>s_service.create_<dominio>(payload)

    assert_status_code(response, 201)
    assert_json_schema(response, <DOMINIO>_SCHEMA)
    assert_response_time(response, max_ms=2000)


####################################################################################
@pytest.mark.component
@pytest.mark.scenario_name(f"{TC_KEY} Create <dominio> missing required field is rejected")
def test_PROD_TC_1_create_<dominio>_missing_required_field_is_rejected(<dominio>s_service) -> None:
    response = <dominio>s_service.create_<dominio>_raw({})
    assert_status_code(response, 400)


####################################################################################
@pytest.mark.component
@pytest.mark.parametrize(
    "price",
    [
        pytest.param(-1, id="negativo", marks=pytest.mark.scenario_name(f"{TC_KEY} Create <dominio> price value negativo")),
        pytest.param(0, id="cero", marks=pytest.mark.scenario_name(f"{TC_KEY} Create <dominio> price value cero")),
    ],
)
def test_PROD_TC_1_create_<dominio>_price_boundaries(<dominio>s_service, price: int) -> None:
    """Caso de borde puro — se queda solo en component, no se promueve a smoke ni a regression."""
    response = <dominio>s_service.create_<dominio>_raw({"name": "x", "price": price})
    assert_status_code(response, 400)
```

### `tests/tests/smoke/test_smoke_<dominio>.py` — solo imports, cero lógica nueva

```python
from tests.component.test_PROD_TC_1_create_<dominio> import test_PROD_TC_1_create_<dominio>
from tests.component.test_PROD_TC_2_get_<dominio> import test_PROD_TC_2_get_<dominio>
```

Nada más que eso: no se redefine el test, no se toca su cuerpo. Si mañana el
escenario en `component/` cambia el payload, el schema o el status esperado,
`smoke/` refleja el cambio automáticamente — no hay nada que mantener acá.

### `tests/tests/regression/test_reg_<dominio>.py` — mismo mecanismo, más cobertura

```python
from tests.component.test_PROD_TC_1_create_<dominio> import (
    test_PROD_TC_1_create_<dominio>,
    test_PROD_TC_1_create_<dominio>_missing_required_field_is_rejected,
)
from tests.component.test_PROD_TC_2_get_<dominio> import test_PROD_TC_2_get_<dominio>
```

Incluye el camino feliz + los alternos de negocio que el diseño marcó como
relevantes (ej. campo obligatorio faltante). **No** incluye
`test_PROD_TC_1_create_<dominio>_price_boundaries` — es un caso de borde puro,
queda solo en `component/`.

### `tests/tests/e2e/test_<dominio>_lifecycle_e2e.py` — única carpeta con código propio (no importa)

```python
import pytest

from builders.<dominio>_builder import <Dominio>Builder
from utils.assertions import assert_status_code


@pytest.mark.e2e
def test_<dominio>_lifecycle(<dominio>s_service) -> None:
    """Flujo completo: crear -> consultar -> actualizar -> eliminar."""
    create_response = <dominio>s_service.create_<dominio>(<Dominio>Builder().build())
    assert_status_code(create_response, 201)

    get_response = <dominio>s_service.get_<dominio>(<dominio>_id=1)
    assert_status_code(get_response, 200)

    update_response = <dominio>s_service.update_<dominio>(<dominio>_id=1, payload={"name": "E2E-Flow"})
    assert_status_code(update_response, 200)

    delete_response = <dominio>s_service.delete_<dominio>(<dominio>_id=1)
    assert_status_code(delete_response, 200)
```
