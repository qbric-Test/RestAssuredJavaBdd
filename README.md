# RestAssuredJavaBdd

REST Assured + Cucumber BDD + JUnit 5 API automation framework, built on a
layered client / specification / model architecture.

## Stack

| Concern         | Tool                                         |
| --------------- | -------------------------------------------- |
| Language        | Java 17                                      |
| Build           | Maven                                        |
| API automation  | REST Assured 5                               |
| BDD runner      | Cucumber 7 on the JUnit 5 Platform           |
| Assertions      | JUnit 5 + Hamcrest + REST Assured            |
| Data mapping    | Jackson                                      |
| POJOs           | Lombok                                       |
| Schema checks   | `matchesJsonSchemaInClasspath()`             |
| Logging         | SLF4J + Logback                              |

## ⚠️ Read this first: reqres.in needs an API key

Since 2025 **every** reqres.in endpoint returns `401 missing_api_key` without a
personal key — including the read endpoints, and including the old public
`reqres-free-v1` value, which no longer works:

```bash
curl -s -o /dev/null -w "%{http_code}\n" https://reqres.in/api/users/2   # 401
```

Create a free key at [app.reqres.in/api-keys](https://app.reqres.in/api-keys),
then supply it in whichever way suits you:

```bash
mvn test -DapiKey=your-key-here
```

```bash
export API_KEY=your-key-here && mvn test
```

Or set `apiKey=` in `src/test/resources/config/config.properties`.

Without a key the `Given User API is available` step fails immediately with an
explanatory message, rather than letting all five scenarios fail on confusing
status assertions.

## Setup and run

```bash
mvn clean install -DskipTests
```

```bash
mvn test -DapiKey=your-key-here
```

| Command                                                | What it does                 |
| ------------------------------------------------------ | ---------------------------- |
| `mvn test`                                              | All scenarios                |
| `mvn test -Dcucumber.filter.tags="@auth"`               | Auth contract — needs no key |
| `mvn test -Dcucumber.filter.tags="@smoke"`              | Tagged scenarios only        |
| `mvn test -DbaseUri=https://staging.example.com`         | Point at another host        |
| `mvn test -Dcucumber.execution.parallel.enabled=false`  | Run serially                 |

Every key in `config.properties` can be overridden by a system property (`-Dkey=`)
or an environment variable — `apiKey` is also satisfied by `API_KEY`, the shape CI
systems use, so no credential ever needs committing.

## Layout

```
RestAssuredJavaBdd
├── pom.xml
├── src/test/java
│   ├── client/ApiClient.java              # get, post, put, patch, delete
│   ├── specs/RequestSpecBuilder.java      # base URI, headers, auth, timeouts
│   ├── specs/ResponseSpecBuilder.java     # status, content type, time budget
│   ├── models/UserRequest.java            # Lombok + Jackson POJOs
│   ├── models/UserResponse.java
│   ├── context/TestContext.java           # per-scenario shared state
│   ├── hooks/Hooks.java                   # setup, response attachment, cleanup
│   ├── stepdefinitions/UsersApiSteps.java # Gherkin bindings + assertions
│   ├── runners/TestRunner.java            # JUnit 5 Platform suite
│   └── utils/                             # ConfigReader, JsonUtils,
│                                          # LoggerUtility, TestDataFactory
├── src/test/resources
│   ├── features/users.feature            # the five CRUD scenarios
│   ├── features/auth.feature             # auth contract, runs without a key
│   ├── testdata/users.json                # payloads, no JSON strings in Java
│   ├── schemas/user-schema.json
│   ├── schemas/user-list-schema.json
│   ├── config/config.properties
│   ├── junit-platform.properties
│   └── logback.xml
├── reports                                # HTML / JSON / JUnit XML
└── test-results                           # api-execution.log
```

## Reports

- HTML report: `reports/cucumber-report.html`
- JSON / JUnit XML: `reports/cucumber-report.json`, `reports/cucumber-junit.xml`
- Execution log: `test-results/api-execution.log`

The After hook attaches the **response body, status, timing and request payload**
to every scenario — passing ones included. For an API suite the payload is the
evidence, and having it on a green run is what makes the report useful when the
API changes later.

## Design notes

**Layered.** Step definitions never build requests. They call `ApiClient`, which
uses the shared specification from `RequestSpecBuilder`. Adding an endpoint means
one client call and one step, with base URI, auth, timeouts and logging inherited.

**Credentials are masked in logs.** `LoggerUtility` replaces `x-api-key`,
`authorization` and `api-key` values with `****`, so a shared CI console or a
committed log never leaks a key. Verified: the raw value appears zero times in
the log file.

**Logging goes through SLF4J, not `System.out`.** REST Assured's own filters print
straight to standard out, which never reaches the log file and cannot be filtered
by level. Those filters are still available via `logRequests` / `logResponses`,
but the structured, file-backed logging is independent and always on.

**`UserResponse.id` is a String.** The read endpoints return `id` as a number,
but `POST /api/users` returns it quoted. A numeric field throws on create — a
trap worth knowing about with this API.

**`FAIL_ON_UNKNOWN_PROPERTIES` is off, deliberately.** A field added upstream
should not break every test; the suite asserts on what it cares about.

**204 gets its own response specification.** `ResponseSpecBuilder.forStatus()`
routes 204, 205 and 304 to a variant with no content-type expectation. Asserting
`ContentType.JSON` on a bodyless response fails a perfectly correct API.

**One shared `ObjectMapper`.** Construction is expensive and per-call instances
drift out of configuration with each other.

**Parallel by default.** API scenarios have no browser to isolate, so
`junit-platform.properties` ships with four workers. `TestContext` is scoped per
scenario by PicoContainer and nothing is held statically.

## Verified behaviour

### Green against reqres.in today, with no key

`features/auth.feature` passes right now — run it to confirm the whole stack is
wired up before you obtain a key:

```bash
mvn test -Dcucumber.filter.tags="@auth"
```

```
2 Scenarios (2 passed)
8 Steps (8 passed)
```

That exercises the real target API end to end: base URI and path resolution,
`ApiClient`, all three request specifications, the response specification
(status, content type, response-time budget), error-body parsing, logging,
hooks, report attachment and parallel execution.

The two scenarios also encode a genuine contract worth testing — reqres
distinguishes an *absent* credential from an *unrecognised* one, and the
distinction is easy to break:

| `x-api-key` sent   | Status | `error` code       |
| ------------------ | ------ | ------------------ |
| *(none)*           | 401    | `missing_api_key`  |
| unrecognised value | 403    | `invalid_api_key`  |
| `reqres-free-v1`   | 401    | `missing_api_key`  |

That last row is worth noting: the formerly public key is now treated as
*absent* rather than invalid, which is how you can tell it has been retired
rather than merely rejected.

### Verified by pointing the framework at a keyless public API

| Check                                                       | Result |
| ------------------------------------------------------------ | ------ |
| Compilation, Lombok annotation processing                     | Pass   |
| All steps bind to definitions, no undefined steps             | Pass   |
| `GET` sent, status + content type + response time asserted     | Pass   |
| `POST` with a Jackson-serialised body, 201 asserted            | Pass   |
| JSON schema validation via `matchesJsonSchemaInClasspath()`    | Pass   |
| Request / response logging to file, API key masked            | Pass   |
| Hooks, context sharing, report attachment                     | Pass   |
| Parallel execution across workers                             | Pass   |
| Missing-key guard produces an actionable message               | Pass   |

### Still unexercised — needs your key

These depend on reqres's specific response shapes: `response should contain user
id 2` (the `data` envelope), `users list should not be empty` (the `data`
array), `created user should be returned` (`createdAt`), and the two shipped
schemas. They are written against reqres's documented contract; run
`mvn test -DapiKey=<key>` once you have one.

## Adding an endpoint

1. Add the payload to `src/test/resources/testdata/`, and a POJO in `models/` if
   the response shape is new.
2. Call `ApiClient` from a step definition — the specification, auth and logging
   come for free.
3. Add a schema under `schemas/` and assert with
   `Then response should match schema "schemas/<name>.json"`.
