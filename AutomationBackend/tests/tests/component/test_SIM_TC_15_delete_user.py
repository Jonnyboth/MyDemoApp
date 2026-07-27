"""Tests de componente para DELETE /users/{id} (borrado de usuario) — SIM-TC-15."""

import pytest

from utils.assertions import (
    assert_header_present,
    assert_json_schema,
    assert_response_time,
    assert_status_code,
)

TC_KEY = "SIM-TC-15"

SLA_MAX_MS = 1500
CONTENT_TYPE_JSON = "application/json; charset=utf-8"

# Contrato de la respuesta de borrado: el usuario completo + las marcas de borrado.
DELETED_USER_SCHEMA = {
    "type": "object",
    "properties": {
        "id": {"type": "integer"},
        "isDeleted": {"type": "boolean"},
        "deletedOn": {"type": "string"},
    },
    "required": ["id", "isDeleted", "deletedOn"],
}


####################################################################################
@pytest.mark.component
@pytest.mark.scenario_name(f"{TC_KEY} Delete user happy path")
def test_SIM_TC_15_delete_user(users_service) -> None:
    response = users_service.delete_user(user_id=1)

    assert_status_code(response, 200)
    assert_header_present(response, "Content-Type", CONTENT_TYPE_JSON)
    assert_json_schema(response, DELETED_USER_SCHEMA)
    assert_response_time(response, max_ms=SLA_MAX_MS)
