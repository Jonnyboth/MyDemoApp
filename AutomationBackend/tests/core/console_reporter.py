from __future__ import annotations

import json
from typing import Any

import pyfiglet

_RESET = "\033[0m"
_BOLD = "\033[1m"
_DIM = "\033[2m"
_CYAN = "\033[36m"
_MAGENTA = "\033[35m"
_GREEN = "\033[32m"
_YELLOW = "\033[33m"
_RED = "\033[31m"

_WIDTH = 72
_MAX_BODY_LINES = 20
_MAX_LIST_ITEMS = 2

_ROBOT_CAT = r"""
          (o)
           |
         /\_/\
        ( o.o )
         > ~ <
        /|   |\
       | |---| |
       |_|___|_|
        d     b
""".strip("\n")

# Datos estructurados del test en curso (request/response/aserciones). Se
# resetea por test (fixture autouse en tests/tests/conftest.py) y se vuelca
# al terminar el test (pytest_runtest_logreport en conftest.py raíz) hacia
# core/html_report.py, que arma el reporte HTML final.
_items_buffer: list[dict[str, Any]] = []


def _divider(char: str = "─") -> str:
    return char * _WIDTH


def _truncate_list(items: list[Any]) -> list[Any]:
    if len(items) <= _MAX_LIST_ITEMS:
        return items
    omitted = len(items) - _MAX_LIST_ITEMS
    return [*items[:_MAX_LIST_ITEMS], f"... ({omitted} elementos más omitidos)"]


def _summarize(data: Any) -> Any:
    """Recorta arreglos largos (top-level o anidados un nivel) antes de serializar,
    para no cortar un objeto JSON a la mitad cuando el body trae muchos elementos.
    """
    if isinstance(data, list):
        return _truncate_list(data)
    if isinstance(data, dict):
        return {
            key: (_truncate_list(value) if isinstance(value, list) else value)
            for key, value in data.items()
        }
    return data


def _pretty(data: Any) -> str:
    if data is None:
        return "(vacío)"
    try:
        text = json.dumps(_summarize(data), indent=2, ensure_ascii=False, default=str)
    except (TypeError, ValueError):
        text = str(data)

    lines = text.splitlines()
    if len(lines) > _MAX_BODY_LINES:
        omitted = len(lines) - _MAX_BODY_LINES
        lines = lines[:_MAX_BODY_LINES] + [f"... ({omitted} líneas más omitidas)"]
    return "\n".join(lines)


def reset_buffer() -> None:
    """Limpia los items acumulados. Se llama al iniciar cada test (fixture autouse)."""
    _items_buffer.clear()


def pop_buffer_items() -> list[dict[str, Any]]:
    """Devuelve una copia de los items del test en curso y limpia el buffer."""
    items = list(_items_buffer)
    _items_buffer.clear()
    return items


def print_module_banner() -> None:
    """Arte de bienvenida impreso una sola vez al iniciar cada archivo de test."""
    banner_text = pyfiglet.figlet_format("QA AUTOMATION", font="small").rstrip("\n")

    print(f"\n{_CYAN}{_divider('═')}{_RESET}")
    for line in _ROBOT_CAT.splitlines():
        print(f"{_CYAN}{_BOLD}{line.center(_WIDTH)}{_RESET}")
    print()
    for line in banner_text.splitlines():
        print(f"{_MAGENTA}{_BOLD}{line.center(_WIDTH)}{_RESET}")
    print(f"{_CYAN}{_divider('═')}{_RESET}\n")


def print_scenario(name: str) -> None:
    """Encabezado legible del escenario en curso (marcador @pytest.mark.scenario_name),
    impreso antes del primer REQUEST del test."""
    print(f"{_MAGENTA}{_BOLD}{_divider('━')}{_RESET}")
    print(f"{_MAGENTA}{_BOLD}📌 {name}{_RESET}")


def print_request(method: str, url: str, body: Any = None) -> None:
    print(f"{_DIM}{_divider()}{_RESET}")
    print(f"{_CYAN}{_BOLD}▶ REQUEST{_RESET}  {_BOLD}{method}{_RESET} {url}")
    body_text = None
    if body is not None:
        body_text = _pretty(body)
        print(f"{_DIM}  body:{_RESET}")
        for line in body_text.splitlines():
            print(f"    {line}")

    _items_buffer.append({"type": "request", "method": method, "url": url, "body": body_text})


def print_response(status_code: int, body: Any = None) -> None:
    if 200 <= status_code < 300:
        color, bucket = _GREEN, "2xx"
    elif status_code < 500:
        color, bucket = _YELLOW, "4xx"
    else:
        color, bucket = _RED, "5xx"

    print(f"{color}{_BOLD}◀ RESPONSE{_RESET} status={color}{_BOLD}{status_code}{_RESET}")
    body_text = None
    if body is not None:
        body_text = _pretty(body)
        print(f"{_DIM}  body:{_RESET}")
        for line in body_text.splitlines():
            print(f"    {line}")

    _items_buffer.append(
        {"type": "response", "status": status_code, "statusClass": bucket, "body": body_text}
    )


def print_assertion(description: str, expected: Any, actual: Any, passed: bool) -> None:
    if passed:
        print(
            f"{_GREEN}{_BOLD}  ✔ SUCCESS ASSERTION{_RESET} — {description} "
            f"(esperado={expected!r}, obtenido={actual!r})"
        )
    else:
        print(
            f"{_RED}{_BOLD}  ✘ FAILED ASSERTION{_RESET} — {description} "
            f"(esperado={expected!r}, obtenido={actual!r})"
        )
    print(f"{_DIM}{_divider()}{_RESET}")

    _items_buffer.append(
        {
            "type": "assertion",
            "desc": str(description),
            "expected": repr(expected),
            "obtained": repr(actual),
            "pass": passed,
        }
    )
