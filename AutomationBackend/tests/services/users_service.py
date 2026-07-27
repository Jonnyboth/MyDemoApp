from __future__ import annotations

from typing import Any

from requests import Response

from config.endpoints import UsersEndpoints
from core.http_client import HttpClient
from models.user_model import CreateUserRequest


class UsersService:
    """Encapsula las operaciones CRUD del dominio de usuarios sobre HttpClient."""

    def __init__(self, client: HttpClient) -> None:
        self._client = client

    def create_user(self, payload: CreateUserRequest) -> Response:
        return self._client.post(UsersEndpoints.ADD, json_body=payload.model_dump())

    def get_user(self, user_id: int | str) -> Response:
        return self._client.get(UsersEndpoints.by_id(user_id))

    def update_user(self, user_id: int | str, payload: dict[str, Any]) -> Response:
        return self._client.put(UsersEndpoints.by_id(user_id), json_body=payload)

    def delete_user(self, user_id: int) -> Response:
        return self._client.delete(UsersEndpoints.by_id(user_id))
