package io.github.opendonationassistant.vk.webhook.handlers;

import io.github.opendonationassistant.rabbit.RabbitClient;
import io.github.opendonationassistant.vk.account.VkAccountRepository;
import io.github.opendonationassistant.vk.metrics.VkMetrics;
import io.github.opendonationassistant.vk.webhook.VkWebhook.Event;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;

@Singleton
class AccountEventForwarder {

  private final VkAccountRepository accountRepository;
  private final RabbitClient events;
  private final VkMetrics metrics;

  @Inject
  public AccountEventForwarder(
    VkAccountRepository accountRepository,
    @Named("vklive") RabbitClient events,
    VkMetrics metrics
  ) {
    this.accountRepository = accountRepository;
    this.events = events;
    this.metrics = metrics;
  }

  public CompletableFuture<Void> forward(
    Event event,
    @Nullable AccountEventFactory factory
  ) {
    if (factory == null) {
      return CompletableFuture.completedFuture(null);
    }
    return accountRepository
      .findByVKId(event.userId())
      .stream()
      .findFirst()
      .map(account -> {
        try {
          events.sendEvent(factory.create(account.data().recipientId()));
          return CompletableFuture.<Void>completedFuture(null);
        } catch (Exception e) {
          return CompletableFuture.<Void>failedFuture(e);
        }
      })
      .orElseGet(() -> {
        metrics.eventNoAccount(event.type());
        return CompletableFuture.<Void>completedFuture(null);
      });
  }
}
