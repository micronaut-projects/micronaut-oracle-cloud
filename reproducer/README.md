# OCI synchronous retries blocking Netty

`SyncRetryEventLoopTest` reproduces an OCI synchronous DELETE that times out and
retries through the Micronaut Netty provider. It uses loopback HTTP only; OCI
credentials and cloud resources are unnecessary.

Use the JDK required by the target branch and run from the repository root:

```sh
./gradlew :micronaut-oraclecloud-httpclient-netty:test --tests '*SyncRetryEventLoopTest'
```

The four cases cover the current and legacy adapters, each with a shared event
loop and a dedicated five-thread OCI pool. They keep
`micronaut.server.thread-selection=BLOCKING`, use a 200 ms read timeout and a 2 s
retry delay, and probe the actual `/health` endpoint during retry backoff with
`HttpURLConnection`, independent of the Micronaut event loops.

Assertions require the retry delay to execute on the synchronous caller thread,
a responsive health endpoint, two HTTP attempts, and the original read-timeout
failure after attempts are exhausted.

## Reproduce without the fix

On this fix commit, revert just the production edits while keeping the test:

```sh
git show --format= HEAD -- oraclecloud-httpclient-netty/src/main/java/io/micronaut/oraclecloud/httpclient/netty/{NettyHttpClient,MicronautHttpRequest,NettyHttpRequest}.java > /tmp/oci-sync-retry-fix.patch
git apply -R /tmp/oci-sync-retry-fix.patch
./gradlew :micronaut-oraclecloud-httpclient-netty:test --tests '*SyncRetryEventLoopTest'
git apply /tmp/oci-sync-retry-fix.patch
```

The original callback path calculates retry delay on a Netty event loop. A shared
loop also causes the health probe to time out. Isolation keeps the health probe
responsive while the OCI loop still sleeps. The test prints the callback thread,
its stack, and the health probe result in the Gradle test report.

## Fix

OCI `ClientCall.callSync()` selects `WaiterScheduler.SYNC` and supplies a
`SyncFutureWaiter` through `HttpRequest.offloadExecutor(...)`. That executor queues
work for the thread waiting in `callSync()`.

Failed HTTP futures previously completed on the transport event loop. The SDK's
retrier handles the timeout there and `WaiterScheduler.SYNC.schedule()` sleeps
there. Starting the synchronous call on a blocking executor does not move this
completion.

The adapter now dispatches failed completion through the supplied executor using
`exceptionallyComposeAsync(CompletableFuture::failedFuture, executor)`. This
preserves the exception and retry policy while moving backoff onto the waiting
caller. Both adapters use the same helper. No public API or dependency changes
are required.

## Isolation workaround

For a managed HTTP client whose service ID is `oci`:

```properties
micronaut.netty.event-loops.oci-pool.num-threads=5
micronaut.http.services.oci.event-loop-group=oci-pool
```

Use `num-threads`, not `num-of-threads`. Match the service ID used by the actual
client, or select the group on a custom `ServiceHttpClientConfiguration` with
`setEventLoopGroup("oci-pool")`. An unmanaged provider does not use these settings.

Isolation protects server I/O but leaves OCI threads vulnerable to retry sleeps.
The server's `thread-selection=BLOCKING` governs handler execution; accepting
connections and reading and writing HTTP messages still require event loops.
