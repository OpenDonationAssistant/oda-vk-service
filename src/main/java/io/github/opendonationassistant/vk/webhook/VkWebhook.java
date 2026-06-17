package io.github.opendonationassistant.vk.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class VkWebhook {

  private final Logger log = LoggerFactory.getLogger(VkWebhook.class);
  private final List<VkEventHandler> handlers;
  private final ObjectMapper mapper;

  @Inject
  public VkWebhook(List<VkEventHandler> handlers, ObjectMapper mapper) {
    this.handlers = handlers;
    this.mapper = mapper;
  }

  @Post("/vklive/events")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Secured(SecurityRule.IS_ANONYMOUS)
  @ExecuteOn(TaskExecutors.BLOCKING)
  public void listenVklive(String message) {
    log.info("Received event: {}", message);
    try {
      var event = mapper.readValue(message, Event.class);
      handlers
        .stream()
        .filter(it -> it.canHandle(event.type()))
        .forEach(it -> it.handle(event).join());
    } catch (Exception e) {
      log.error("Failed to handle event", e.getMessage());
    }
  }

  @Serdeable
  public static record Event(
    String id,
    String type,
    @JsonProperty("user_id") String userId,
    Demand demand
  ) {}

  @Serdeable
  public static record Demand(
    Long id,
    User user,
    Reward reward,
    @JsonProperty("message_parts") List<Map<String, Part>> messageParts
  ) {}

  @Serdeable
  public static record User(String nick) {}

  @Serdeable
  public static record Reward(String id) {}

  @Serdeable
  public static record Part(String content) {}
}
