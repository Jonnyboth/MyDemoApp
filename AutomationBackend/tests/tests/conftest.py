from __future__ import annotations

import pytest

from core import console_reporter
from core.http_client import HttpClient
from services.users_service import UsersService


@pytest.fixture(scope="module", autouse=True)
def _print_module_art() -> None:
    """Imprime el arte de bienvenida una sola vez, al iniciar cada archivo de test."""
    console_reporter.print_module_banner()


@pytest.fixture(autouse=True)
def _reset_console_buffer(request: pytest.FixtureRequest) -> None:
    """Limpia el buffer de request/response/aserciones antes de cada test individual
    e imprime el nombre legible del escenario (@pytest.mark.scenario_name), si tiene."""
    console_reporter.reset_buffer()

    marker = request.node.get_closest_marker("scenario_name")
    if marker is not None and marker.args:
        scenario_name = marker.args[0]
        console_reporter.print_scenario(scenario_name)
        request.node.user_properties.append(("scenario_name", scenario_name))


@pytest.fixture
def users_service(http_client: HttpClient) -> UsersService:
    return UsersService(http_client)
