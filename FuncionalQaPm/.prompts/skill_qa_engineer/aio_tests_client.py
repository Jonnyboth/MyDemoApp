#!/usr/bin/env python3
"""
Cliente REST para AIO Tests (All-In-One Tests) — plugin de gestión de pruebas
para Jira Cloud, usado en tu proyecto Jira (configura project_key según tu proyecto; por defecto: TP).

Provee funciones de bajo nivel para:
    - create_test_case(...)  -> POST  (crear un Caso de Prueba)
    - update_test_case(...)  -> PUT   (actualizar un Caso de Prueba existente)
    - list_test_cases(...)   -> GET   (listar Casos de Prueba, paginado)
    - get_test_case(...)     -> GET   (obtener un Caso de Prueba puntual por ID/key)
    - search_test_cases(...) -> POST  (buscar por título, útil para evitar duplicados)

Autenticación:
    AIO Tests usa un esquema de autorización propio ("nativo"), NO Basic Auth
    ni Bearer estándar. La cabecera enviada tiene la forma:

        Authorization: AioAuth <token_base64>

    El token se lee de la variable de entorno AIO_API_TOKEN (ver .env).

Endpoints (confirmados contra el OpenAPI oficial publicado por AIO Tests en
https://tcms.aiojiraapps.com/aio-tcms/aiotcms-static/api-docs/, spec en
https://tcms.aiojiraapps.com/aio-tcms/api/v1/openapi.json):
    Base:  https://tcms.aiojiraapps.com/aio-tcms
    POST   /api/v1/project/{jiraProjectId}/testcase                 -> crear
    GET    /api/v1/project/{jiraProjectId}/testcase                 -> listar (paginado)
    POST   /api/v1/project/{jiraProjectId}/testcase/search           -> buscar por criterios
    GET    /api/v1/project/{jiraProjectId}/testcase/{testCaseId}/detail -> obtener uno
    PUT    /api/v1/project/{jiraProjectId}/testcase/{testCaseId}/detail -> actualizar uno

    `jiraProjectId` acepta tanto la key del proyecto (ej. "TP") como su ID numérico.

Este módulo expone una interfaz "amigable" (title/description/precondition/
priority/labels/steps con test_data/expected_result) y la traduce internamente
al formato real que espera la API de AIO Tests (steps con step/data/
expectedResult, tags como objetos, priority como objeto {"name": ...}, etc.).
Ver test_spec.md para el contrato de entrada completo.

Variables de entorno esperadas (ver archivo .env en esta misma carpeta):
    AIO_API_TOKEN   -> Token de autenticación de AIO Tests (sin el prefijo "AioAuth ")
    PROJECT_KEY     -> Clave del proyecto Jira, ej. "TP"
    AIO_BASE_URL    -> URL base de la API de AIO Tests (opcional, tiene default)
"""

from __future__ import annotations

import argparse
import json
import logging
import os
import sys
from typing import Any, Optional

import requests
from dotenv import load_dotenv

# Carga las variables definidas en el archivo .env ubicado junto a este script.
load_dotenv(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".env"))

# ---------------------------------------------------------------------------
# Configuración de logging
# ---------------------------------------------------------------------------
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger("aio_tests_client")

# ---------------------------------------------------------------------------
# Constantes de configuración (placeholders de ejemplo; los valores reales
# deben venir del archivo .env, NUNCA hardcodeados aquí ni commiteados).
# ---------------------------------------------------------------------------
AIO_API_TOKEN: str = os.getenv("AIO_API_TOKEN", "TU_AIO_API_TOKEN_AQUI")
PROJECT_KEY: str = os.getenv("PROJECT_KEY", "TU_PROJECT_KEY_AQUI")
AIO_BASE_URL: str = os.getenv("AIO_BASE_URL", "https://tcms.aiojiraapps.com/aio-tcms")

REQUEST_TIMEOUT_SECONDS = 20

