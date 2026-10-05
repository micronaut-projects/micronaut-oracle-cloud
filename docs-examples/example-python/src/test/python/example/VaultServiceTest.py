from typing import Annotated

from jakarta.inject import Inject
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from example.VaultService import VaultService


# The Vault client is created with the mock authentication details provider of src/test/java (package mock)
@MicronautTest
class VaultServiceTest:
    vault_service: Annotated[VaultService, Inject]

    @Test
    def test_vault_client_is_injected(self) -> None:
        assert self.vault_service.vault is not None
