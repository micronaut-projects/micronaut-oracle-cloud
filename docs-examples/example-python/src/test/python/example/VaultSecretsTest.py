from typing import Annotated

from jakarta.inject import Inject
from micronaut.context.annotation import Property
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from example.MySecretsConfig import MySecretsConfig
from example.VaultSecrets import VaultSecrets


# The secrets read from Oracle Cloud Vault are supplied as test properties
@MicronautTest
@Property(name="SECRET_ONE", value="Value One")
@Property(name="SECRET_TWO", value="value two")
@Property(name="my-secrets.one", value="Value One")
@Property(name="my-secrets.two", value="value two")
@Property(name="my-secrets.three", value="true")
@Property(name="my-secrets.four", value="42")
@Property(name="my-secrets.five", value="3.16")
class VaultSecretsTest:
    vault_secrets: Annotated[VaultSecrets, Inject]
    my_secrets_config: Annotated[MySecretsConfig, Inject]

    @Test
    def test_secrets(self) -> None:
        assert self.vault_secrets.secret_one == "Value One"
        assert self.vault_secrets.secret_two == "value two"

    @Test
    def test_secrets_configuration(self) -> None:
        assert self.my_secrets_config.one == "Value One"
        assert self.my_secrets_config.two == "value two"
        assert self.my_secrets_config.three
        assert self.my_secrets_config.four == 42
        assert self.my_secrets_config.five == 3.16
