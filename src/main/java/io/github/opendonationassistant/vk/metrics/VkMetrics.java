package io.github.opendonationassistant.vk.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

@Singleton
public class VkMetrics {

  public static final String EVENTS_RECEIVED_METRIC_NAME = "vk.events.received";
  public static final String EVENTS_HANDLED_METRIC_NAME = "vk.events.handled";
  public static final String EVENTS_UNHANDLED_METRIC_NAME =
    "vk.events.unhandled";
  public static final String EVENTS_INVALID_METRIC_NAME = "vk.events.invalid";
  public static final String EVENTS_NOACCOUNT_METRIC_NAME = "vk.events.noaccount";
  public static final String EVENTS_FAILED_METRIC_NAME = "vk.events.failed";
  public static final String TYPE_TAG = "type";
  public static final String UNKNOWN = "unknown";

  private final MeterRegistry registry;

  @Inject
  public VkMetrics(MeterRegistry registry) {
    this.registry = registry;
  }

  public void eventReceived(@Nullable String type) {
    counter(EVENTS_RECEIVED_METRIC_NAME, type).increment();
  }

  public void eventHandled(@Nullable String type) {
    counter(EVENTS_HANDLED_METRIC_NAME, type).increment();
  }

  public void eventUnhandled(@Nullable String type) {
    counter(EVENTS_UNHANDLED_METRIC_NAME, type).increment();
  }

  public void eventInvalid(@Nullable String type) {
    counter(EVENTS_INVALID_METRIC_NAME, type).increment();
  }

  public void eventNoAccount(@Nullable String type) {
    counter(EVENTS_NOACCOUNT_METRIC_NAME, type).increment();
  }

  public void eventFailed(@Nullable String type) {
    counter(EVENTS_FAILED_METRIC_NAME, type).increment();
  }

  private Counter counter(String name, @Nullable String type) {
    return registry.counter(
      name,
      TYPE_TAG,
      Optional.ofNullable(type).orElse(UNKNOWN)
    );
  }
}