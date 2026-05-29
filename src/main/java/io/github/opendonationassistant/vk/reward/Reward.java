package io.github.opendonationassistant.vk.reward;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record Reward(RewardData data) {

  public String id() {
    return data.id();
  }

  public String recipientId() {
    return data.recipientId();
  }

  public String refreshTokenId() {
    return data.refreshTokenId();
  }

  public String type() {
    return data.type();
  }
}
