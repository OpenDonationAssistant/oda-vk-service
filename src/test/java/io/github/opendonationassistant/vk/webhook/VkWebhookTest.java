package io.github.opendonationassistant.vk.webhook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.opendonationassistant.vk.metrics.VkMetrics;
import io.github.opendonationassistant.vk.webhook.VkWebhook.Event;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micronaut.serde.ObjectMapper;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@MicronautTest
public class VkWebhookTest {

  private static final String DEMAND_TYPE = "channel_points_reward_demand_create";
  private static final String FOLLOW_TYPE = "channel_follow_create";
  private static final String REWARD_ID =
    "39a919a7-fd10-4ece-9606-dde43b595fc9";
  private static final String MEDIA_LINK =
    "https://cdn.example.com/rewards/cat.mp4";

  private static final String DEMAND_PAYLOAD = """
    {
      "id": "6795254",
      "type": "%s",
      "user_id": 29586153,
      "data": {
        "demand": {
          "id": 8433849,
          "status": "pending",
          "user": {"id": 15346984, "nick": "stCarolas"},
          "reward": {"id": "%s"},
          "message_parts": [
            {"link": {"content": "%s"}}
          ]
        }
      }
    }
    """.formatted(DEMAND_TYPE, REWARD_ID, MEDIA_LINK);

  private static final String FOLLOW_PAYLOAD = """
    {
      "id": "6987495",
      "type": "%s",
      "user_id": 123123123,
      "data": {
        "follow": {
          "started_at": 1785489039,
          "follower": {
            "nick_color": 15,
            "id": 19219592,
            "nick": "OneTuz",
            "avatar_url": "https://images.example/avatar"
          }
        }
      }
    }
    """.formatted(FOLLOW_TYPE);

  private static final String UNKNOWN_TYPE_PAYLOAD = """
    {
      "id": "6987495",
      "type": "channel_donation_create",
      "user_id": 123123123,
      "data": {
        "channel": {"id": 12312311, "url": "someuser"},
        "owner": {"id": 123123123}
      }
    }
    """;

  @Inject
  ObjectMapper mapper;

  private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
  private final VkMetrics metrics = new VkMetrics(registry);
  private final VkEventHandler handler = mock(VkEventHandler.class);

  @Test
  public void passesCompleteDemandEventToHandler() {
    when(handler.type()).thenReturn(
      VkEventType.CHANNEL_POINTS_REWARD_DEMAND_CREATE
    );
    when(handler.handle(any(Event.class))).thenReturn(
      CompletableFuture.completedFuture(null)
    );

    webhookWith(handler).listenVklive(DEMAND_PAYLOAD).join();

    var captor = ArgumentCaptor.forClass(Event.class);
    verify(handler).handle(captor.capture());
    var event = captor.getValue();
    assertEquals("6795254", event.id());
    assertEquals(DEMAND_TYPE, event.type());
    assertEquals("29586153", event.userId());

    var data = event.data();
    var demand = Objects.requireNonNull(data.demand());
    assertEquals(8433849L, Objects.requireNonNull(demand.id()));
    assertEquals("pending", demand.status());
    assertEquals(REWARD_ID, Objects.requireNonNull(demand.reward()).id());

    var user = Objects.requireNonNull(demand.user());
    assertEquals("stCarolas", user.nick());
    assertEquals(
      15346984L,
      Objects.requireNonNull(user.id()).longValue()
    );

    var messageParts = Objects.requireNonNull(demand.messageParts());
    var link = messageParts
      .stream()
      .map(part -> part.get("link"))
      .filter(Objects::nonNull)
      .findFirst()
      .orElseThrow();
    assertEquals(MEDIA_LINK, link.content());
  }

