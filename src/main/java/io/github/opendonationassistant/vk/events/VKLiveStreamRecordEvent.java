package io.github.opendonationassistant.vk.events;

import io.github.opendonationassistant.events.HasRecipientId;
import io.micronaut.serde.annotation.Serdeable;
import java.time.Instant;

@Serdeable
public record VKLiveStreamRecordEvent(
  String id,
  String recipientId,
  String videoId,
  String previewUrl,
  String title,
  Instant startedAt,
  Long duration
) implements HasRecipientId {}