# Paths de la API REST v1 de AIO Tests (confirmados contra el OpenAPI oficial).
ENDPOINT_TEST_CASE_COLLECTION = "/api/v1/project/{project_key}/testcase"
ENDPOINT_TEST_CASE_SEARCH = "/api/v1/project/{project_key}/testcase/search"
ENDPOINT_TEST_CASE_DETAIL = "/api/v1/project/{project_key}/testcase/{test_case_id}/detail"
ENDPOINT_TAGS = "/api/v1/project/{project_key}/tag"
ENDPOINT_CUSTOM_FIELDS = "/api/v1/project/{project_key}/config/customfield"

# Cache en memoria de la configuración de custom fields por proyecto, para no
# repetir la consulta en cada create/update dentro del mismo proceso.
_custom_fields_cache: dict[str, list] = {}


class AioTestsConfigError(Exception):
    """Falta configuración obligatoria (token o project key) antes de llamar a la API."""


class AioTestsAPIError(Exception):
    """La API de AIO Tests respondió con un código de error HTTP."""

    def __init__(self, status_code: int, message: str, raw_response: Any = None):
        self.status_code = status_code
        self.message = message
        self.raw_response = raw_response
        super().__init__(f"[HTTP {status_code}] {message}")


# ---------------------------------------------------------------------------
# Helpers internos
# ---------------------------------------------------------------------------
def _build_headers() -> dict:
    """Construye las cabeceras de autenticación en el formato nativo de AIO Tests."""
    if not AIO_API_TOKEN or AIO_API_TOKEN == "TU_AIO_API_TOKEN_AQUI":
        raise AioTestsConfigError(
            "AIO_API_TOKEN no está configurado. Define esta variable en el "
            "archivo .env (ver .env.example) antes de llamar a la API de AIO Tests."
        )
    return {
        "Authorization": f"AioAuth {AIO_API_TOKEN}",
        "Content-Type": "application/json",
        "Accept": "application/json",
    }


def _resolve_project_key(project_key: Optional[str]) -> str:
    key = project_key or PROJECT_KEY
    if not key or key == "TU_PROJECT_KEY_AQUI":
        raise AioTestsConfigError(
            "PROJECT_KEY no está configurado. Define esta variable en el "
            "archivo .env o pásala explícitamente como argumento."
        )
    return key


def _build_case_payload(test_case: dict) -> dict:
    """
    Traduce la estructura "amigable" documentada en test_spec.md al formato
    real que espera AIO Tests (esquema CaseFullDetails del OpenAPI oficial).

    Entrada amigable:
        {
            "title": str, "description": str, "precondition": str,
            "priority": str, "labels": [str, ...],
            "status": str, "type": str, "estimatedEffort": int (segundos),
            "steps": [{"step": str, "test_data": str, "expected_result": str}, ...]
        }

    Nota sobre `status` (verificado en vivo vía `get_test_case_schema`, campo
    OBLIGATORIO en AIO): esta función NO le pone un default — eso solo tiene
    sentido en creación (ver `create_test_case`, que sí lo defaultea a
    `"Draft"`), nunca en actualización, donde forzar un default pisaría el
    estado real de un Caso ya existente que el usuario no pidió cambiar.
    """
    payload: dict = {}
    for field in ("title", "description", "precondition"):
        if field in test_case and test_case[field] is not None:
            payload[field] = test_case[field]

    if test_case.get("priority"):
        payload["priority"] = {"name": test_case["priority"]}

    # `status` y `type` son objetos `{"name": ...}` en el esquema real
    # (CaseStatus / CaseType), igual que `priority` — nunca un string plano.
    if test_case.get("status"):
        payload["status"] = {"name": test_case["status"]}
    if test_case.get("type"):
        payload["type"] = {"name": test_case["type"]}

    if test_case.get("estimatedEffort") is not None:
        payload["estimatedEffort"] = test_case["estimatedEffort"]

    # `labels` (etiquetas amigables) se resuelve aparte vía _resolve_tags(),
    # porque AIO Tests requiere el ID real del tag (ya existente o recién
    # creado con POST /tag) y no acepta crearlo "al vuelo" dentro del case.

    if "steps" in test_case and test_case["steps"] is not None:
        payload["steps"] = [
            {
                "step": step.get("step", ""),
                "data": step.get("test_data", step.get("data", "")),
                "expectedResult": step.get("expected_result", step.get("expectedResult", "")),
                "stepType": step.get("stepType", "TEXT"),
            }
            for step in test_case["steps"]
        ]
        # Los pasos "TEXT" (step/data/expectedResult) requieren el Script Type
        # "Classic" del proyecto; sin esto la API responde 400 "Invalid or
        # missing value for Test Script Type".
        payload.setdefault("scriptType", {"name": "Classic"})

    # `status`, `type`, `scriptType` ya quedaron resueltos arriba como objetos
    # `{"name": ...}`. `customFields` y `tags` se resuelven aparte (ver
    # _resolve_custom_fields / _resolve_tags) porque requieren traducir
    # nombres amigables a IDs reales contra la configuración del proyecto.

    return payload


