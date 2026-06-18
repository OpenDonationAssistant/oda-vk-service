package io.github.opendonationassistant.vk.reward;

import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.data.model.DataType;

@MappedEntity("reward")
public record RewardData(
  @Id String id,
  @MappedProperty(type = DataType.UUID) String accountId,
  @MappedProperty(type = DataType.UUID) String widgetId,
  String type
) {}
