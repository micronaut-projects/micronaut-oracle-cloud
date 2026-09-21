from micronaut.context.annotation import ConfigurationProperties


# tag::class[]
@ConfigurationProperties("my-secrets")
class MySecretsConfig:
    one: str | None = None
    two: str | None = None
    three: bool = False
    four: int = 0
    five: float | None = None
# end::class[]
