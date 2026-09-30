from typing import Annotated

from com.oracle.bmc.vault import Vaults
from jakarta.inject import Inject, Singleton


@Singleton
class VaultService:

    # tag::inject[]
    vault: Annotated[Vaults, Inject]
    # end::inject[]
