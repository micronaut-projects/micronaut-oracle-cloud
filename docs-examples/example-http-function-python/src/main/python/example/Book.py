from dataclasses import dataclass
from typing import Annotated

from jakarta.validation.constraints import Min, NotBlank, NotNull
from micronaut.serde.annotation import Serdeable


@Serdeable
@dataclass
class Book:
    title: Annotated[str, NotBlank, NotNull]
    pages: Annotated[int, Min(100)]
