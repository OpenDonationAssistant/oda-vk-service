package io.github.opendonationassistant.vk.webhook.handlers;

import io.github.opendonationassistant.events.HasRecipientId;

/**
 * Builds the outbound event for an already resolved recipient.
 *
 * <p>Implementations capture the source payload; only the recipient id varies.
 */
@FunctionalInterface
interface AccountEventFactory {
  HasRecipientId create(String recipientId);
}
