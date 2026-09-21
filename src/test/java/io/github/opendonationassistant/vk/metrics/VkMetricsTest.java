package io.github.opendonationassistant.vk.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

public class VkMetricsTest {

  @Test
  public void countsReceivedEventsPerType() {
    var registry = new SimpleMeterRegistry();
    var metrics = new VkMetrics(registry);

    metrics.eventReceived("channel_follow_create");
    metrics.eventReceived("channel_follow_create");
    metrics.eventReceived("raid_start");
    metrics.eventReceived(null);

    assertEquals(
      2,
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
          VkMetrics.EVENTS_RECEIVED_METRIC_NAME,
          VkMetrics.TYPE_TAG,
          "raid_start"
        )
        .count()
    );
    assertEquals(
      1,
      registry
        .counter(
          VkMetrics.EVENTS_RECEIVED_METRIC_NAME,
          VkMetrics.TYPE_TAG,
          VkMetrics.UNKNOWN
        )
        .count()
    );
  }

  @Test
  public void countsHandledAndUnhandledEventsPerType() {
    var registry = new SimpleMeterRegistry();
    var metrics = new VkMetrics(registry);

    metrics.eventHandled("channel_follow_create");
    metrics.eventHandled("channel_follow_create");
    metrics.eventHandled("stream_start");
    metrics.eventUnhandled("unknown_type");
    metrics.eventUnhandled(null);

    assertEquals(
      2,
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
          VkMetrics.EVENTS_HANDLED_METRIC_NAME,
          VkMetrics.TYPE_TAG,
          "stream_start"
        )
        .count()
    );
    assertEquals(
      1,
      registry
        .counter(
          VkMetrics.EVENTS_UNHANDLED_METRIC_NAME,
          VkMetrics.TYPE_TAG,
          "unknown_type"
        )
        .count()
    );
    assertEquals(
      1,
      registry
        .counter(
          VkMetrics.EVENTS_UNHANDLED_METRIC_NAME,
          VkMetrics.TYPE_TAG,
          VkMetrics.UNKNOWN
        )
        .count()
    );
  }

  @Test
  public void countsInvalidEventsPerType() {
    var registry = new SimpleMeterRegistry();
    var metrics = new VkMetrics(registry);

    metrics.eventInvalid(null);
    metrics.eventInvalid(null);

    assertEquals(
      2,
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
  public void countsNoAccountEventsPerType() {
    var registry = new SimpleMeterRegistry();
    var metrics = new VkMetrics(registry);

    metrics.eventNoAccount("channel_follow_create");
    metrics.eventNoAccount("channel_follow_create");
    metrics.eventNoAccount("raid_start");

    assertEquals(
      2,
      registry
        .counter(
          VkMetrics.EVENTS_NOACCOUNT_METRIC_NAME,
          VkMetrics.TYPE_TAG,
          "channel_follow_create"
        )
        .count()
    );
    assertEquals(
      1,
      registry
        .counter(
          VkMetrics.EVENTS_NOACCOUNT_METRIC_NAME,
          VkMetrics.TYPE_TAG,
          "raid_start"
        )
        .count()
    );
  }

  @Test
  public void countsFailedEventsPerType() {
    var registry = new SimpleMeterRegistry();
    var metrics = new VkMetrics(registry);

    metrics.eventFailed("channel_follow_create");
    metrics.eventFailed("channel_follow_create");
    metrics.eventFailed("raid_start");
    metrics.eventFailed(null);

    assertEquals(
      2,
      registry
        .counter(
          VkMetrics.EVENTS_FAILED_METRIC_NAME,
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
          "raid_start"
        )
        .count()
    );
    assertEquals(
      1,
      registry
        .counter(
          VkMetrics.EVENTS_FAILED_METRIC_NAME,
          VkMetrics.TYPE_TAG,
          VkMetrics.UNKNOWN
        )
        .count()
    );
  }
}