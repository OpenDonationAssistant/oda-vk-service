package io.github.opendonationassistant.vk.webhook;

import java.util.concurrent.CompletableFuture;

public interface VkEventHandler {
  VkEventType type();

  CompletableFuture<?> handle(VkWebhook.Event event);
}
