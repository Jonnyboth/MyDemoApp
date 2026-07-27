"""Gate de deploy del microservicio de usuarios: solo camino feliz crítico.

No define tests nuevos — reexporta (import) los escenarios ya escritos en
tests/tests/component/, así que si su firma/payload/schema cambia, este
archivo no necesita tocarse.
"""

from tests.component.test_SIM_TC_12_create_user import test_SIM_TC_12_create_user
from tests.component.test_SIM_TC_13_get_user import test_SIM_TC_13_get_user
from tests.component.test_SIM_TC_14_update_user import test_SIM_TC_14_update_user
from tests.component.test_SIM_TC_15_delete_user import test_SIM_TC_15_delete_user

__all__ = [
    "test_SIM_TC_12_create_user",
    "test_SIM_TC_13_get_user",
    "test_SIM_TC_14_update_user",
    "test_SIM_TC_15_delete_user",
]
