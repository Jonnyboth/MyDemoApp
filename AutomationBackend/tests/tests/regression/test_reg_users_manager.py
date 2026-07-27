"""Regresión curada del microservicio de usuarios: camino feliz + alternos de
negocio (campo/id faltante). Excluye los bordes puros de tests/tests/component/
(límites numéricos, ids inválidos usados solo para forzar un 404 genérico) —
esos quedan solo ahí, no se promueven acá.

No define tests nuevos — reexporta (import) los escenarios ya escritos en
tests/tests/component/, así que si su firma/payload/schema cambia, este
archivo no necesita tocarse.
"""

from tests.component.test_SIM_TC_12_create_user import (
    test_SIM_TC_12_create_user,
    test_SIM_TC_12_create_user_missing_required_field_is_rejected,
)
from tests.component.test_SIM_TC_13_get_user import (
    test_SIM_TC_13_get_user,
    test_SIM_TC_13_get_user_empty_id_returns_full_list,
)
from tests.component.test_SIM_TC_14_update_user import (
    test_SIM_TC_14_update_user,
    test_SIM_TC_14_update_user_empty_id_returns_bad_request,
    test_SIM_TC_14_update_user_empty_last_name_is_rejected,
)
from tests.component.test_SIM_TC_15_delete_user import test_SIM_TC_15_delete_user

__all__ = [
    "test_SIM_TC_12_create_user",
    "test_SIM_TC_12_create_user_missing_required_field_is_rejected",
    "test_SIM_TC_13_get_user",
    "test_SIM_TC_13_get_user_empty_id_returns_full_list",
    "test_SIM_TC_14_update_user",
    "test_SIM_TC_14_update_user_empty_id_returns_bad_request",
    "test_SIM_TC_14_update_user_empty_last_name_is_rejected",
    "test_SIM_TC_15_delete_user",
]
