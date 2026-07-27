from __future__ import annotations

import platform
from datetime import datetime
from importlib.metadata import PackageNotFoundError, version
from pathlib import Path
from typing import Any

import pytest

from config.environment import EnvironmentConfig, get_environment
from core import console_reporter, html_report
from core.http_client import HttpClient
from core.session_manager import SessionManager

_TESTS_ROOT = Path(__file__).parent / "tests"

# Resultados acumulados de la corrida, usados para armar el reporte HTML propio
# al final de la sesión (pytest_sessionfinish). Vive a nivel de módulo porque
# pytest_runtest_logreport no tiene un fixture request al cual "colgarse".
_collected_tests: list[dict[str, Any]] = []


def pytest_addoption(parser: pytest.Parser) -> None:
    parser.addoption(
        "--no-html-report",
        action="store_true",
        default=False,
        help="Desactiva la generación del reporte HTML (por defecto está activado).",
    )


def _package_version(name: str) -> str:
    try:
        return version(name)
    except PackageNotFoundError:
        return "?"


def _build_environment() -> dict[str, Any]:
    return {
        "Python": platform.python_version(),
        "Platform": platform.platform(),
        "Packages": {
            "pytest": _package_version("pytest"),
            "pluggy": _package_version("pluggy"),
        },
        "Plugins": {
            "xdist": _package_version("pytest-xdist"),
            "Faker": _package_version("Faker"),
            "pydantic": _package_version("pydantic"),
        },
    }


def pytest_runtest_logreport(report: pytest.TestReport) -> None:
    """Captura, al terminar la fase 'call' de cada test, su resultado y el
    detalle de request/response/aserciones acumulado por console_reporter."""
    if report.when != "call":
        return

    items = console_reporter.pop_buffer_items()
    assertions = [item for item in items if item["type"] == "assertion"]
    scenario_name = next(
        (value for key, value in report.user_properties if key == "scenario_name"),
        report.nodeid,
    )

    _collected_tests.append(
        {
            "id": report.nodeid,
            "testId": report.nodeid,
            "scenarioName": scenario_name,
            "result": report.outcome.capitalize(),
            "duration": f"{report.duration * 1000:.0f} ms",
            "durationMs": report.duration * 1000,
            "assertionCount": len(assertions),
            "assertionPassCount": sum(1 for a in assertions if a["pass"]),
            "items": items,
        }
    )


def _category_and_file(nodeid: str) -> tuple[str, str] | None:
    """Extrae (categoria, archivo) de un nodeid tipo 'tests/smoke/test_x.py::test_y'.

    La categoría es el subdirectorio bajo tests/ (smoke/regression/component/e2e).
    Devuelve None si el nodeid no sigue esa convención (ej. un test suelto).
    """
    parts = nodeid.split("::")[0].split("/")
    if len(parts) < 3 or parts[0] != "tests":
        return None
    return parts[1], Path(parts[-1]).stem


def pytest_sessionfinish(session: pytest.Session) -> None:
    """Genera un reporte HTML con historial por cada carpeta de test tocada
    en la corrida (smoke/regression/component/e2e), dentro de su propia
    subcarpeta reports/. El nombre incluye fecha y hora; si la corrida tocó
    un único archivo de esa carpeta (caso típico de component/e2e, que se
    ejecutan archivo por archivo), el nombre del archivo va antes de la fecha.

    Se salta en los workers de pytest-xdist (solo el proceso controller
    escribe el archivo final) para no pisar el reporte con datos parciales.
    """
    if hasattr(session.config, "workerinput"):
        return
    if session.config.getoption("--no-html-report"):
        return
    if not _collected_tests:
        return

    tests_by_category: dict[str, list[dict[str, Any]]] = {}
    files_by_category: dict[str, set[str]] = {}
    for test in _collected_tests:
        parsed = _category_and_file(test["id"])
        if parsed is None:
            continue
        category, file_stem = parsed
        tests_by_category.setdefault(category, []).append(test)
        files_by_category.setdefault(category, set()).add(file_stem)

    if not tests_by_category:
        return

    environment = _build_environment()
    now = datetime.now()
    generated_at = now.strftime("%d-%b-%Y, %H:%M")
    timestamp = now.strftime("%Y%m%d_%H%M%S")

    reporter = session.config.pluginmanager.getplugin("terminalreporter")

    for category, tests in tests_by_category.items():
        files = files_by_category[category]
        filename = f"{next(iter(files))}_{timestamp}.html" if len(files) == 1 else f"{timestamp}.html"
        output_path = _TESTS_ROOT / category / "reports" / filename

        data = {"environment": environment, "tests": tests}
        html_report.generate_report(output_path, f"Reporte de Pruebas — {category}", generated_at, data)

        if reporter is not None:
            reporter.write_sep("-", f"Generated html report: {output_path.as_uri()}")


@pytest.fixture(scope="session")
def environment() -> EnvironmentConfig:
    return get_environment("users")


@pytest.fixture(scope="session")
def http_client(environment: EnvironmentConfig) -> HttpClient:
    """Cliente HTTP sin autenticar hacia el host de 'users' (dummyjson), para endpoints públicos (ej. login)."""
    return HttpClient(base_url=environment.base_url, timeout=environment.timeout)


@pytest.fixture(scope="session")
def session_manager(environment: EnvironmentConfig) -> SessionManager:
    return SessionManager(base_url=environment.base_url, timeout=environment.timeout)


@pytest.fixture(scope="session")
def authenticated_client(session_manager: SessionManager, environment: EnvironmentConfig) -> HttpClient:
    """Cliente HTTP con token cacheado a nivel de sesión: un solo login por corrida."""
    return session_manager.authenticated_client(environment.username, environment.password)


@pytest.fixture(scope="session")
def posts_environment() -> EnvironmentConfig:
    return get_environment("posts")


@pytest.fixture(scope="session")
def posts_http_client(posts_environment: EnvironmentConfig) -> HttpClient:
    """Cliente HTTP hacia el host de 'posts' (jsonplaceholder) — un host distinto al de 'users'."""
    return HttpClient(base_url=posts_environment.base_url, timeout=posts_environment.timeout)
