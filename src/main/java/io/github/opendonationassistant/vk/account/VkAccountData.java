package io.github.opendonationassistant.vk.account;

import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.data.model.DataType;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
@MappedEntity("accounts")
public record VkAccountData(
  @Id String id,
  String username,
  @MappedProperty(type = DataType.UUID) String recipientId,
  @MappedProperty(type = DataType.UUID) String refreshTokenId
) {}