def _get_custom_fields_config(project_key: str) -> list:
    """
    Obtiene (y cachea en memoria por proceso) la configuración real de custom
    fields de Test Case del proyecto, vía GET /config/customfield.
    """
    if project_key in _custom_fields_cache:
        return _custom_fields_cache[project_key]
    url = f"{AIO_BASE_URL}{ENDPOINT_CUSTOM_FIELDS.format(project_key=project_key)}"
    fields = _request("GET", url, headers=_build_headers(), timeout=REQUEST_TIMEOUT_SECONDS)
    _custom_fields_cache[project_key] = fields
    return fields


def _resolve_custom_fields(custom_fields: Any, project_key: str) -> list:
    """
    Traduce el formato "amigable" documentado en formatting-rules.md
    (ej. {"Testing Layers": "Web", "Test Type Cycle": "Smoke"}) al formato
    real que exige AIO Tests: un ARRAY de objetos
    [{"ID": <fieldId>, "name": <fieldName>, "value": {"ID": <valueId>, "value": <valueName>}}, ...]
    (esquema `CustomFieldValue` / `SingleSelectListValue` del OpenAPI oficial).

    Si `custom_fields` ya viene como una lista (formato nativo, para uso avanzado),
    se pasa tal cual sin resolver nada.

    Lanza AioTestsConfigError si el nombre del campo o del valor no existe en la
    configuración real del proyecto — nunca inventa un ID ni lo omite en silencio.
    """
    if not custom_fields:
        return []
    if isinstance(custom_fields, list):
        return custom_fields  # ya viene en formato nativo, no se traduce

    fields_config = _get_custom_fields_config(project_key)
    by_name = {f["name"]: f for f in fields_config}

    resolved = []
    for field_name, value in custom_fields.items():
        field = by_name.get(field_name)
        if field is None:
            available = ", ".join(sorted(by_name.keys())) or "(ninguno configurado)"
            raise AioTestsConfigError(
                f"Custom field '{field_name}' no existe en la configuración de "
                f"Test Cases del proyecto {project_key}. Campos disponibles: {available}. "
                "Nunca se debe inventar un customfield_XXXXX ni un nombre de campo."
            )

        entry: dict = {"ID": field["ID"], "name": field["name"]}
        if field.get("type") in ("SINGLE_SELECT_LIST", "MULTI_SELECT_LIST"):
            allowed = {v["value"]: v for v in field.get("allowedListValues", [])}
            option = allowed.get(value)
            if option is None:
                valid = ", ".join(sorted(allowed.keys())) or "(ninguno)"
                raise AioTestsConfigError(
                    f"Valor '{value}' no es una opción válida para el custom field "
                    f"'{field_name}'. Valores permitidos: {valid}."
                )
            entry["value"] = {"ID": option["ID"], "value": option["value"]}
        else:
            entry["value"] = value
        resolved.append(entry)

    return resolved


