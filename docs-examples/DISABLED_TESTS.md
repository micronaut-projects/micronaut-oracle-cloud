# Python Docs Disabled Test Inventory

This file tracks the Python documentation examples under `docs-examples/example-python`,
`docs-examples/example-function-python` and `docs-examples/example-http-function-python` that are present but
disabled, or that deviate from the Java example because the direct port does not compile or does not behave like
the Java example yet. It is the bug-fixing task list for the Python compiler (`micronaut-inject-python` /
`micronaut-context-python`); every row references a `TODO(python)` comment in the sources or a workaround
described below.

The Python examples are compiled by every build and their tests run with
`./gradlew pythonCheck -Ppython-ci` (the "Python CI" GitHub workflow).

## Reconciliation

- Last generated active `@Disabled` count: 2.
- Last generated command: `rg -n "@Disabled\(" docs-examples/example-python docs-examples/example-function-python docs-examples/example-http-function-python`.
- Last full-suite command: `./gradlew :micronaut-docs-examples:micronaut-example-python:test :micronaut-docs-examples:micronaut-example-function-python:test :micronaut-docs-examples:micronaut-example-http-function-python:test -Ppython-ci`.
- Last full-suite result: build successful, 12 tests executed, 2 skipped, 0 failures (`example-python`: 5 tests / 1 skipped, `example-function-python`: 1 test, `example-http-function-python`: 6 tests / 1 skipped).

## Migration Rules

- Do not define local copies of Micronaut annotation helpers or custom annotation shims in docs snippets.
  The Micronaut and OCI SDK classes are imported from their Java packages (`micronaut.http.annotation`,
  `micronaut.oraclecloud.core`, `com.oracle.bmc.objectstorage...`, `jakarta.inject`).
- Methods are snake_case (`list_buckets`, `handle_request`); route-bound argument names keep the name of the URI
  template variable (`compartmentId`).
- The example sources live in `src/main/python` (the guide uses `source="main"` snippets) and the tests in
  `src/test/python`; both roots are merged into one compilation by the `mergePythonSources` task of the
  `io.micronaut.build.internal.oraclecloud-python-example` convention plugin, see below.
- The tests are `@MicronautTest` classes. Like the Java examples, the OCI clients (`ObjectStorageClient`,
  `ObjectStorageAsyncClient`) and the authentication details/tenancy id providers are replaced by mock beans; the
  mocks subclass the OCI SDK client classes, which a Python class cannot do, so they are the Java classes of
  `src/test/java/mock` (a sibling of the Python `example` package, imported as `from mock import MockData`).

## Active `@Disabled` Tests

| Test | Reason |
| --- | --- |
| `example.BucketControllerTest.test_create_bucket` (`docs-examples/example-http-function-python`) | The Fn testing harness (`com.fnproject.fn.testing.FnTestingRule`) runs the function in a child-first classloader with its own application context and GraalPy runtime. Inside it the Python controller resolves the OCI model class `CreateBucketDetails` (`java.type` of the generated `com.oracle.bmc.objectstorage.model` shim) from the parent application classloader, while the request classes (`CreateBucketRequest`, ...) and the OCI client come from the function classloader, so `CreateBucketRequest.Builder.createBucketDetails(details)` fails (`TypeError: invalid instantiation of foreign object`; `body$` shows the underlying `ClassCastException: CreateBucketDetails (loader 'app') cannot be cast to CreateBucketDetails (loader FnTestingClassLoader)`). The other requests of the test only pass strings to the builders and pass. |
| `example.SdkImportsTest` (`docs-examples/example-python`) | `@SdkImport` is processed by `micronaut-oraclecloud-serde-processor`, which generates the client factory with Micronaut SourceGen. SourceGen has no `SourceGenerator` for `VisitorContext.Language.PYTHON`, so with the processor on the Python compile classpath the visitor fails the compilation of `example.SdkImports` (`Cannot process @SdkImport(). Missing SourceGenerator module from annotation processor path`); the processor is therefore left off the Python compile classpath (`docs-examples/example-python/build.gradle`), no `DisasterRecoveryClient` bean exists and the test is disabled. The guide carries a `[.lang-python]` note. |

## Commented Unsupported Snippet Ports

None.

## Workarounds Kept In Snippets

| Target | Reason |
| --- | --- |
| `io.micronaut.build.internal.oraclecloud-python-example` (`mergePythonSources`) | The documentation classes live in `src/main/python` and the tests in `src/test/python`; compiling them separately yields two GraalPy VFS roots whose generated shim modules shadow each other at test time, and the Python compiler resolves the imports of a source file only within its own source root, so both roots are merged into one directory compiled by `compileTestPython`. |
| `io.micronaut.build.internal.oraclecloud-python-example` (`PythonCompileClasspath` usage attribute) | The OCI SDK modules of this build (`micronaut-oraclecloud-bmc-*`) expose their generated client factories (`ObjectStorageReactorClient`, ...) and their dependencies (`micronaut-oraclecloud-common`) only through the `metadataElements` variant (java-runtime usage). The Python compile classpath requests the java-api usage and resolves the stripped `apiElements` variant instead, so the imported types silently become `Object` and the `micronaut.oraclecloud` shim package is not generated (`ModuleNotFoundError` at runtime); the convention plugin requests the java-runtime usage on the Python compile classpaths. Not a compiler issue as such, but the silent `Object` fallback hides it. |
| `example.ListBucketsFunction` (`docs-examples/example-function-python`) | A Python class cannot subclass the Java class `OciFunction`, which the Fn runtime instantiates reflectively and which starts the application context. The Python example is a plain `@Singleton` bean with the injected `ObjectStorageClient` and `TenancyIdProvider`; its test obtains it from the running application context instead of running the function through `com.fnproject.fn.testing.FnTestingRule` like the Java test. The guide carries a `[.lang-python]` warning. |
| `example.BucketControllerTest` (`docs-examples/example-http-function-python`) | The Java test invokes the function through `io.micronaut.oraclecloud.function.http.test.FnHttpTest`, which starts (and closes) a nested `ApplicationContext` to serialize the request body; closing it tears down the GraalPy runtime shared with the application context of the running Python test, so every test method after the first fails with `GraalPy context has not been initialized` (tracking item B3). The Python test invokes the function through the Fn testing harness with the Java helper `support.FnHttpInvoker` (`src/test/java`), which does not start a nested context. |
| `example.BookControllerTest` (`docs-examples/example-http-function-python`) | Every request through the embedded Fn test server (`micronaut-oraclecloud-function-http-test`) runs the function in a new Fn classloader with its own application context and GraalPy runtime (the Truffle runtime falls back to the interpreter because the Truffle jars are loaded by a second classloader), which takes a while and a lot of memory; `src/test/resources/application-test.properties` raises the HTTP client read timeout and the build file the heap size of the test JVM (four function invocations ran out of memory with the default heap). |

## Intentionally Unsupported Snippet Targets

| Target | Reason |
| --- | --- |
| `example.ListBucketsFunction` as the Oracle Functions entry point | The Fn runtime instantiates the function class reflectively before any application context exists and expects it to extend `OciFunction`, which a Python class cannot do; the deployed entry point must be a Java, Kotlin or Groovy class. The HTTP function example needs no such class: `io.micronaut.oraclecloud.function.http.HttpFunction` is the entry point and routes to the Python controllers. |

## java.type usages

None.
