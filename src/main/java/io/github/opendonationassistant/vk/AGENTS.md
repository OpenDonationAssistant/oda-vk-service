# VK DOMAIN

Core domain logic for VK Live integration. All business rules live here.

## STRUCTURE

```
vk/
├── account/         # Account aggregate + repository
│   ├── VkAccount.java              # Domain aggregate (holds rewards, dispatches commands)
│   ├── VkAccountData.java          # @MappedEntity persistence record
│   ├── VkAccountRepository.java    # Domain repository (converts Data → Domain)
│   ├── VkAccountDataRepository.java # Micronaut Data JDBC repository
│   ├── LinkAccountCommandHandler.java    # Handles "LinkVkAccount" command
│   └── UnlinkAccountCommandHandler.java  # Handles "UnlinkVkAccount" command
├── command/
│   └── GetSocketChannels.java      # Authenticated endpoint (WIP)
├── events/         # Outbound event DTOs (VKLive*Event records)
│   ├── VKLiveChannelFollowEvent.java
│   ├── VKLiveChannelSubscriptionEvent.java
│   ├── VKLiveRaidEvent.java
│   ├── VKLiveStreamEndedEvent.java
│   ├── VKLiveStreamStartedEvent.java
│   └── VKLiveStreamRecordEvent.java
├── metrics/
│   └── VkMetrics.java              # Micrometer counters (received/handled/unhandled/invalid/noaccount)
├── reward/
│   ├── Reward.java                 # Domain entity (calls VkClient for API ops)
│   ├── RewardData.java             # @MappedEntity persistence record
│   ├── RewardDataRepository.java   # Micronaut Data JDBC repository
│   └── WidgetChangedEventHandler.java # RabbitMQ listener for widget config changes
└── webhook/
    ├── VkWebhook.java              # HTTP controller + nested event DTOs
    ├── VkEventHandler.java         # Handler interface (canHandle + handle)
    └── handlers/                   # 7 concrete event handlers
        ├── ChannelFollowCreateHandler.java
        ├── PointsDemandCreateHandler.java
        ├── RaidStartHandler.java
        ├── StreamRecordCreateHandler.java
        ├── StreamStartHandler.java
        ├── StreamStopHandler.java
        └── SubscriptionRenewHandler.java
```

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| Add new VK webhook event | `webhook/handlers/` | Create handler implementing `VkEventHandler`, add to `VkWebhook` dispatch |
| Modify account lifecycle | `account/VkAccount.java` | Domain aggregate with reward + command logic |
| Add reward operation | `reward/Reward.java` | Calls `VkClient` for API operations |
| Change event DTO shape | `events/VKLive*Event.java` | Outbound records sent via RabbitMQ |
| Modify widget config handling | `reward/WidgetChangedEventHandler.java` | Parses widget properties, creates/updates rewards |

## PATTERNS

**Handler dispatch:** `VkWebhook` collects all `VkEventHandler` beans, filters by `canHandle(type)`, calls `handle()` on each. Handlers return `CompletableFuture<?>`.

**Repository pattern:** `VkAccountRepository` (domain) wraps `VkAccountDataRepository` (JDBC). Domain converts `VkAccountData` → `VkAccount` with injected dependencies.

**Event flow:** Webhook → Handler → looks up `VkAccount` → sends outbound event via `RabbitClient("vklive")` or calls `VkClient` for API operations.

## ANTI-PATTERNS

- `VkWebhook.java` has `e.printStackTrace()` and broad `catch (Exception e)` - fix before adding new handlers
- `VkWebhook.Event` record is 200+ lines of nested records - consider extracting to separate file if growing
- Handler tests duplicate `account()`/`demand()`/`follow()` helper methods - consider shared test fixtures
