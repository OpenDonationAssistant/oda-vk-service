package io.github.opendonationassistant.vk.events;

import io.github.opendonationassistant.events.HasRecipientId;
import io.micronaut.serde.annotation.Serdeable;
import java.time.Instant;

@Serdeable
public record VKLiveStreamEndedEvent(
  String id,
  String recipientId,
  String videoId,
  String previewUrl,
  String title,
  Instant startedAt,
  Instant endedAt
) implements HasRecipientId {}