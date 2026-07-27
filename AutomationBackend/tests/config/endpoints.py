class AuthEndpoints:
    """Rutas relativas del dominio de autenticación."""

    LOGIN = "/auth/login"


class UsersEndpoints:
    """Rutas relativas del dominio de usuarios."""

    BASE = "/users"
    ADD = "/users/add"

    @staticmethod
    def by_id(user_id: int) -> str:
        return f"/users/{user_id}"


class PostsEndpoints:
    """Rutas relativas del dominio de posts (host: jsonplaceholder)."""

    BASE = "/posts"

    @staticmethod
    def by_id(post_id: int) -> str:
        return f"/posts/{post_id}"
