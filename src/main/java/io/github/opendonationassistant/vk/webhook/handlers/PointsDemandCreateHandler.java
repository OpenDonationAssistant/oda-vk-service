package io.github.opendonationassistant.vk.webhook.handlers;

import io.github.opendonationassistant.integration.VkClient;
import io.github.opendonationassistant.rabbit.RabbitClient;
import io.github.opendonationassistant.vk.account.VkAccountRepository;
import io.github.opendonationassistant.vk.metrics.VkMetrics;
import io.github.opendonationassistant.vk.webhook.VkEventHandler;
import io.github.opendonationassistant.vk.webhook.VkWebhook.Event;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.concurrent.CompletableFuture;

@Singleton
public class PointsDemandCreateHandler implements VkEventHandler {

  private final VkClient vk;
  private final VkAccountRepository accountRepository;
  private final RabbitClient commands;
  private final VkMetrics metrics;

  @Inject
  public PointsDemandCreateHandler(
    VkClient vk,
    VkAccountRepository accountRepository,
    @Named("commands") RabbitClient commands,
    VkMetrics metrics
  ) {
    this.vk = vk;
    this.accountRepository = accountRepository;
    this.commands = commands;
    this.metrics = metrics;
  }

  @Override
  public boolean canHandle(String type) {
    return "channel_points_reward_demand_create".equals(type);
  }

  @Override
  public CompletableFuture<?> handle(Event event) {
    if (!event.data().demand().status().equals("pending")) {
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
