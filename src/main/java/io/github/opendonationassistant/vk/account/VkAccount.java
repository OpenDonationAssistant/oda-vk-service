package io.github.opendonationassistant.vk.account;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public class VkAccount {

  private final VkAccountData data;

  public VkAccount(VkAccountData data) {
    this.data = data;
  }

  public VkAccountData data() {
    return data;
  }
}
