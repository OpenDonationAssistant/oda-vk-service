package io.github.opendonationassistant.vk.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.github.opendonationassistant.commons.logging.ODALogger;
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

  private ODALogger log = new ODALogger(this);
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
  public void listenVklive(String event) {
    log.info("Received event", Map.of("event", event));
    try {
      var payload = mapper.readValue(event, Event.class);
      log.debug("Handling event", Map.of("event", payload));
      handlers
        .stream()
        .filter(it -> it.canHandle(payload.type()))
        .forEach(it -> it.handle(payload).join());
    } catch (Exception e) {
      e.printStackTrace();
      log.error("Failed to handle event", Map.of("error", e.getMessage()));
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
  public static record EventData(Demand demand){}

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
}
