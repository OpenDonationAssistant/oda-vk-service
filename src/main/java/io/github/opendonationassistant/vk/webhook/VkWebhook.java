package io.github.opendonationassistant.vk.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.vk.metrics.VkMetrics;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.serde.ObjectMapper;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;

@Controller
public class VkWebhook {

  private final ODALogger log = new ODALogger(this);
  private final List<VkEventHandler> handlers;
  private final ObjectMapper mapper;
  private final VkMetrics metrics;

  @Inject
  public VkWebhook(
    List<VkEventHandler> handlers,
    ObjectMapper mapper,
    VkMetrics metrics
  ) {
    this.handlers = handlers;
    this.mapper = mapper;
    this.metrics = metrics;
  }

  @Post("/vklive/events")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Secured(SecurityRule.IS_ANONYMOUS)
  @ExecuteOn(TaskExecutors.BLOCKING)
  public void listenVklive(String event) {
    Event payload;
    try {
      payload = mapper.readValue(event, Event.class);
    } catch (Exception e) {
      log.error("Failed to parse event", e);
      metrics.eventInvalid(null);
      return;
    }

    if (payload == null) {
      log.warn("Received invalid event", Map.of("reason", "empty payload"));
      metrics.eventInvalid(null);
      return;
    }

    var type = payload.type();
    if (type == null || payload.data() == null) {
      log.warn(
        "Received invalid event",
        Map.of("reason", "missing type or data")
      );
      metrics.eventInvalid(type);
      return;
    }

    var id = payload.id();
    log.info(
      "Received event",
      Map.of("type", type, "id", id == null ? VkMetrics.UNKNOWN : id)
    );
    log.debug("Handling event", Map.of("payload", payload));
    metrics.eventReceived(type);

    var matchingHandlers =
      handlers
        .stream()
        .filter(it -> it.canHandle(type))
        .toList();
    if (matchingHandlers.isEmpty()) {
      metrics.eventUnhandled(type);
      return;
    }

    try {
      var results = matchingHandlers
        .stream()
        .map(it -> it.handle(payload))
        .toList();
      CompletableFuture.allOf(
        results.toArray(CompletableFuture<?>[]::new)
      ).join();
      metrics.eventHandled(type);
    } catch (Exception e) {
      log.error("Failed to handle event", e);
      metrics.eventFailed(type);
    }
  }

  @Serdeable
  public static record Event(
    String id,
    String type,
    @JsonProperty("user_id") String userId,
    EventData data
  ) {}

  @Serdeable
  public static record EventData(
    @Nullable Demand demand,
    Follow follow,
    @Nullable @JsonProperty("stream_record") StreamRecord streamRecord,
    @Nullable Stream stream,
    @Nullable Subscription subscription,
    @Nullable Raid raid,
    @Nullable Channel channel,
    @Nullable Owner owner
  ) {}

  @Serdeable
  public static record Follow(
    @Nullable @JsonProperty("started_at") Long startedAt,
    Follower follower
  ) {}

  @Serdeable
  public static record Follower(
    @JsonProperty("nick_color") Integer nickColor,
    Long id,
    String nick,
    @JsonProperty("avatar_url") String avatarUrl
  ) {}

  @Serdeable
  public static record Channel(Long id, String url) {}

  @Serdeable
  public static record Owner(Long id) {}

  @Serdeable
  public static record Demand(
    Long id,
    @Nullable User user,
    @Nullable Reward reward,
    @Nullable String status,
    @Nullable @JsonProperty("message_parts") List<Map<String, Part>> messageParts
  ) {}

  @Serdeable
  public static record User(String nick) {}

  @Serdeable
  public static record Reward(String id) {}

  @Serdeable
  public static record Part(String content) {}

  @Serdeable
  public static record StreamRecord(
    String id,
    @JsonProperty("stream_started_at") Long streamStartedAt,
    Long duration,
    @JsonProperty("process_state") String processState,
    Category category,
    String title,
    @JsonProperty("video_id") String videoId,
    @JsonProperty("source_urls") List<SourceUrl> sourceUrls,
    @JsonProperty("created_at") Long createdAt,
    @JsonProperty("preview_url") String previewUrl
  ) {}

  @Serdeable
  public static record Category(String type, String title, String id) {}

  @Serdeable
  public static record SourceUrl(String type, String url) {}

  @Serdeable
  public static record Stream(
    String id,
    @JsonProperty("preview_url") String previewUrl,
    @Nullable @JsonProperty("ended_at") Long endedAt,
    @Nullable @JsonProperty("started_at") Long startedAt,
    String title,
    @JsonProperty("video_id") String videoId,
    Category category
  ) {}

  @Serdeable
  public static record Subscription(
    @Nullable Level level,
    @JsonProperty("days_duration") Long daysDuration,
    @Nullable Subscriber subscriber,
    Long amount,
    @Nullable @JsonProperty("renewed_at") Long renewedAt,
    @JsonProperty("started_at") Long startedAt
  ) {}

  @Serdeable
  public static record Level(String name, Long price, String id) {}

  @Serdeable
  public static record Subscriber(
    Long id,
    String nick,
    @JsonProperty("avatar_url") String avatarUrl,
    @JsonProperty("nick_color") Integer nickColor
  ) {}

  @Serdeable
  public static record Raid(
    @JsonProperty("raiders_count") Long raidersCount,
    @Nullable @JsonProperty("started_at") Long startedAt,
    RaidSource source
  ) {}

  @Serdeable
  public static record RaidSource(RaidOwner owner, Channel channel) {}

  @Serdeable
  public static record RaidOwner(
    @JsonProperty("nick_color") Integer nickColor,
    @JsonProperty("avatar_url") String avatarUrl,
    String nick,
    Long id
  ) {}
}
