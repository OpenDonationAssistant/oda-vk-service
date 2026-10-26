package io.github.opendonationassistant.vk.webhook.handlers;

import io.github.opendonationassistant.vk.events.VKLiveStreamEndedEvent;
import io.github.opendonationassistant.vk.webhook.VkEventHandler;
import io.github.opendonationassistant.vk.webhook.VkEventType;
import io.github.opendonationassistant.vk.webhook.VkWebhook.Event;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;

@Singleton
public class StreamStopHandler implements VkEventHandler {

  private final AccountEventForwarder forwarder;

  @Inject
  public StreamStopHandler(AccountEventForwarder forwarder) {
    this.forwarder = forwarder;
  }

  @Override
  public VkEventType type() {
    return VkEventType.CHANNEL_STREAM_STOP;
  }

  @Override
  public CompletableFuture<?> handle(Event event) {
    return forwarder.forward(event, eventFactory(event));
  }

  private static @Nullable AccountEventFactory eventFactory(Event event) {
    var stream = event.data().stream();
    if (stream == null) {
      return null;
    }
    var startedAt = stream.startedAt();
    var endedAt = stream.endedAt();
    if (startedAt == null || endedAt == null) {
      return null;
    }
    return recipientId ->
      new VKLiveStreamEndedEvent(
        event.id(),
        recipientId,
        stream.videoId(),
        stream.previewUrl(),
        stream.title(),
        Instant.ofEpochSecond(startedAt),
        Instant.ofEpochSecond(endedAt)
      );
  }
}
