package io.github.opendonationassistant.vk.webhook.handlers;

import io.github.opendonationassistant.rabbit.RabbitClient;
import io.github.opendonationassistant.vk.account.VkAccountRepository;
import io.github.opendonationassistant.vk.events.VKLiveStreamRecordEvent;
import io.github.opendonationassistant.vk.webhook.VkEventHandler;
import io.github.opendonationassistant.vk.webhook.VkWebhook.Event;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

@Singleton
public class StreamRecordCreateHandler implements VkEventHandler {

  private final VkAccountRepository accountRepository;
  private final RabbitClient events;

  @Inject
  public StreamRecordCreateHandler(
    VkAccountRepository accountRepository,
    @Named("vklive") RabbitClient events
  ) {
    this.accountRepository = accountRepository;
    this.events = events;
  }

  @Override
  public boolean canHandle(String type) {
    return "channel_stream_record_new".equals(type);
  }

  @Override
  public CompletableFuture<?> handle(Event event) {
    var streamRecord = event.data().streamRecord();
    if (streamRecord == null) {
      return CompletableFuture.completedFuture(null);
    }
    var startedAt = streamRecord.streamStartedAt();
    if (startedAt == null) {
      return CompletableFuture.completedFuture(null);
    }
    return accountRepository
      .findByVKId(event.userId())
      .stream()
      .findFirst()
      .map(account -> {
        try {
          events.sendEvent(
            new VKLiveStreamRecordEvent(
              event.id(),
              account.data().recipientId(),
              streamRecord.videoId(),
              streamRecord.previewUrl(),
              streamRecord.title(),
              Instant.ofEpochSecond(startedAt),
              streamRecord.duration()
            )
          );
          return CompletableFuture.<Void>completedFuture(null);
        } catch (IOException e) {
          return CompletableFuture.<Void>failedFuture(e);
        }
      })
      .orElseGet(() -> CompletableFuture.completedFuture(null));
  }
}