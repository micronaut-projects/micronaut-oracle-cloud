from typing import Annotated

from jakarta.inject import Singleton
from micronaut.context.annotation import Property, Value


@Singleton
class VaultSecrets:

    # tag::value[]
    secret_one: Annotated[str, Value("${SECRET_ONE}")]
    # end::value[]

    # tag::property[]
    secret_two: Annotated[str, Property(name="SECRET_TWO")]
    # end::property[]
