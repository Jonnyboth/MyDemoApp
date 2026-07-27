#!/usr/bin/env python3
"""Extrae endpoints únicos (y un cURL reconstruido) de un archivo .flow de mitmproxy.

Uso:
    ~/.venvs/mitmproxy/bin/python3 extract_endpoints.py captures/<archivo>.flow [--curl]

Sin --curl: lista un resumen (método, host, path, cuántas veces se repitió).
Con --curl: además imprime un `curl` reconstruido por cada endpoint único
(usa el primer flujo visto de ese endpoint como muestra), útil como insumo
directo para skill_api_test_designer / skill_api_automation_developer.
"""
from __future__ import annotations

import sys
from collections import OrderedDict

from mitmproxy.io import FlowReader


def _as_curl(flow) -> str:
    req = flow.request
    parts = [f"curl -X {req.method}"]
    for name, value in req.headers.items():
        if name.lower() in ("host", "content-length"):
            continue
        parts.append(f'-H "{name}: {value}"')
    if req.content:
        try:
            body = req.get_text()
        except Exception:
            body = None
        if body:
            parts.append(f"-d '{body}'")
    parts.append(f"'{req.pretty_url}'")
    return " ".join(parts)


def main() -> None:
    if len(sys.argv) < 2:
        print(f"Uso: {sys.argv[0]} <archivo.flow> [--curl]", file=sys.stderr)
        sys.exit(1)

    path = sys.argv[1]
    show_curl = "--curl" in sys.argv[2:]

    seen: "OrderedDict[tuple, dict]" = OrderedDict()
    with open(path, "rb") as f:
        for flow in FlowReader(f).stream():
            if flow.request is None:
                continue
            req = flow.request
            key = (req.method, req.scheme, req.pretty_host, req.path.split("?")[0])
            entry = seen.setdefault(key, {"count": 0, "flow": flow, "status": None})
            entry["count"] += 1
            if flow.response is not None:
                entry["status"] = flow.response.status_code

    if not seen:
        print("No se encontraron requests en el archivo. ¿La app realmente hizo llamadas de red?")
        return

    print(f"=== {len(seen)} endpoint(s) único(s) en {path} ===\n")
    for (method, scheme, host, path_only), entry in seen.items():
        status = entry["status"]
        print(f"{method:6s} {scheme}://{host}{path_only}  (x{entry['count']}, último status={status})")
        if show_curl:
            print(f"  {_as_curl(entry['flow'])}\n")


if __name__ == "__main__":
    main()
