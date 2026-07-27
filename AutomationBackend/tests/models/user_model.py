from __future__ import annotations

from pydantic import BaseModel


class CreateUserRequest(BaseModel):
    firstName: str
    lastName: str
    age: int
