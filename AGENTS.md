# PROJECT KNOWLEDGE BASE

**Generated:** 2026-09-11
**Commit:** f8ac9e5
**Branch:** main

## OVERVIEW

Java 25 / Micronaut 5.1.0 microservice bridging VK Live streaming events into the OpenDonationAssistant (ODA) ecosystem. Handles VK webhooks (stream events, subscriptions, rewards, follows, raids), processes commands via RabbitMQ, manages VK account linkage. Part of a larger ODA microservice architecture.

## STRUCTURE

```
oda-vk-service/
├── src/main/java/io/github/opendonationassistant/
│   ├── Application.java          # Entry point + RabbitMQ factory beans
│   ├── CommandListener.java      # RabbitMQ "vk.command" queue listener
│   ├── integration/              # VK API client layer (VkClient, VkDataClient)
│   └── vk/                       # Core domain (see vk/AGENTS.md)
│       ├── account/              # VK account link/unlink
│       ├── command/              # Socket channel commands (WIP)
│       ├── events/               # Outbound event DTOs
│       ├── metrics/              # Micrometer counters
│       ├── reward/               # Channel point reward system
│       └── webhook/              # VK Live webhook endpoint + handlers
├── src/main/resources/
│   ├── application.yml           # Main config (JWT, Flyway, metrics)
│   ├── application-standalone.yml # Standalone profile (default)
│   ├── application-allinone.yml   # All-in-one profile (tests)
│   └── db/migration/             # Flyway SQL migrations
├── src/test/java/                # JUnit 5 + Mockito tests
├── pom.xml                       # Maven build (Micronaut parent)
├── Dockerfile                    # Fedora 41 + native binary
└── .github/workflows/maven.yml   # CI (delegates to org reusable workflow)
```

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| Add new VK event type | `vk/webhook/handlers/` | Implement `VkEventHandler` interface |
| Modify VK API calls | `integration/VkClient.java` | Wraps `VkDataClient` (Micronaut declarative HTTP) |
| Add DB schema change | `src/main/resources/db/migration/` | Flyway: `V{N}__description.sql` |
| Add RabbitMQ binding | `Application.rabbitConfiguration()` | Register `Exchange` with routing keys |
| Add command handler | `vk/account/` | Extend `AbstractMessageHandler<T>` |
| Modify webhook payload | `VkWebhook.Event` record | Nested `@Serdeable` records inside controller |
| Add metrics | `vk/metrics/VkMetrics.java` | Micrometer counters with tags |
| Change deployment profile | `application-{profile}.yml` | Default is `standalone` |

## ARCHITECTURE

```
VK Live Platform ──POST /vklive/events──▶ VkWebhook ──dispatch──▶ 7 VkEventHandlers
                                                                      │
RabbitMQ "commands" queue ──▶ CommandListener ──▶ Link/Unlink handlers
                                                                      │
RabbitMQ "changes.widgets" ──▶ WidgetChangedEventHandler              │
                                                                      │
                                              ┌───────────────────────┘
                                              ▼
                                    VkClient ──▶ VkDataClient ──▶ external vklive-data service
                                              │
                                    TokenRPC (RabbitMQ RPC for auth tokens)
                                              │
                                    PostgreSQL (accounts + reward tables)
```

**3 inbound channels:** HTTP webhook (anonymous), RabbitMQ command queue, RabbitMQ widget-change queue
**1 outbound HTTP:** VkDataClient to vklive-data service
**1 outbound RabbitMQ:** Normalized events via `@Named("vklive") RabbitClient`
**1 authenticated endpoint:** `GetSocketChannels` (POST, JWT required)

## EXTERNAL DEPENDENCY: oda-rabbit-conf

Critical shared library (`io.github.opendonationassistant:oda-rabbit-conf:0.11.232`) provides:
- `RabbitClient` / `TokenRPC` - RabbitMQ messaging + token RPC
- `MessageProcessor` / `AbstractMessageHandler` - Command processing framework
- `BaseController` - Base HTTP controller with auth helpers
- `ODALogger` - Structured logging wrapper
- `Exchange` / `Queue` / `AMQPConfiguration` - RabbitMQ binding model

**Cannot fully trace application flow without this library.** When modifying command/event handling, check `oda-rabbit-conf` source.

## CONVENTIONS (DEVIATIONS FROM STANDARD)

- **2-space indentation** (no formatter config; enforced by practice)
- **Records everywhere** - entities, DTOs, events, commands are all Java records
- **Nested record DTOs** - HTTP payloads defined as `public static record` inside controller/handler classes
- **`Vk` vs `VK` dual naming** - service classes use `Vk` prefix, external event types use `VK` (all-caps)
- **No reactive streams** - `CompletableFuture` exclusively; Reactor/RxJava explicitly excluded in AOT config
- **`var` extensively** - local variable type inference used throughout
- **Constructor injection only** - `@Inject` on constructors, `private final` fields
- **`ODALogger` with `Map.of()`** - structured logging, never `System.out` or `printStackTrace` (except one known violation in VkWebhook)

## ANTI-PATTERNS (KNOWN)

| Issue | File | Severity |
|-------|------|----------|
| `e.printStackTrace()` in production code | `VkWebhook.java:68` | HIGH - replace with `log.error()` |
| Broad `catch (Exception e)` | `VkWebhook.java:67` | MEDIUM - separate deserialization from handler errors |
| `.join()` in sequential `forEach` | `VkWebhook.java:65` | MEDIUM - blocks thread, no parallel execution |
| Non-final logger fields | `VkWebhook.java:25`, `WidgetChangedEventHandler.java:27` | LOW |
| Commented-out dead code | `VkClient.java:79-98` | LOW - remove or ticket |
| Unauthenticated management endpoints | `application.yml:35,37` | MEDIUM - verify intentional |

## COMMANDS

```bash
# Build (JAR)
mvn clean package

# Build (native image)
mvn clean package -Dpackaging=native-image

# Run tests
mvn test

# Run locally (standalone profile - needs PostgreSQL)
mvn mn:run

# Run tests (allinone profile - auto-provisions PostgreSQL)
mvn test  # uses Micronaut test resources
```

## NOTES

- **Default profile is `standalone`** (set in `Application.DefaultEnvironmentConfigurer`). Requires external PostgreSQL.
- **`allinone` profile** auto-provisions test resources for integration tests.
- **Thread pools are fixed-size with 1 thread each** for `command-listener` and `event-listener` executors - limits concurrency.
- **Webhook is synchronous** - `.join()` on each handler means HTTP response blocks until all handlers complete.
- **Dockerfile expects pre-built native binary** - build happens in CI, not in Docker.
- **CI fully delegated** to `OpenDonationAssistant/oda-libraries/.github/workflows/release_service.yml` - zero local CI logic.
- **JDK 25** is bleeding-edge; ensure dependencies are compatible.
- **NullAway in ERROR mode** - null-safety violations are compile errors, not warnings.
