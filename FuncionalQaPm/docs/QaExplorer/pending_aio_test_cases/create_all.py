#!/usr/bin/env python3
"""Carga masiva de los Casos de Prueba de SIM_test_cases.json contra AIO Tests (proyecto SIM).

Uso:
    cd FuncionalQaPm
    .prompts/skill_qa_engineer/.venv/bin/python3 docs/QaExplorer/pending_aio_test_cases/create_all.py

Requiere AIO_API_TOKEN configurado en el .env (ver README.md de esta carpeta).
"""
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[3] / ".prompts" / "skill_qa_engineer"))

from aio_tests_client import (  # noqa: E402
    create_test_case,
    AioTestsConfigError,
    AioTestsAPIError,
)

JSON_PATH = Path(__file__).with_name("SIM_test_cases.json")
PROJECT_KEY = "SIM"


def main() -> None:
    test_cases = json.loads(JSON_PATH.read_text(encoding="utf-8"))
    created, failed = [], []
    for idx, tc in enumerate(test_cases, start=1):
        title = tc["title"]
        try:
            result = create_test_case(tc, project_key=PROJECT_KEY)
            key = result.get("key") or result.get("id") or "?"
            print(f"[{idx}/{len(test_cases)}] OK  {key}  {title}")
            created.append(key)
        except (AioTestsConfigError, AioTestsAPIError) as exc:
            print(f"[{idx}/{len(test_cases)}] FAIL {title}\n    -> {exc}")
            failed.append(title)

    print(f"\nResumen: {len(created)} creados, {len(failed)} fallidos de {len(test_cases)} totales.")
    if failed:
        sys.exit(1)


if __name__ == "__main__":
    main()
