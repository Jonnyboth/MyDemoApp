#!/usr/bin/env python3
"""
Crea una Historia de Usuario (HU) en el Backlog de Jira.

Vía primaria: API REST de Jira Cloud (POST /rest/api/3/issue).
Vía de contingencia: si la llamada REST falla por cualquier motivo
(credenciales, red, respuesta de error de Jira), el script NO falla en
silencio. Imprime un bloque FALLBACK_MCP_INSTRUCTION en JSON con los
campos ya normalizados para que el agente de IA complete la creación
usando el MCP de Atlassian/Jira configurado en el entorno.

Variables de entorno requeridas para la vía REST (ver .env en esta misma
carpeta, symlink al .env global del proyecto):
    JIRA_BASE_URL   -> https://tuempresa.atlassian.net
    JIRA_EMAIL      -> correo de la cuenta con permisos de creación de issues
    JIRA_API_TOKEN  -> API token de Atlassian
"""

import argparse
import json
import os
import sys

import requests

try:
    from dotenv import load_dotenv

    load_dotenv(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".env"))
except ImportError:
    pass

DEFAULT_ISSUE_TYPE = "Story"
REQUEST_TIMEOUT_SECONDS = 15


def _text_to_adf(text: str) -> dict:
    """Convierte texto plano (con saltos de línea) al formato ADF de Jira Cloud."""
    paragraphs = text.split("\n\n") if text else [""]
    content = []
    for paragraph in paragraphs:
        lines = paragraph.split("\n")
        paragraph_content = []
        for i, line in enumerate(lines):
            if i > 0:
                paragraph_content.append({"type": "hardBreak"})
            if line:
                paragraph_content.append({"type": "text", "text": line})
        if not paragraph_content:
            paragraph_content = [{"type": "text", "text": " "}]
        content.append({"type": "paragraph", "content": paragraph_content})
    return {"type": "doc", "version": 1, "content": content}


def _build_description(description: str, acceptance_criteria: str) -> str:
    return (
        f"{description.strip()}\n\n"
        f"### Criterios de Aceptación (BDD)\n\n"
        f"{acceptance_criteria.strip()}"
    )


def create_user_story(
    project_key: str,
    summary: str,
    description: str,
    acceptance_criteria: str,
    issue_type: str = DEFAULT_ISSUE_TYPE,
) -> dict:
    """
    Intenta crear la HU vía API REST de Jira. Si falla, retorna un payload
    de contingencia listo para que el agente lo ejecute vía MCP.
    """
    full_description = _build_description(description, acceptance_criteria)

    payload_for_agent = {
        "project_key": project_key,
        "summary": summary,
        "description": full_description,
        "issue_type": issue_type,
        "target": "Backlog",
    }

    base_url = os.environ.get("JIRA_BASE_URL")
    email = os.environ.get("JIRA_EMAIL")
    api_token = os.environ.get("JIRA_API_TOKEN")

    if not base_url or not email or not api_token:
        return _fallback(
            payload_for_agent,
            reason=(
                "Faltan variables de entorno JIRA_BASE_URL / JIRA_EMAIL / "
                "JIRA_API_TOKEN para autenticar contra la API REST de Jira."
            ),
        )

    url = f"{base_url.rstrip('/')}/rest/api/3/issue"
    body = {
        "fields": {
            "project": {"key": project_key},
            "summary": summary,
            "issuetype": {"name": issue_type},
            "description": _text_to_adf(full_description),
        }
    }

    try:
        response = requests.post(
            url,
            json=body,
            auth=(email, api_token),
            headers={"Content-Type": "application/json"},
            timeout=REQUEST_TIMEOUT_SECONDS,
        )
        response.raise_for_status()
        data = response.json()
        issue_key = data.get("key")
        return {
            "status": "created",
            "method": "rest_api",
            "issue_key": issue_key,
            "issue_url": f"{base_url.rstrip('/')}/browse/{issue_key}" if issue_key else None,
            "raw_response": data,
        }
    except requests.exceptions.RequestException as exc:
        detail = str(exc)
        if getattr(exc, "response", None) is not None:
            try:
                detail = json.dumps(exc.response.json())
            except ValueError:
                detail = exc.response.text
        return _fallback(
            payload_for_agent,
            reason=f"Fallo en la llamada REST a Jira: {detail}",
        )


def _fallback(payload_for_agent: dict, reason: str) -> dict:
    instruction = {
        "status": "fallback_required",
        "method": "mcp_atlassian",
        "reason": reason,
        "action_for_agent": (
            "La creación vía API REST no fue posible. Usa la herramienta del "
            "MCP de Atlassian/Jira configurada en este entorno para crear el "
            "issue con los campos de 'payload' a continuación, destinado al "
            "Backlog del proyecto (sin sprint asignado)."
        ),
        "payload": payload_for_agent,
    }
    print("FALLBACK_MCP_INSTRUCTION:", json.dumps(instruction, ensure_ascii=False, indent=2))
    return instruction


def _parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Crea una Historia de Usuario en el Backlog de Jira (REST con fallback a MCP)."
    )
    parser.add_argument("--project-key", required=True, help="Clave del proyecto Jira, ej. TP")
    parser.add_argument("--summary", required=True, help="Título corto de la HU")
    parser.add_argument("--description", required=True, help="Narrativa Como/Quiero/Para + contexto")
    parser.add_argument(
        "--acceptance-criteria",
        required=True,
        help="Criterios de aceptación en formato Gherkin (Dado/Cuando/Entonces)",
    )
    parser.add_argument(
        "--issue-type",
        default=DEFAULT_ISSUE_TYPE,
        help=f"Tipo de issue (por defecto: {DEFAULT_ISSUE_TYPE})",
    )
    return parser.parse_args()


def main() -> None:
    args = _parse_args()
    result = create_user_story(
        project_key=args.project_key,
        summary=args.summary,
        description=args.description,
        acceptance_criteria=args.acceptance_criteria,
        issue_type=args.issue_type,
    )
    if result["status"] == "created":
        print(f"Issue creado correctamente: {result['issue_key']} ({result['issue_url']})")
        sys.exit(0)
    else:
        # El bloque FALLBACK_MCP_INSTRUCTION ya se imprimió dentro de _fallback().
        sys.exit(1)


if __name__ == "__main__":
    main()