  @Test
  public void countsReceivedAndHandledEvent() {
    when(handler.type()).thenReturn(VkEventType.CHANNEL_FOLLOW_CREATE);
    when(handler.handle(any(Event.class))).thenReturn(
      CompletableFuture.completedFuture(null)
    );

    webhookWith(handler).listenVklive(FOLLOW_PAYLOAD).join();

    verify(handler).handle(any(Event.class));
    assertEquals(1, events(VkMetrics.EVENTS_RECEIVED_METRIC_NAME, FOLLOW_TYPE));
    assertEquals(1, events(VkMetrics.EVENTS_HANDLED_METRIC_NAME, FOLLOW_TYPE));
    assertEquals(
      0,
      events(VkMetrics.EVENTS_UNHANDLED_METRIC_NAME, FOLLOW_TYPE)
    );
  }

  @Test
  public void countsUnhandledEvent() {
    when(handler.type()).thenReturn(VkEventType.RAID_START);

    webhookWith(handler).listenVklive(FOLLOW_PAYLOAD).join();

    verify(handler).type();
    assertEquals(1, events(VkMetrics.EVENTS_RECEIVED_METRIC_NAME, FOLLOW_TYPE));
    assertEquals(0, events(VkMetrics.EVENTS_HANDLED_METRIC_NAME, FOLLOW_TYPE));
    assertEquals(
      1,
      events(VkMetrics.EVENTS_UNHANDLED_METRIC_NAME, FOLLOW_TYPE)
    );
  }

  @Test
  public void countsUnhandledEventForUnknownType() {
    when(handler.type()).thenReturn(VkEventType.RAID_START);

    webhookWith(handler).listenVklive(UNKNOWN_TYPE_PAYLOAD).join();

    verify(handler, never()).handle(any(Event.class));
    assertEquals(
      1,
      events(VkMetrics.EVENTS_RECEIVED_METRIC_NAME, "channel_donation_create")
    );
    assertEquals(
      1,
      events(VkMetrics.EVENTS_UNHANDLED_METRIC_NAME, "channel_donation_create")
    );
    assertEquals(
      0,
      events(VkMetrics.EVENTS_HANDLED_METRIC_NAME, "channel_donation_create")
    );
  }

  @Test
  public void countsInvalidEventOnNullPayload() {
    webhookWithoutHandlers().listenVklive("null").join();

    assertEquals(
      1,
      events(VkMetrics.EVENTS_INVALID_METRIC_NAME, VkMetrics.UNKNOWN)
    );
  }

  @Test
  public void countsInvalidEventOnParseFailure() {
    webhookWithoutHandlers().listenVklive("not json").join();

    assertEquals(
      1,
      events(VkMetrics.EVENTS_INVALID_METRIC_NAME, VkMetrics.UNKNOWN)
    );
    assertEquals(
      0,
      events(VkMetrics.EVENTS_FAILED_METRIC_NAME, VkMetrics.UNKNOWN)
    );
  }

  @Test
  public void countsFailedEventAndNotHandledWhenHandlerFails() {
    when(handler.type()).thenReturn(VkEventType.CHANNEL_FOLLOW_CREATE);
    when(handler.handle(any(Event.class))).thenReturn(
      CompletableFuture.failedFuture(new IllegalStateException("send failed"))
    );

    webhookWith(handler).listenVklive(FOLLOW_PAYLOAD).join();

    verify(handler).handle(any(Event.class));
    assertEquals(1, events(VkMetrics.EVENTS_RECEIVED_METRIC_NAME, FOLLOW_TYPE));
    assertEquals(1, events(VkMetrics.EVENTS_FAILED_METRIC_NAME, FOLLOW_TYPE));
    assertEquals(0, events(VkMetrics.EVENTS_HANDLED_METRIC_NAME, FOLLOW_TYPE));
    assertEquals(
      0,
      events(VkMetrics.EVENTS_INVALID_METRIC_NAME, VkMetrics.UNKNOWN)
    );
  }

  private VkWebhook webhookWith(VkEventHandler eventHandler) {
    return new VkWebhook(List.of(eventHandler), mapper, metrics);
  }

  private VkWebhook webhookWithoutHandlers() {
    return new VkWebhook(List.of(), mapper, metrics);
  }

  private double events(String metricName, String type) {
    return registry.counter(metricName, VkMetrics.TYPE_TAG, type).count();
  }
}
