"""Tests de componente para GET /users/{id} (consulta de usuario) — SIM-TC-13."""

import pytest

from utils.assertions import (
    assert_body_contains,
    assert_header_present,
    assert_json_schema,
    assert_response_time,
    assert_status_code,
)

TC_KEY = "SIM-TC-13"

SLA_MAX_MS = 1500
CONTENT_TYPE_JSON = "application/json; charset=utf-8"

USER_SCHEMA = {
    "type": "object",
    "properties": {
        "id": {"type": "integer"},
        "firstName": {"type": "string"},
        "lastName": {"type": "string"},
        "age": {"type": "integer"},
    },
    "required": ["id", "firstName", "lastName", "age"],
}

USERS_LIST_SCHEMA = {
    "type": "object",
    "properties": {"users": {"type": "array"}},
    "required": ["users"],
}


####################################################################################
@pytest.mark.component
@pytest.mark.scenario_name(f"{TC_KEY} Get user happy path")
def test_SIM_TC_13_get_user(users_service) -> None:
    response = users_service.get_user(user_id=1)

    assert_status_code(response, 200)
    assert_header_present(response, "Content-Type", CONTENT_TYPE_JSON)
    assert_json_schema(response, USER_SCHEMA)
    assert_response_time(response, max_ms=SLA_MAX_MS)


####################################################################################
@pytest.mark.component
@pytest.mark.parametrize(
    "invalid_id",
    [
        pytest.param(0, id="cero", marks=pytest.mark.scenario_name(f"{TC_KEY} Get user id value cero")),
        pytest.param(-1, id="negativo", marks=pytest.mark.scenario_name(f"{TC_KEY} Get user id value negativo")),
        pytest.param(9999999, id="inexistente", marks=pytest.mark.scenario_name(f"{TC_KEY} Get user id inexistente")),
    ],
)
def test_SIM_TC_13_get_user_invalid_id_returns_not_found(users_service, invalid_id: int) -> None:
    response = users_service.get_user(user_id=invalid_id)

    assert_status_code(response, 404)
    assert_body_contains(response, "message", f"User with id '{invalid_id}' not found")


####################################################################################
@pytest.mark.component
@pytest.mark.scenario_name(f"{TC_KEY} Get user id vacio retorna listado completo")
def test_SIM_TC_13_get_user_empty_id_returns_full_list(users_service) -> None:
    """GET /users/ (sin id) responde 301 y redirige a /users; requests sigue el
    redirect en GET, así que el resultado final es 200 con la lista completa."""
    response = users_service.get_user(user_id="")

    assert_status_code(response, 200)
    assert_json_schema(response, USERS_LIST_SCHEMA)
