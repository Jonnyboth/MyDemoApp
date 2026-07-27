from __future__ import annotations

import os
from dataclasses import dataclass

from dotenv import load_dotenv

load_dotenv()


@dataclass(frozen=True)
class EnvironmentConfig:
    name: str
    base_url: str
    username: str
    password: str
    timeout: int = 15


_ENV_TIERS = ("dev", "qa", "prod")

# Cada dominio de negocio puede vivir en un backend distinto. La variable leída
# de .env sigue el patrón {SERVICE}_{TIER}_BASE_URL, ej. USERS_QA_BASE_URL,
# POSTS_QA_BASE_URL. Agregar un servicio nuevo es agregar una línea acá.
_DEFAULT_BASE_URLS: dict[str, str] = {
    "users": "https://dummyjson.com",
    "posts": "https://jsonplaceholder.typicode.com",
}


def get_environment(service: str = "users") -> EnvironmentConfig:
    """Construye la configuración del ambiente activo para un servicio/dominio dado.

    ENV=qa (default) | dev | prod selecciona el tier. `service` selecciona QUÉ
    host usar ("users", "posts", ...), porque un mismo framework puede automatizar
    varios backends a la vez, cada uno con su propio ciclo de ambientes.
    """
    env_name = os.getenv("ENV", "qa").lower()
    if env_name not in _ENV_TIERS:
        raise ValueError(f"Ambiente '{env_name}' no soportado. Usa uno de: {_ENV_TIERS}")
    if service not in _DEFAULT_BASE_URLS:
        raise ValueError(f"Servicio '{service}' no soportado. Usa uno de: {list(_DEFAULT_BASE_URLS)}")

    var_name = f"{service.upper()}_{env_name.upper()}_BASE_URL"
    base_url = os.getenv(var_name, _DEFAULT_BASE_URLS[service])
    return EnvironmentConfig(
        name=env_name,
        base_url=base_url,
        username=os.getenv("API_USERNAME", ""),
        password=os.getenv("API_PASSWORD", ""),
        timeout=int(os.getenv("API_TIMEOUT", "15")),
    )
