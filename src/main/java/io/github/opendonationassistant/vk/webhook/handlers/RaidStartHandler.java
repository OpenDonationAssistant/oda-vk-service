package io.github.opendonationassistant.vk.webhook.handlers;

import io.github.opendonationassistant.vk.events.VKLiveRaidEvent;
import io.github.opendonationassistant.vk.webhook.VkEventHandler;
import io.github.opendonationassistant.vk.webhook.VkEventType;
import io.github.opendonationassistant.vk.webhook.VkWebhook.Event;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;

@Singleton
public class RaidStartHandler implements VkEventHandler {

  private final AccountEventForwarder forwarder;

  @Inject
  public RaidStartHandler(AccountEventForwarder forwarder) {
    this.forwarder = forwarder;
  }

  @Override
  public VkEventType type() {
    return VkEventType.RAID_START;
  }

  @Override
  public CompletableFuture<?> handle(Event event) {
    return forwarder.forward(event, eventFactory(event));
  }

  private static @Nullable AccountEventFactory eventFactory(Event event) {
    var raid = event.data().raid();
    if (raid == null) {
      return null;
    }
    var source = raid.source();
    if (source == null) {
      return null;
    }
    var owner = source.owner();
    var startedAt = raid.startedAt();
    if (owner == null || startedAt == null) {
      return null;
    }
    return recipientId ->
      new VKLiveRaidEvent(
        event.id(),
        recipientId,
        owner.nick(),
        raid.raidersCount(),
        Instant.ofEpochSecond(startedAt)
      );
  }
}
