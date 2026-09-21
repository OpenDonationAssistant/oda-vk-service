package io.github.opendonationassistant.vk.webhook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.opendonationassistant.vk.metrics.VkMetrics;
import io.github.opendonationassistant.vk.webhook.VkWebhook.Channel;
import io.github.opendonationassistant.vk.webhook.VkWebhook.Demand;
import io.github.opendonationassistant.vk.webhook.VkWebhook.Event;
import io.github.opendonationassistant.vk.webhook.VkWebhook.EventData;
import io.github.opendonationassistant.vk.webhook.VkWebhook.Follow;
import io.github.opendonationassistant.vk.webhook.VkWebhook.Follower;
import io.github.opendonationassistant.vk.webhook.VkWebhook.Owner;
import io.github.opendonationassistant.vk.webhook.VkWebhook.Reward;
import io.github.opendonationassistant.vk.webhook.VkWebhook.User;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micronaut.serde.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

public class VkWebhookTest {

  SimpleMeterRegistry registry = new SimpleMeterRegistry();
  VkMetrics metrics = new VkMetrics(registry);
  ObjectMapper mapper = mock(ObjectMapper.class);
  VkEventHandler handler = mock(VkEventHandler.class);

  @Test
  public void countsReceivedAndHandledEvent() throws Exception {
    when(handler.canHandle(any())).thenReturn(true);
    when(handler.handle(any(Event.class))).thenReturn(
      CompletableFuture.completedFuture(null)
    );
    when(mapper.readValue(anyString(), eq(Event.class))).thenReturn(event());

    VkWebhook webhook = new VkWebhook(List.of(handler), mapper, metrics);
    webhook.listenVklive("{ \"event\": \"payload\" }");

    verify(handler).canHandle("channel_follow_create");
    verify(handler).handle(any(Event.class));
    assertEquals(
      1,
      registry
        .counter(
          VkMetrics.EVENTS_RECEIVED_METRIC_NAME,
          VkMetrics.TYPE_TAG,
          "channel_follow_create"
        )
        .count()
    );
    assertEquals(
      1,
      registry
        .counter(
          VkMetrics.EVENTS_HANDLED_METRIC_NAME,
          VkMetrics.TYPE_TAG,
          "channel_follow_create"
        )
        .count()
    );
    assertEquals(
      0,
      registry
        .counter(
          VkMetrics.EVENTS_UNHANDLED_METRIC_NAME,
          VkMetrics.TYPE_TAG,
          "channel_follow_create"
        )
        .count()
    );
  }

  @Test
  public void countsUnhandledEvent() throws Exception {
    when(handler.canHandle(any())).thenReturn(false);
    when(mapper.readValue(anyString(), eq(Event.class))).thenReturn(event());

    VkWebhook webhook = new VkWebhook(List.of(handler), mapper, metrics);
    webhook.listenVklive("{ \"event\": \"payload\" }");

    verify(handler).canHandle("channel_follow_create");
    assertEquals(
      1,
      registry
        .counter(
          VkMetrics.EVENTS_RECEIVED_METRIC_NAME,
          VkMetrics.TYPE_TAG,
          "channel_follow_create"
        )
        .count()
    );
    assertEquals(
      0,
      registry
        .counter(
          VkMetrics.EVENTS_HANDLED_METRIC_NAME,
          VkMetrics.TYPE_TAG,
          "channel_follow_create"
        )
        .count()
    );
    assertEquals(
      1,
      registry
        .counter(
          VkMetrics.EVENTS_UNHANDLED_METRIC_NAME,
          VkMetrics.TYPE_TAG,
          "channel_follow_create"
        )
        .count()
    );
  }

  @Test
  public void countsInvalidEventOnNullPayload() throws Exception {
    when(handler.canHandle(any())).thenReturn(false);
    when(mapper.readValue(anyString(), eq(Event.class))).thenReturn(null);

    VkWebhook webhook = new VkWebhook(List.of(), mapper, metrics);
    webhook.listenVklive("{ \"event\": \"payload\" }");

    assertEquals(
      1,
      registry
        .counter(
          VkMetrics.EVENTS_INVALID_METRIC_NAME,
          VkMetrics.TYPE_TAG,
          VkMetrics.UNKNOWN
        )
        .count()
    );
  }

  @Test
  public void countsInvalidEventOnParseFailure() throws Exception {
    when(handler.canHandle(any())).thenReturn(false);
    when(mapper.readValue(anyString(), eq(Event.class))).thenThrow(
      new IOException("malformed")
    );

    VkWebhook webhook = new VkWebhook(List.of(), mapper, metrics);
    webhook.listenVklive("{ \"event\": \"broken\" }");

    assertEquals(
      1,
      registry
        .counter(
          VkMetrics.EVENTS_INVALID_METRIC_NAME,
          VkMetrics.TYPE_TAG,
          VkMetrics.UNKNOWN
        )
        .count()
    );
    assertEquals(
      0,
      registry
        .counter(
          VkMetrics.EVENTS_FAILED_METRIC_NAME,
          VkMetrics.TYPE_TAG,
          VkMetrics.UNKNOWN
        )
        .count()
    );
  }

  @Test
  public void countsFailedEventAndNotHandledWhenHandlerFails() throws Exception {
    when(handler.canHandle(any())).thenReturn(true);
    when(handler.handle(any(Event.class))).thenReturn(
      CompletableFuture.failedFuture(new IOException("send failed"))
    );
    when(mapper.readValue(anyString(), eq(Event.class))).thenReturn(event());

    VkWebhook webhook = new VkWebhook(List.of(handler), mapper, metrics);
    webhook.listenVklive("{ \"event\": \"payload\" }");

    verify(handler).handle(any(Event.class));
    assertEquals(
      1,
      registry
        .counter(
          VkMetrics.EVENTS_RECEIVED_METRIC_NAME,
          VkMetrics.TYPE_TAG,
          "channel_follow_create"
        )
        .count()
    );
    assertEquals(
      1,
      registry
        .counter(
          VkMetrics.EVENTS_FAILED_METRIC_NAME,
          VkMetrics.TYPE_TAG,
          "channel_follow_create"
        )
        .count()
    );
    assertEquals(
      0,
      registry
        .counter(
          VkMetrics.EVENTS_HANDLED_METRIC_NAME,
          VkMetrics.TYPE_TAG,
          "channel_follow_create"
        )
        .count()
    );
    assertEquals(
      0,
      registry
        .counter(
          VkMetrics.EVENTS_INVALID_METRIC_NAME,
          VkMetrics.TYPE_TAG,
          VkMetrics.UNKNOWN
        )
        .count()
    );
  }

  private static Event event() {
    var demand = new Demand(
      8433849L,
      new User("stCarolas"),
      new Reward("39a919a7-fd10-4ece-9606-dde43b595fc9"),
      "pending",
      List.of()
    );
    var follow = new Follow(
      1785489039L,
      new Follower(15, 19219592L, "OneTuz", "https://images.example/avatar")
    );
    var data = new EventData(
      demand,
      follow,
      null,
      null,
      null,
      null,
      new Channel(12312311L, "someuser"),
      new Owner(123123123L)
    );
    return new Event("6987495", "channel_follow_create", "123123123", data);
  }
}

