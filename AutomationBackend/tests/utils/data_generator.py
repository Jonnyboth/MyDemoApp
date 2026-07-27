from __future__ import annotations

from faker import Faker

_faker = Faker()


def random_first_name() -> str:
    return _faker.first_name()


def random_last_name() -> str:
    return _faker.last_name()


def random_email() -> str:
    return _faker.email()


def random_age(min_age: int = 18, max_age: int = 65) -> int:
    return _faker.random_int(min=min_age, max=max_age)


def random_password(length: int = 12) -> str:
    return _faker.password(length=length)


def random_title() -> str:
    return _faker.sentence(nb_words=6)


def random_body() -> str:
    return _faker.paragraph(nb_sentences=3)