# Mensajes legibles para los códigos de estado HTTP más comunes de esta API.
_STATUS_MESSAGES = {
    400: "Solicitud inválida: revisa el formato del JSON enviado (título, pasos, etc.).",
    401: "No autorizado: el AIO_API_TOKEN es inválido, expiró o falta la cabecera Authorization.",
    403: "Prohibido: el token no tiene permisos sobre el proyecto/recurso solicitado.",
    404: "No encontrado: el proyecto o el Caso de Prueba (ID/key) no existe.",
    409: "Conflicto: el recurso fue modificado por otro proceso (ej. versión desactualizada).",
    429: "Demasiadas solicitudes: se alcanzó el límite de rate limiting de AIO Tests.",
    500: "Error interno del servidor de AIO Tests. Reintenta más tarde.",
    503: "Servicio no disponible temporalmente en AIO Tests.",
}


def _handle_response(response: requests.Response) -> dict:
    """Interpreta la respuesta HTTP, logea el resultado y lanza error si corresponde."""
    try:
        body = response.json() if response.content else {}
    except ValueError:
        body = {"raw_text": response.text}

    if response.status_code in (200, 201):
        logger.info("OK (HTTP %s) -> %s", response.status_code, response.url)
        return body

    message = _STATUS_MESSAGES.get(
        response.status_code, f"Error HTTP no clasificado ({response.status_code})."
    )
    logger.error(
        "Fallo en la llamada a AIO Tests (HTTP %s) %s: %s | Respuesta: %s",
        response.status_code,
        response.url,
        message,
        json.dumps(body, ensure_ascii=False)[:2000],
    )
    raise AioTestsAPIError(response.status_code, message, raw_response=body)


def _request(method: str, url: str, **kwargs) -> dict:
    try:
        response = requests.request(method, url, **kwargs)
    except requests.exceptions.RequestException as exc:
        logger.error("Error de red llamando a %s %s: %s", method, url, exc)
        raise
    return _handle_response(response)


def _resolve_tags(labels: list[str], project_key: str) -> list[dict]:
    """
    Resuelve nombres de etiquetas amigables a tags reales de AIO Tests.

    POST /tag es "create-or-get": si la etiqueta ya existe en el proyecto
    devuelve su ID existente, si no la crea. El resultado se envuelve en la
    forma que espera el campo `tags` de un Caso de Prueba: [{"tag": {...}}].
    """
    if not labels:
        return []
    url = f"{AIO_BASE_URL}{ENDPOINT_TAGS.format(project_key=project_key)}"
    body = [{"name": label} for label in labels]
    tags = _request("POST", url, headers=_build_headers(), json=body, timeout=REQUEST_TIMEOUT_SECONDS)
    return [{"tag": tag} for tag in tags]


# ---------------------------------------------------------------------------
# Operaciones públicas
# ---------------------------------------------------------------------------
def create_test_case(test_case: dict, project_key: Optional[str] = None) -> dict:
    """
    Crea un nuevo Caso de Prueba en AIO Tests.

    `test_case` sigue la estructura amigable documentada en test_spec.md
    (title, description, precondition, priority, labels, steps con
    test_data/expected_result). Único campo obligatorio: `title`.
    """
    key = _resolve_project_key(project_key)
    url = f"{AIO_BASE_URL}{ENDPOINT_TEST_CASE_COLLECTION.format(project_key=key)}"
    body = _build_case_payload(test_case)
    # `status` es obligatorio en AIO (verificado en vivo); solo en creación
    # (nunca en update) se defaultea a "Draft" si no se especifica.
    body.setdefault("status", {"name": "Draft"})
    if test_case.get("labels"):
        body["tags"] = _resolve_tags(test_case["labels"], key)
    if test_case.get("customFields"):
        body["customFields"] = _resolve_custom_fields(test_case["customFields"], key)

    logger.info("Creando Caso de Prueba '%s' en proyecto %s", test_case.get("title"), key)
    return _request(
        "POST",
        url,
        headers=_build_headers(),
        json=body,
        timeout=REQUEST_TIMEOUT_SECONDS,
    )


