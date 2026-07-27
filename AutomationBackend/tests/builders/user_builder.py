from __future__ import annotations

from models.user_model import CreateUserRequest
from utils.data_generator import random_age, random_first_name, random_last_name


class UserBuilder:
    """Builder encadenable para construir payloads de usuario de forma legible en los tests."""

    def __init__(self) -> None:
        self._first_name: str = random_first_name()
        self._last_name: str = random_last_name()
        self._age: int = random_age()

    def with_first_name(self, first_name: str) -> "UserBuilder":
        self._first_name = first_name
        return self

    def with_last_name(self, last_name: str) -> "UserBuilder":
        self._last_name = last_name
        return self

    def with_age(self, age: int) -> "UserBuilder":
        self._age = age
        return self

    def build(self) -> CreateUserRequest:
        return CreateUserRequest(firstName=self._first_name, lastName=self._last_name, age=self._age)
