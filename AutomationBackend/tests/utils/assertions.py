from __future__ import annotations

from typing import Any

import jsonschema
from requests import Response

from core import console_reporter
from core.exceptions import ApiAssertionError, SchemaValidationError


def assert_status_code(response: Response, expected: int) -> None:
    actual = response.status_code
    passed = actual == expected
    console_reporter.print_assertion("status_code", expected, actual, passed)
    if not passed:
        raise ApiAssertionError(
            f"Status code esperado {expected}, recibido {actual}. Body: {response.text[:500]}"
        )


def assert_response_time(response: Response, max_ms: int) -> None:
    elapsed_ms = response.elapsed.total_seconds() * 1000
    passed = elapsed_ms <= max_ms
    console_reporter.print_assertion(
        "response_time", f"<= {max_ms}ms", f"{elapsed_ms:.0f}ms", passed
    )
    if not passed:
        raise ApiAssertionError(
            f"Tiempo de respuesta {elapsed_ms:.0f}ms excede el máximo permitido de {max_ms}ms"
        )


def assert_json_schema(response: Response, schema: dict[str, Any]) -> None:
    try:
        jsonschema.validate(instance=response.json(), schema=schema)
        console_reporter.print_assertion("json_schema", "cumple el schema", "cumple el schema", True)
    except jsonschema.exceptions.ValidationError as e:
        console_reporter.print_assertion("json_schema", "cumple el schema", e.message, False)
        raise SchemaValidationError(f"Respuesta no cumple el schema esperado: {e.message}") from e


def assert_body_contains(response: Response, key: str, expected_value: Any) -> None:
    body = response.json()
    actual_value = body.get(key, "<clave ausente>")
    passed = key in body and actual_value == expected_value
    console_reporter.print_assertion(f"body['{key}']", expected_value, actual_value, passed)
    if key not in body:
        raise ApiAssertionError(f"Clave '{key}' no encontrada en el body: {body}")
    if not passed:
        raise ApiAssertionError(
            f"Valor de '{key}' esperado={expected_value!r}, recibido={actual_value!r}"
        )


def assert_header_present(response: Response, header_name: str, expected_value: str | None = None) -> None:
    present = header_name in response.headers
    actual_value = response.headers.get(header_name, "<header ausente>")
    passed = present if expected_value is None else (present and actual_value == expected_value)
    console_reporter.print_assertion(
        f"header['{header_name}']", expected_value if expected_value is not None else "presente", actual_value, passed
    )
    if not present:
        raise ApiAssertionError(f"Header '{header_name}' no presente. Headers: {dict(response.headers)}")
    if expected_value is not None and actual_value != expected_value:
        raise ApiAssertionError(
            f"Header '{header_name}' esperado={expected_value!r}, recibido={actual_value!r}"
        )
