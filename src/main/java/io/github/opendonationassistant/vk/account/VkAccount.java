package io.github.opendonationassistant.vk.account;

import io.github.opendonationassistant.integration.VkClient;
import io.github.opendonationassistant.integration.VkDataClient;
import io.micronaut.serde.annotation.Serdeable;
import java.util.concurrent.CompletableFuture;

@Serdeable
public class VkAccount {

  private final VkAccountData data;
  private final VkClient vk;

  public VkAccount(VkAccountData data, VkClient vk) {
    this.data = data;
    this.vk = vk;
  }

  public VkAccountData data() {
    return data;
  }

  public CompletableFuture<Void> acceptReward(
    VkDataClient.AcceptRewardRequest request
  ) {
    return vk.acceptReward(
      data.recipientId(),
      data.refreshTokenId(),
      data.channelUrl(),
      request
    );
  }
}
