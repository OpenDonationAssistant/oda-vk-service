package io.github.opendonationassistant.vk.webhook.handlers;

import io.github.opendonationassistant.vk.account.VkAccountRepository;
import io.github.opendonationassistant.vk.metrics.VkMetrics;
import io.github.opendonationassistant.vk.webhook.VkEventHandler;
import io.github.opendonationassistant.vk.webhook.VkEventType;
import io.github.opendonationassistant.vk.webhook.VkWebhook.Event;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.concurrent.CompletableFuture;

@Singleton
public class PointsDemandCreateHandler implements VkEventHandler {

  private static final String PENDING_STATUS = "pending";

  private final VkAccountRepository accountRepository;
  private final VkMetrics metrics;

  @Inject
  public PointsDemandCreateHandler(
    VkAccountRepository accountRepository,
    VkMetrics metrics
  ) {
    this.accountRepository = accountRepository;
    this.metrics = metrics;
  }

  @Override
  public VkEventType type() {
    return VkEventType.CHANNEL_POINTS_REWARD_DEMAND_CREATE;
  }

  @Override
  public CompletableFuture<?> handle(Event event) {
    var demand = event.data().demand();
    if (demand == null || !PENDING_STATUS.equals(demand.status())) {
      return CompletableFuture.completedFuture(null);
    }
    return accountRepository
      .findByVKId(event.userId())
      .stream()
      .findFirst()
      .map(it -> it.acceptReward(event))
      .orElseGet(() -> {
        metrics.eventNoAccount(event.type());
        return CompletableFuture.<Void>completedFuture(null);
      });
  }
}
