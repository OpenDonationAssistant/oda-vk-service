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
import org.jspecify.annotations.Nullable;

@Controller
public class VkWebhook {

  private ODALogger log = new ODALogger(this);
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
    log.info("Received event", Map.of("event", event));
    try {
      var payload = mapper.readValue(event, Event.class);
      if (payload == null) {
        log.warn("Received invalid event", Map.of("event", event));
        metrics.eventInvalid(null);
        return;
      }
      log.debug("Handling event", Map.of("event", payload));
      metrics.eventReceived(payload.type());
      var matchingHandlers =
        handlers
          .stream()
          .filter(it -> it.canHandle(payload.type()))
          .toList();
      if (matchingHandlers.isEmpty()) {
        metrics.eventUnhandled(payload.type());
      } else {
        metrics.eventHandled(payload.type());
        matchingHandlers.forEach(it -> it.handle(payload).join());
      }
    } catch (Exception e) {
      e.printStackTrace();
      log.error("Failed to handle event", Map.of("error", e.getMessage()));
      metrics.eventInvalid(null);
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
    Demand demand,
    Follow follow,
    @Nullable @JsonProperty("stream_record") StreamRecord streamRecord,
    @Nullable Stream stream,
    @Nullable Subscription subscription,
    @Nullable Raid raid,
    Channel channel,
    Owner owner
  ) {}

  @Serdeable
  public static record Follow(
    @JsonProperty("started_at") Long startedAt,
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
    User user,
    Reward reward,
    String status,
    @JsonProperty("message_parts") List<Map<String, Part>> messageParts
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
    @JsonProperty("started_at") Long startedAt,
    String title,
    @JsonProperty("video_id") String videoId,
    Category category
  ) {}

  @Serdeable
  public static record Subscription(
    Level level,
    @JsonProperty("days_duration") Long daysDuration,
    Subscriber subscriber,
    Long amount,
    @JsonProperty("renewed_at") Long renewedAt,
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
    @JsonProperty("started_at") Long startedAt,
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
