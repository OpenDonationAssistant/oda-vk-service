package io.github.opendonationassistant.vk.events;

import io.github.opendonationassistant.events.HasRecipientId;
import io.micronaut.serde.annotation.Serdeable;
import java.time.Instant;

@Serdeable
public record VKLiveRaidEvent(
  String id,
  String recipientId,
  String username,
  Long raidersCount,
  Instant startedAt
) implements HasRecipientId {}