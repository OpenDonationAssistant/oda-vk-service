package io.github.opendonationassistant.vk.webhook.handlers;

import io.github.opendonationassistant.vk.events.VKLiveStreamRecordEvent;
import io.github.opendonationassistant.vk.webhook.VkEventHandler;
import io.github.opendonationassistant.vk.webhook.VkEventType;
import io.github.opendonationassistant.vk.webhook.VkWebhook.Event;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;

@Singleton
public class StreamRecordCreateHandler implements VkEventHandler {

  private final AccountEventForwarder forwarder;

  @Inject
  public StreamRecordCreateHandler(AccountEventForwarder forwarder) {
    this.forwarder = forwarder;
  }

  @Override
  public VkEventType type() {
    return VkEventType.CHANNEL_STREAM_RECORD_NEW;
  }

  @Override
  public CompletableFuture<?> handle(Event event) {
    return forwarder.forward(event, eventFactory(event));
  }

  private static @Nullable AccountEventFactory eventFactory(Event event) {
    var streamRecord = event.data().streamRecord();
    if (streamRecord == null) {
      return null;
    }
    var startedAt = streamRecord.streamStartedAt();
    if (startedAt == null) {
      return null;
    }
    return recipientId ->
      new VKLiveStreamRecordEvent(
        event.id(),
        recipientId,
        streamRecord.videoId(),
        streamRecord.previewUrl(),
        streamRecord.title(),
        Instant.ofEpochSecond(startedAt),
        streamRecord.duration()
      );
  }
}
