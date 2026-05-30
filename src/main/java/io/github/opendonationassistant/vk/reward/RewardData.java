package io.github.opendonationassistant.vk.reward;

import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.data.model.DataType;

@MappedEntity
public record RewardData(
  @Id String id,
  @MappedProperty String recipientId,
  @MappedProperty(type = DataType.UUID) String refreshTokenId,
  String type
) {}
