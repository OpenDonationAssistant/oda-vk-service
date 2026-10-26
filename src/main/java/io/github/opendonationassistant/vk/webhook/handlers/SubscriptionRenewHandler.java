package io.github.opendonationassistant.vk.webhook.handlers;

import io.github.opendonationassistant.vk.events.VKLiveChannelSubscriptionEvent;
import io.github.opendonationassistant.vk.webhook.VkEventHandler;
import io.github.opendonationassistant.vk.webhook.VkEventType;
import io.github.opendonationassistant.vk.webhook.VkWebhook.Event;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;

@Singleton
public class SubscriptionRenewHandler implements VkEventHandler {

  private final AccountEventForwarder forwarder;

  @Inject
  public SubscriptionRenewHandler(AccountEventForwarder forwarder) {
    this.forwarder = forwarder;
  }

  @Override
  public VkEventType type() {
    return VkEventType.CHANNEL_SUBSCRIPTION_RENEW;
  }

  @Override
  public CompletableFuture<?> handle(Event event) {
    return forwarder.forward(event, eventFactory(event));
  }

  private static @Nullable AccountEventFactory eventFactory(Event event) {
    var subscription = event.data().subscription();
    if (subscription == null) {
      return null;
    }
    var subscriber = subscription.subscriber();
    var renewedAt = subscription.renewedAt();
    var level = subscription.level();
    if (subscriber == null || renewedAt == null || level == null) {
      return null;
    }
    return recipientId ->
      new VKLiveChannelSubscriptionEvent(
        event.id(),
        recipientId,
        subscriber.nick(),
        level.name(),
        subscription.amount(),
        Instant.ofEpochSecond(renewedAt)
      );
  }
}