# Campos de CaseFullDetails que la API acepta reenviar en un PUT. El resto
# (jiraProjectID, permission, key, *Attachments, createdDate, updatedDate,
# hasDataSets, versions, dataSets, etc.) son metadatos de solo lectura que la
# API rechaza o ignora si se incluyen.
_CASE_WRITABLE_FIELDS = (
    "ID", "version", "title", "description", "precondition", "ownedByID",
    "folder", "status", "priority", "scriptType", "type", "jiraComponentIDs",
    "jiraReleaseIDs", "estimatedEffort", "customFields", "steps", "tags",
    "automationStatus", "automationOwnerID", "automationKey", "jiraRequirementIDs",
)


def update_test_case(
    test_case_id: str, updates: dict, project_key: Optional[str] = None
) -> dict:
    """
    Actualiza (parcial o totalmente) un Caso de Prueba existente, identificado
    por su ID o su key (ej. "TP-TC-3").

    La API de AIO Tests no soporta PATCH parcial real: un PUT exitoso requiere
    reenviar el Caso completo (incluyendo `ID`/`version` para control de
    concurrencia optimista), tal como indica su propia documentación ("Fetch
    the existing case details ... Update the required parameters and call
    this API"). Por eso esta función primero hace GET del caso actual, aplica
    encima `updates` (que sí puede ser un subconjunto parcial, por ejemplo
    solo {"description": "..."} o solo {"steps": [...]}), y recién entonces
    hace el PUT con el objeto resultante completo.
    """
    key = _resolve_project_key(project_key)
    current = get_test_case(test_case_id, project_key=key)

    body = {field: current[field] for field in _CASE_WRITABLE_FIELDS if field in current}
    body.update(_build_case_payload(updates))
    if updates.get("labels"):
        body["tags"] = _resolve_tags(updates["labels"], key)
    if updates.get("customFields"):
        body["customFields"] = _resolve_custom_fields(updates["customFields"], key)

    url = f"{AIO_BASE_URL}{ENDPOINT_TEST_CASE_DETAIL.format(project_key=key, test_case_id=test_case_id)}"
    logger.info("Actualizando Caso de Prueba %s en proyecto %s", test_case_id, key)
    return _request(
        "PUT",
        url,
        headers=_build_headers(),
        json=body,
        timeout=REQUEST_TIMEOUT_SECONDS,
    )


def get_test_case(test_case_id: str, project_key: Optional[str] = None) -> dict:
    """Obtiene el detalle de un único Caso de Prueba por su ID o key."""
    key = _resolve_project_key(project_key)
    url = f"{AIO_BASE_URL}{ENDPOINT_TEST_CASE_DETAIL.format(project_key=key, test_case_id=test_case_id)}"

    logger.info("Consultando Caso de Prueba %s en proyecto %s", test_case_id, key)
    return _request(
        "GET",
        url,
        headers=_build_headers(),
        timeout=REQUEST_TIMEOUT_SECONDS,
    )


def list_test_cases(
    project_key: Optional[str] = None,
    max_results: int = 50,
    start_at: int = 0,
) -> dict:
    """Lista los Casos de Prueba del proyecto, con paginación (respuesta paginada)."""
    key = _resolve_project_key(project_key)
    url = f"{AIO_BASE_URL}{ENDPOINT_TEST_CASE_COLLECTION.format(project_key=key)}"
    params = {"maxResults": max_results, "startAt": start_at}

    logger.info("Listando Casos de Prueba del proyecto %s (startAt=%s)", key, start_at)
    return _request(
        "GET",
        url,
        headers=_build_headers(),
        params=params,
        timeout=REQUEST_TIMEOUT_SECONDS,
    )


