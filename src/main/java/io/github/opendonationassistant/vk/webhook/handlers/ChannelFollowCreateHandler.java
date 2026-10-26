package io.github.opendonationassistant.vk.webhook.handlers;

import io.github.opendonationassistant.vk.events.VKLiveChannelFollowEvent;
import io.github.opendonationassistant.vk.webhook.VkEventHandler;
import io.github.opendonationassistant.vk.webhook.VkEventType;
import io.github.opendonationassistant.vk.webhook.VkWebhook.Event;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;

@Singleton
public class ChannelFollowCreateHandler implements VkEventHandler {

  private final AccountEventForwarder forwarder;

  @Inject
  public ChannelFollowCreateHandler(AccountEventForwarder forwarder) {
    this.forwarder = forwarder;
  }

  @Override
  public VkEventType type() {
    return VkEventType.CHANNEL_FOLLOW_CREATE;
  }

  @Override
  public CompletableFuture<?> handle(Event event) {
    return forwarder.forward(event, eventFactory(event));
  }

  private static @Nullable AccountEventFactory eventFactory(Event event) {
    var follow = event.data().follow();
    if (follow == null) {
      return null;
    }
    var follower = follow.follower();
    var startedAt = follow.startedAt();
    if (follower == null || startedAt == null) {
      return null;
    }
    return recipientId ->
      new VKLiveChannelFollowEvent(
        event.id(),
        recipientId,
        follower.nick(),
        Instant.ofEpochSecond(startedAt)
      );
  }
}
