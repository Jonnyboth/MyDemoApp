class ApiRequestError(Exception):
    """Error de red o de comunicación al ejecutar una petición HTTP."""


class ApiAssertionError(AssertionError):
    """Se lanza cuando una aserción sobre una respuesta de API falla."""


class SchemaValidationError(ApiAssertionError):
    """Se lanza cuando el body de una respuesta no cumple el JSON Schema esperado."""
