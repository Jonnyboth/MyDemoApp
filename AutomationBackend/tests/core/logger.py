from __future__ import annotations

import logging
from pathlib import Path

_LOG_DIR = Path(__file__).resolve().parent.parent / "reports"
_LOG_DIR.mkdir(exist_ok=True)


def get_logger(name: str) -> logging.Logger:
    """Devuelve un logger configurado con salida a consola (WARNING+) y archivo (DEBUG).

    La consola solo muestra el detalle "bonito" de core.console_reporter (vía
    print/stdout); el logging estándar (stderr) queda reservado a warnings/errores
    para no entrelazarse ni duplicar esa salida. El archivo de log conserva todo,
    incluido el cURL de cada request, para depuración posterior.

    Se guarda en un registro por nombre de módulo para evitar handlers duplicados
    cuando pytest importa el mismo módulo varias veces durante el descubrimiento.
    """
    logger = logging.getLogger(name)
    if logger.handlers:
        return logger

    logger.setLevel(logging.DEBUG)

    formatter = logging.Formatter(
        fmt="%(asctime)s | %(levelname)-8s | %(name)s | %(message)s",
        datefmt="%Y-%m-%d %H:%M:%S",
    )

    console_handler = logging.StreamHandler()
    console_handler.setLevel(logging.WARNING)
    console_handler.setFormatter(formatter)

    file_handler = logging.FileHandler(_LOG_DIR / "test_execution.log", encoding="utf-8")
    file_handler.setLevel(logging.DEBUG)
    file_handler.setFormatter(formatter)

    logger.addHandler(console_handler)
    logger.addHandler(file_handler)
    logger.propagate = False
    return logger
