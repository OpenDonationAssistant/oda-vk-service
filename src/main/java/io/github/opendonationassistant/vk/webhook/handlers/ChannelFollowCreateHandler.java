package io.github.opendonationassistant.vk.webhook.handlers;

import io.github.opendonationassistant.rabbit.RabbitClient;
import io.github.opendonationassistant.vk.account.VkAccountRepository;
import io.github.opendonationassistant.vk.events.VKLiveChannelFollowEvent;
import io.github.opendonationassistant.vk.metrics.VkMetrics;
import io.github.opendonationassistant.vk.webhook.VkEventHandler;
import io.github.opendonationassistant.vk.webhook.VkWebhook.Event;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

@Singleton
public class ChannelFollowCreateHandler implements VkEventHandler {

  private final VkAccountRepository accountRepository;
  private final RabbitClient events;
  private final VkMetrics metrics;

  @Inject
  public ChannelFollowCreateHandler(
    VkAccountRepository accountRepository,
    @Named("vklive") RabbitClient events,
    VkMetrics metrics
  ) {
    this.accountRepository = accountRepository;
    this.events = events;
    this.metrics = metrics;
  }

  @Override
  public boolean canHandle(String type) {
    return "channel_follow_create".equals(type);
  }

  @Override
  public CompletableFuture<?> handle(Event event) {
    var follow = event.data().follow();
    if (follow == null) {
      return CompletableFuture.completedFuture(null);
    }
    var follower = follow.follower();
    if (follower == null) {
      return CompletableFuture.completedFuture(null);
    }
    var startedAt = follow.startedAt();
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
            new VKLiveChannelFollowEvent(
              event.id(),
              account.data().recipientId(),
              follower.nick(),
              Instant.ofEpochSecond(startedAt)
            )
          );
          return CompletableFuture.<Void>completedFuture(null);
        } catch (IOException e) {
          return CompletableFuture.<Void>failedFuture(e);
        }
      })
      .orElseGet(() -> {
        metrics.eventNoAccount(event.type());
        return CompletableFuture.<Void>completedFuture(null);
      });
  }
}