def search_test_cases(
    title_contains: str,
    project_key: Optional[str] = None,
    max_results: int = 50,
    start_at: int = 0,
) -> dict:
    """
    Busca Casos de Prueba cuyo título contenga `title_contains`.
    Útil para verificar duplicados antes de crear un nuevo caso.
    """
    key = _resolve_project_key(project_key)
    url = f"{AIO_BASE_URL}{ENDPOINT_TEST_CASE_SEARCH.format(project_key=key)}"
    params = {"maxResults": max_results, "startAt": start_at}
    body = {"title": {"comparisonType": "CONTAINS", "value": title_contains}}

    logger.info("Buscando Casos de Prueba con título que contenga '%s' en %s", title_contains, key)
    return _request(
        "POST",
        url,
        headers=_build_headers(),
        params=params,
        json=body,
        timeout=REQUEST_TIMEOUT_SECONDS,
    )


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------
def _load_json_arg(json_str: Optional[str], json_file: Optional[str]) -> dict:
    if json_file:
        with open(json_file, "r", encoding="utf-8") as fh:
            return json.load(fh)
    if json_str:
        return json.loads(json_str)
    raise SystemExit("Debes proporcionar --json o --json-file con el payload del Caso de Prueba.")


def _parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Cliente CLI para AIO Tests (crear / actualizar / listar / obtener / buscar Casos de Prueba)."
    )
    parser.add_argument("--project-key", default=None, help="Clave del proyecto Jira (default: .env PROJECT_KEY)")
    sub = parser.add_subparsers(dest="action", required=True)

    p_create = sub.add_parser("create", help="Crear un nuevo Caso de Prueba")
    p_create.add_argument("--json", help="Payload JSON en línea")
    p_create.add_argument("--json-file", help="Ruta a un archivo .json con el payload")

    p_update = sub.add_parser("update", help="Actualizar un Caso de Prueba existente")
    p_update.add_argument("--id", required=True, help="ID o key del Caso de Prueba a actualizar")
    p_update.add_argument("--json", help="Payload JSON en línea con los campos a actualizar")
    p_update.add_argument("--json-file", help="Ruta a un archivo .json con los campos a actualizar")

    p_get = sub.add_parser("get", help="Obtener un Caso de Prueba por ID/key")
    p_get.add_argument("--id", required=True, help="ID o key del Caso de Prueba")

    p_list = sub.add_parser("list", help="Listar Casos de Prueba del proyecto")
    p_list.add_argument("--max-results", type=int, default=50)
    p_list.add_argument("--start-at", type=int, default=0)

    p_search = sub.add_parser("search", help="Buscar Casos de Prueba por título")
    p_search.add_argument("--title-contains", required=True)
    p_search.add_argument("--max-results", type=int, default=50)
    p_search.add_argument("--start-at", type=int, default=0)

    return parser.parse_args()


def main() -> None:
    args = _parse_args()
    try:
        if args.action == "create":
            payload = _load_json_arg(args.json, args.json_file)
            result = create_test_case(payload, project_key=args.project_key)
        elif args.action == "update":
            payload = _load_json_arg(args.json, args.json_file)
            result = update_test_case(args.id, payload, project_key=args.project_key)
        elif args.action == "get":
            result = get_test_case(args.id, project_key=args.project_key)
        elif args.action == "list":
            result = list_test_cases(
                project_key=args.project_key,
                max_results=args.max_results,
                start_at=args.start_at,
            )
        elif args.action == "search":
            result = search_test_cases(
                args.title_contains,
                project_key=args.project_key,
                max_results=args.max_results,
                start_at=args.start_at,
            )
        else:  # pragma: no cover - argparse ya restringe las opciones válidas
            raise SystemExit(f"Acción no soportada: {args.action}")
    except (AioTestsConfigError, AioTestsAPIError) as exc:
        logger.error(str(exc))
        sys.exit(1)
    except requests.exceptions.RequestException as exc:
        logger.error("Error de red/conexión hacia AIO Tests: %s", exc)
        sys.exit(1)

    print(json.dumps(result, ensure_ascii=False, indent=2))
    sys.exit(0)


if __name__ == "__main__":
    main()
