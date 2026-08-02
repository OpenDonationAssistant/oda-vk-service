package io.github.opendonationassistant.vk.events;

import io.github.opendonationassistant.events.HasRecipientId;
import io.micronaut.serde.annotation.Serdeable;
import java.time.Instant;

@Serdeable
public record VKLiveChannelSubscriptionEvent(
  String id,
  String recipientId,
  String username,
  String tier,
  Long amount,
  Instant renewedAt
) implements HasRecipientId {}