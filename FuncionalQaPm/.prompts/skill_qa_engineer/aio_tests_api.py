#!/usr/bin/env python3
"""
API HTTP (FastAPI) que expone las operaciones de aio_tests_client.py como
endpoints propios, para que agentes/herramientas de este proyecto puedan
crear, actualizar y listar Casos de Prueba en AIO Tests vía HTTP local en
lugar de invocar el script CLI directamente.

Ejecución:
    uvicorn aio_tests_api:app --reload --port 8000

Documentación interactiva una vez levantado:
    http://127.0.0.1:8000/docs
"""

from __future__ import annotations

from typing import Optional

import requests
from fastapi import FastAPI, HTTPException, Query
from pydantic import BaseModel, Field

import aio_tests_client as aio

app = FastAPI(
    title="AIO Tests Bridge — QA",
    description="Puente HTTP local hacia la API REST de AIO Tests (Jira Cloud) para tu proyecto.",
    version="1.0.0",
)


# ---------------------------------------------------------------------------
# Modelos de entrada/salida (Pydantic)
# ---------------------------------------------------------------------------
class TestStepIn(BaseModel):
    step: str = Field(..., description="Acción a ejecutar en el paso")
    test_data: str = Field("", description="Datos de prueba usados en el paso (opcional)")
    expected_result: str = Field(..., description="Resultado esperado tras ejecutar el paso")


class TestCaseCreateIn(BaseModel):
    title: str = Field(..., description="Título del Caso de Prueba")
    description: str = Field("", description="Descripción/objetivo del caso")
    precondition: str = Field("", description="Precondiciones necesarias antes de ejecutar el caso")
    priority: str = Field("Medium", description="Prioridad: High | Medium | Low")
    labels: list[str] = Field(default_factory=list, description="Etiquetas opcionales")
    steps: list[TestStepIn] = Field(..., description="Pasos desglosados del caso")
    project_key: Optional[str] = Field(None, description="Override del PROJECT_KEY por defecto")


class TestCaseUpdateIn(BaseModel):
    title: Optional[str] = None
    description: Optional[str] = None
    precondition: Optional[str] = None
    priority: Optional[str] = None
    labels: Optional[list[str]] = None
    steps: Optional[list[TestStepIn]] = None
    project_key: Optional[str] = Field(None, description="Override del PROJECT_KEY por defecto")


# ---------------------------------------------------------------------------
# Helper de manejo de errores: traduce las excepciones del cliente AIO Tests
# a códigos de estado HTTP coherentes para quien consuma esta API.
# ---------------------------------------------------------------------------
def _call_or_raise(fn, *args, **kwargs) -> dict:
    try:
        return fn(*args, **kwargs)
    except aio.AioTestsConfigError as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc
    except aio.AioTestsAPIError as exc:
        raise HTTPException(status_code=exc.status_code, detail=exc.message) from exc
    except requests.exceptions.RequestException as exc:
        raise HTTPException(status_code=502, detail=f"Error de red hacia AIO Tests: {exc}") from exc


# ---------------------------------------------------------------------------
# Endpoints
# ---------------------------------------------------------------------------
@app.get("/health")
def health() -> dict:
    """Chequeo simple de que el servicio está arriba y qué proyecto usa por defecto."""
    return {"status": "ok", "default_project_key": aio.PROJECT_KEY, "base_url": aio.AIO_BASE_URL}


@app.post("/test-cases", status_code=201)
def create_test_case_endpoint(payload: TestCaseCreateIn) -> dict:
    """Crea un nuevo Caso de Prueba en AIO Tests."""
    body = payload.model_dump(exclude={"project_key"})
    return _call_or_raise(aio.create_test_case, body, project_key=payload.project_key)


@app.put("/test-cases/{test_case_id}")
def update_test_case_endpoint(test_case_id: str, payload: TestCaseUpdateIn) -> dict:
    """Actualiza (parcialmente) un Caso de Prueba existente por su ID."""
    body = payload.model_dump(exclude={"project_key"}, exclude_none=True)
    if not body:
        raise HTTPException(status_code=400, detail="No se enviaron campos para actualizar.")
    return _call_or_raise(aio.update_test_case, test_case_id, body, project_key=payload.project_key)


@app.get("/test-cases/{test_case_id}")
def get_test_case_endpoint(test_case_id: str, project_key: Optional[str] = None) -> dict:
    """Obtiene el detalle de un Caso de Prueba puntual."""
    return _call_or_raise(aio.get_test_case, test_case_id, project_key=project_key)


@app.get("/test-cases")
def list_test_cases_endpoint(
    project_key: Optional[str] = None,
    max_results: int = Query(50, ge=1, le=200),
    start_at: int = Query(0, ge=0),
) -> dict:
    """Lista los Casos de Prueba del proyecto, con paginación."""
    return _call_or_raise(
        aio.list_test_cases,
        project_key=project_key,
        max_results=max_results,
        start_at=start_at,
    )


@app.get("/test-cases/search/by-title")
def search_test_cases_endpoint(
    title_contains: str,
    project_key: Optional[str] = None,
    max_results: int = Query(50, ge=1, le=200),
    start_at: int = Query(0, ge=0),
) -> dict:
    """Busca Casos de Prueba cuyo título contenga el texto dado (para detectar duplicados)."""
    return _call_or_raise(
        aio.search_test_cases,
        title_contains,
        project_key=project_key,
        max_results=max_results,
        start_at=start_at,
    )


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("aio_tests_api:app", host="0.0.0.0", port=8000, reload=True)
