package io.github.opendonationassistant.vk.webhook.handlers;

import io.github.opendonationassistant.commons.Optionals;
import io.github.opendonationassistant.vk.account.VkAccountRepository;
import io.github.opendonationassistant.vk.metrics.VkMetrics;
import io.github.opendonationassistant.vk.webhook.VkEventHandler;
import io.github.opendonationassistant.vk.webhook.VkEventType;
import io.github.opendonationassistant.vk.webhook.VkWebhook.Demand;
import io.github.opendonationassistant.vk.webhook.VkWebhook.Event;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Optional;
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
    var demand = Optional.ofNullable(event.data().demand()).filter(it ->
      PENDING_STATUS.equals(it.status())
    );
    var rewardId = demand.map(it -> it.reward()).map(it -> it.id());
    return Optionals.For(demand, rewardId)
      .yield((d, r) -> handleReward(event.userId(), event.type(), d, r))
      .orElseGet(() -> CompletableFuture.<Void>completedFuture(null));
  }

  private CompletableFuture<Void> handleReward(
    String userId,
    String eventType,
    Demand demand,
    String rewardId
  ) {
    return accountRepository
      .findByVKId(userId)
      .stream()
      .findFirst()
      .map(account ->
        account
          .reward(rewardId)
          .map(it -> it.accept(demand))
          .orElseGet(() -> {
            metrics.eventUnknownReward(eventType);
            return CompletableFuture.<Void>completedFuture(null);
          })
      )
      .orElseGet(() -> {
        metrics.eventNoAccount(eventType);
        return CompletableFuture.<Void>completedFuture(null);
      });
  }
}
