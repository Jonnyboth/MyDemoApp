"""Tests de componente para POST /users/add (creación de usuario) — SIM-TC-12."""

import pytest

from builders.user_builder import UserBuilder
from models.user_model import CreateUserRequest
from utils.assertions import (
    assert_body_contains,
    assert_header_present,
    assert_json_schema,
    assert_response_time,
    assert_status_code,
)

TC_KEY = "SIM-TC-12"

# SLA de rendimiento exigido para el flujo CRUD de usuarios: toda respuesta < 1.5s.
SLA_MAX_MS = 1500

CONTENT_TYPE_JSON = "application/json; charset=utf-8"

# Contrato de la respuesta de creación: dummyjson devuelve el payload enviado + id autogenerado.
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
@pytest.mark.scenario_name(f"{TC_KEY} Create user happy path")
def test_SIM_TC_12_create_user(users_service) -> None:
    payload = UserBuilder().build()
    response = users_service.create_user(payload)

    assert_status_code(response, 201)   
    assert_header_present(response, "Content-Type", CONTENT_TYPE_JSON)
    assert_json_schema(response, USER_SCHEMA)
    assert_response_time(response, max_ms=SLA_MAX_MS)


####################################################################################
@pytest.mark.component
@pytest.mark.parametrize(
    "payload",
    [
        pytest.param(
            {"firstName": "", "lastName": "Cervantes", "age": 63},
            id="firstName-vacio",
            marks=pytest.mark.scenario_name(f"{TC_KEY} Create user missing firstName is rejected"),
        ),
        pytest.param(
            {"firstName": "Alexander", "lastName": "", "age": 63},
            id="lastName-vacio",
            marks=pytest.mark.scenario_name(f"{TC_KEY} Create user missing lastName is rejected"),
        ),
        pytest.param(
            {"firstName": "Alexander", "lastName": "Cervantes", "age": ""},
            id="age-vacio",
            marks=pytest.mark.scenario_name(f"{TC_KEY} Create user missing age is rejected"),
        ),
    ],
)
@pytest.mark.xfail(
    reason="SIM-TC-12 espera 400 por campo obligatorio vacío, pero dummyjson.com no "
    "valida campos requeridos en POST /users/add: siempre responde 201 dejando el campo "
    "vacío tal cual se envió. Se documenta el comportamiento real, no el esperado por el TC — "
    "si esto cambia (XPASS), hay que revisar y actualizar el TC.",
    strict=True,
)
def test_SIM_TC_12_create_user_missing_required_field_is_rejected(users_service, payload: dict) -> None:
    response = users_service.create_user(CreateUserRequest(**payload))
    assert_status_code(response, 400)


####################################################################################
@pytest.mark.component
@pytest.mark.parametrize(
    "last_name",
    [
        pytest.param(
            "AAAAAAAAAAAAAAAAAAAAAAAAA",
            id="25-chars",
            marks=pytest.mark.scenario_name(f"{TC_KEY} Create user lastName length upper boundary 25 chars"),
        ),
        pytest.param(
            "Ab",
            id="2-chars",
            marks=pytest.mark.scenario_name(f"{TC_KEY} Create user lastName length lower boundary 2 chars"),
        ),
    ],
)
def test_SIM_TC_12_create_user_last_name_length_boundaries(users_service, last_name: str) -> None:
    payload = CreateUserRequest(firstName="Test", lastName=last_name, age=30)
    response = users_service.create_user(payload)

    assert_status_code(response, 201)
    assert_body_contains(response, "lastName", last_name)


####################################################################################
@pytest.mark.component
@pytest.mark.parametrize(
    "age",
    [
        pytest.param(101, id="mayor-100", marks=pytest.mark.scenario_name(f"{TC_KEY} Create user age value Mayor 100")),
        pytest.param(0, id="cero", marks=pytest.mark.scenario_name(f"{TC_KEY} Create user age value cero")),
        pytest.param(-1, id="negativo", marks=pytest.mark.scenario_name(f"{TC_KEY} Create user age value negativo")),
    ],
)
@pytest.mark.xfail(
    reason="SIM-TC-12 espera 400 por edad fuera del rango 1-100, pero dummyjson.com no "
    "valida rangos de negocio en POST /users/add: siempre responde 201 con el valor tal "
    "cual se envió. Se documenta el comportamiento real, no el esperado por el TC — si esto "
    "cambia (XPASS), hay que revisar y actualizar el TC.",
    strict=True,
)
def test_SIM_TC_12_create_user_age_out_of_range_is_rejected(users_service, age: int) -> None:
    payload = CreateUserRequest(firstName="Test", lastName="QA", age=age)
    response = users_service.create_user(payload)
    assert_status_code(response, 400)
