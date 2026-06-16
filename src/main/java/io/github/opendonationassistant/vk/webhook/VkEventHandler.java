package io.github.opendonationassistant.vk.webhook;

import java.util.concurrent.CompletableFuture;

public interface VkEventHandler {
  boolean canHandle(String type);
  CompletableFuture<?> handle(VkWebhook.Event event);
}
