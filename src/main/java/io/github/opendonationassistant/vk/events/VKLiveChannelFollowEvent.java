package io.github.opendonationassistant.vk.events;

import java.time.Instant;

import io.github.opendonationassistant.events.HasRecipientId;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record VKLiveChannelFollowEvent(
  String id,
  String recipientId,
  String username,
  Instant timestamp
) implements HasRecipientId {}
