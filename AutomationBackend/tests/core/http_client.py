from __future__ import annotations

import json
from typing import Any, Optional
from urllib.parse import urljoin

import requests
from requests import Response
from requests.exceptions import ConnectionError, Timeout
from tenacity import retry, retry_if_exception_type, stop_after_attempt, wait_exponential

from core import console_reporter
from core.logger import get_logger

logger = get_logger(__name__)


class HttpClient:
    """Envoltorio sobre requests.Session que loguea cada petición en formato cURL.

    Usa una única Session para reutilizar conexiones TCP y compartir headers
    (incluido el token de auth) entre llamadas del mismo test.
    """

    def __init__(self, base_url: str, default_headers: Optional[dict[str, str]] = None, timeout: int = 15) -> None:
        self._base_url = base_url.rstrip("/")
        self._timeout = timeout
        self._session = requests.Session()
        self._session.headers.update(default_headers or {"Content-Type": "application/json"})

    def set_auth_token(self, token: str, scheme: str = "Bearer") -> None:
        self._session.headers["Authorization"] = f"{scheme} {token}"

    def get(self, path: str, params: Optional[dict[str, Any]] = None, **kwargs: Any) -> Response:
        # GET es idempotente: seguro reintentar ante fallos transitorios de red.
        return self._request_with_retry("GET", path, params=params, **kwargs)

    def post(self, path: str, json_body: Optional[dict[str, Any]] = None, **kwargs: Any) -> Response:
        return self._request("POST", path, json=json_body, **kwargs)

    def put(self, path: str, json_body: Optional[dict[str, Any]] = None, **kwargs: Any) -> Response:
        return self._request("PUT", path, json=json_body, **kwargs)

    def patch(self, path: str, json_body: Optional[dict[str, Any]] = None, **kwargs: Any) -> Response:
        return self._request("PATCH", path, json=json_body, **kwargs)

    def delete(self, path: str, **kwargs: Any) -> Response:
        return self._request("DELETE", path, **kwargs)

    @retry(
        retry=retry_if_exception_type((ConnectionError, Timeout)),
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=0.5, min=0.5, max=4),
        reraise=True,
    )
    def _request_with_retry(self, method: str, path: str, **kwargs: Any) -> Response:
        return self._request(method, path, **kwargs)

    def _request(self, method: str, path: str, **kwargs: Any) -> Response:
        url = urljoin(f"{self._base_url}/", path.lstrip("/"))
        prepared = self._session.prepare_request(requests.Request(method=method, url=url, **kwargs))

        self._log_as_curl(prepared)
        console_reporter.print_request(method, prepared.url, self._extract_body(prepared))

        response = self._session.send(prepared, timeout=self._timeout)
        logger.info(
            "Respuesta [%s] %s -> status=%s (%.0fms)",
            method, url, response.status_code, response.elapsed.total_seconds() * 1000,
        )
        console_reporter.print_response(response.status_code, self._safe_json(response))
        return response

    @staticmethod
    def _extract_body(prepared: requests.PreparedRequest) -> Any:
        if not prepared.body:
            return None
        raw = prepared.body if isinstance(prepared.body, str) else prepared.body.decode("utf-8")
        try:
            return json.loads(raw)
        except (json.JSONDecodeError, TypeError):
            return raw

    @staticmethod
    def _safe_json(response: Response) -> Any:
        try:
            return response.json()
        except ValueError:
            return response.text or None

    def _log_as_curl(self, prepared: requests.PreparedRequest) -> None:
        headers = " ".join(f'-H "{k}: {v}"' for k, v in prepared.headers.items())
        body = ""
        if prepared.body:
            raw = prepared.body if isinstance(prepared.body, str) else prepared.body.decode("utf-8")
            try:
                body = f"-d '{json.dumps(json.loads(raw))}'"
            except (json.JSONDecodeError, TypeError):
                body = f"-d '{raw}'"

        curl_command = f"curl -X {prepared.method} {headers} {body} '{prepared.url}'".strip()
        logger.debug("cURL equivalente:\n%s", curl_command)
