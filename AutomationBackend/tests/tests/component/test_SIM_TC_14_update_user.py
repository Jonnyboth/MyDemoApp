"""Tests de componente para PUT /users/{id} (actualización de usuario) — SIM-TC-14."""

import pytest

from utils.assertions import (
    assert_body_contains,
    assert_header_present,
    assert_json_schema,
    assert_response_time,
    assert_status_code,
)
from utils.data_generator import random_last_name

TC_KEY = "SIM-TC-14"

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


####################################################################################
@pytest.mark.component
@pytest.mark.scenario_name(f"{TC_KEY} Update user happy path")
def test_SIM_TC_14_update_user(users_service) -> None:
    payload = {"lastName": random_last_name()}
    response = users_service.update_user(user_id=1, payload=payload)

    assert_status_code(response, 200)
    assert_header_present(response, "Content-Type", CONTENT_TYPE_JSON)
    assert_json_schema(response, USER_SCHEMA)
    assert_response_time(response, max_ms=SLA_MAX_MS)


####################################################################################
@pytest.mark.component
@pytest.mark.parametrize(
    "invalid_id",
    [
        pytest.param(0, id="cero", marks=pytest.mark.scenario_name(f"{TC_KEY} Update user id value cero")),
        pytest.param(-1, id="negativo", marks=pytest.mark.scenario_name(f"{TC_KEY} Update user id value negativo")),
        pytest.param(9999999, id="inexistente", marks=pytest.mark.scenario_name(f"{TC_KEY} Update user id inexistente")),
    ],
)
def test_SIM_TC_14_update_user_invalid_id_returns_not_found(users_service, invalid_id: int) -> None:
    response = users_service.update_user(user_id=invalid_id, payload={"lastName": "Nuevo"})

    assert_status_code(response, 404)
    assert_body_contains(response, "message", f"User with id '{invalid_id}' not found")


####################################################################################
@pytest.mark.component
@pytest.mark.scenario_name(f"{TC_KEY} Update user id vacio es rechazado")
@pytest.mark.xfail(
    reason="SIM-TC-14 espera 400 'user_Id requerido' con id vacío, pero dummyjson.com no "
    "valida esa ruta en PUT: responde 404 con una página HTML de Express ('Cannot PUT "
    "/users'), no un JSON de negocio. Se documenta el comportamiento real, no el esperado "
    "por el TC — si esto cambia (XPASS), hay que revisar y actualizar el TC.",
    strict=True,
)
def test_SIM_TC_14_update_user_empty_id_returns_bad_request(users_service) -> None:
    response = users_service.update_user(user_id="", payload={"lastName": "Nuevo"})
    assert_status_code(response, 400)


####################################################################################
@pytest.mark.component
@pytest.mark.parametrize(
    "last_name",
    [
        pytest.param(
            "AAAAAAAAAAAAAAAAAAAAAAAAA",
            id="25-chars",
            marks=pytest.mark.scenario_name(f"{TC_KEY} Update user lastName length upper boundary 25 chars"),
        ),
        pytest.param(
            "Ab",
            id="2-chars",
            marks=pytest.mark.scenario_name(f"{TC_KEY} Update user lastName length lower boundary 2 chars"),
        ),
    ],
)
def test_SIM_TC_14_update_user_last_name_length_boundaries(users_service, last_name: str) -> None:
    response = users_service.update_user(user_id=1, payload={"lastName": last_name})

    assert_status_code(response, 200)
    assert_body_contains(response, "lastName", last_name)


####################################################################################
@pytest.mark.component
@pytest.mark.scenario_name(f"{TC_KEY} Update user lastName vacio es rechazado")
@pytest.mark.xfail(
    reason="SIM-TC-14 espera 400 'lastName requerido' con lastName vacío, pero "
    "dummyjson.com no valida campos requeridos en PUT /users/{id}: responde 200 y persiste "
    "lastName=''. Se documenta el comportamiento real, no el esperado por el TC — si esto "
    "cambia (XPASS), hay que revisar y actualizar el TC.",
    strict=True,
)
def test_SIM_TC_14_update_user_empty_last_name_is_rejected(users_service) -> None:
    response = users_service.update_user(user_id=1, payload={"lastName": ""})
    assert_status_code(response, 400)
